package io.event.ems.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.event.ems.dto.HoldDetailsResponseDTO;
import io.event.ems.dto.HoldResponseDTO;
import io.event.ems.dto.TicketHoldRequestDTO;
import io.event.ems.model.HoldData;
import io.event.ems.model.TicketSelectionModeEnum;
import io.event.ems.repository.TicketRepository;
import io.event.ems.service.TicketHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketHoldServiceImpl implements TicketHoldService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TicketRepository ticketRepository;

    private static final long HOLD_DURATION_MINUTES = 10;
    private static final String HOLD_KEY_PREFIX = "ticket_hold:";
    private static final String HELD_SEATS_KEY_PREFIX = "event_held_seats:";

    @Override
    public HoldResponseDTO createAndValidateHold(UUID eventId, TicketHoldRequestDTO request, UUID userId) {
        if (!request.isValidForMode()) {
            throw new IllegalArgumentException("Invalid hold request for the selected mode.");
        }

        UUID holdId = UUID.randomUUID();
        HoldData holdData = new HoldData(holdId, eventId, userId, request, LocalDateTime.now().plusMinutes(HOLD_DURATION_MINUTES));

        acquireResources(holdData);

        try {
            String holdDataJson = objectMapper.writeValueAsString(holdData);
            redisTemplate.opsForValue().set(HOLD_KEY_PREFIX + holdId, holdDataJson, Duration.ofMinutes(HOLD_DURATION_MINUTES));
            log.info("Successfully created hold [ID={}] for user [ID={}]", holdId, userId);
            return new HoldResponseDTO(holdId, holdData.getExpiresAt());
        } catch (Exception e) {
            log.error("Failed to write hold to Redis [ID={}]. Releasing resources.", holdId, e);
            releaseResources(holdData); // Rollback
            throw new RuntimeException("System error during hold process.");
        }
    }

    @Override
    public void releaseHold(UUID holdId, UUID userId) {
        String holdKey = HOLD_KEY_PREFIX + holdId;
        String holdDataJson = redisTemplate.opsForValue().get(holdKey);
        if (holdDataJson == null) return;

        try {
            HoldData holdData = objectMapper.readValue(holdDataJson, HoldData.class);
            if (!holdData.getUserId().equals(userId)) {
                throw new SecurityException("User not authorized to release this hold.");
            }

            releaseResources(holdData);
            redisTemplate.delete(holdKey);
            log.info("Successfully released hold [ID={}] by user [ID={}]", holdId, userId);
        } catch (Exception e) {
            log.error("Error releasing hold [ID={}]", holdId, e);
        }
    }

    @Override
    public HoldData getAndFinalizeHold(UUID holdId, UUID userId) {
        String holdKey = HOLD_KEY_PREFIX + holdId;
        String holdDataJson = redisTemplate.opsForValue().getAndDelete(holdKey);

        if (holdDataJson == null) {
            throw new IllegalArgumentException("Your session has expired. Please select your tickets again.");
        }
        try {
            HoldData holdData = objectMapper.readValue(holdDataJson, HoldData.class);
            if (!holdData.getUserId().equals(userId)) {
                throw new SecurityException("User not authorized for this hold.");
            }
            return holdData;
        } catch (Exception e) {
            log.error("Failed to parse hold data during checkout [ID={}]", holdId, e);
            throw new RuntimeException("Error processing hold data.");
        }
    }

    @Override
    public HoldDetailsResponseDTO getHoldDetails(UUID holdId, UUID userId) {
        String holdKey = HOLD_KEY_PREFIX + holdId;
        String holdDataJson = redisTemplate.opsForValue().get(holdKey);

        if (holdDataJson == null) {
            throw new IllegalArgumentException("Hold not found or has expired.");
        }

        try {
            HoldData holdData = objectMapper.readValue(holdDataJson, HoldData.class);
            if (!holdData.getUserId().equals(userId)) {
                throw new SecurityException("User not authorized to access this hold.");
            }

            log.info("Retrieved hold details [ID={}] for user [ID={}]", holdId, userId);

            // Convert HoldData to HoldDetailsResponseDTO (không expose userId)
            return new HoldDetailsResponseDTO(
                    holdData.getHoldId(),
                    holdData.getEventId(),
                    holdData.getExpiresAt(),
                    holdData.getRequest()
            );
        } catch (Exception e) {
            log.error("Failed to parse hold data [ID={}]", holdId, e);
            throw new RuntimeException("Error retrieving hold details.");
        }
    }

    @Override
    public void cleanupExpiredHolds() {
        int cleanedCount = 0;
        int scannedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        // Sử dụng SCAN để lặp qua các key một cách an toàn
        // match(HOLD_KEY_PREFIX + "*") -> chỉ quét các key bắt đầu bằng prefix của chúng ta
        // count(100) -> gợi ý Redis trả về khoảng 100 key mỗi lần quét
        ScanOptions scanOptions = ScanOptions.scanOptions().match(HOLD_KEY_PREFIX + "*").count(100).build();

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String holdKey = cursor.next();
                scannedCount++;

                String holdDataJson = redisTemplate.opsForValue().get(holdKey);
                if (holdDataJson == null) {
                    continue;
                }

                try {
                    HoldData holdData = objectMapper.readValue(holdDataJson, HoldData.class);
                    if (holdData.getExpiresAt().isBefore(now)) {
                        log.warn("Hold [ID={}] has expired at {}. Releasing resources.", holdData.getHoldId(), holdData.getExpiresAt());

                        releaseResources(holdData);
                        redisTemplate.delete(holdKey);

                        cleanedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error processing hold key: {}. Removing potentially invalid key.", holdKey, e);
                    redisTemplate.delete(holdKey); // Xóa key lỗi để tránh lặp lại
                }
            }
        } catch (Exception e) {
            log.error("An error occurred during the Redis SCAN operation for hold cleanup.", e);
        }

        log.debug("Scanned {} keys for cleanup.", scannedCount);
        if (cleanedCount > 0) {
            log.info("Successfully cleaned up {} expired holds.", cleanedCount);
        }

    }

    @Override
    public void releaseResourcesForFailedCheckout(HoldData holdData) {
        log.warn("Releasing resources for a FAILED checkout, hold [ID={}]", holdData.getHoldId());
        releaseResources(holdData);
    }

    private void acquireResources(HoldData holdData) {
        TicketHoldRequestDTO request = holdData.getRequest();
        if (request.getSelectionMode() == TicketSelectionModeEnum.RESERVED_SEATING) {
            String key = HELD_SEATS_KEY_PREFIX + holdData.getEventId();
            String[] seatIds = request.getSeatIds().stream().map(UUID::toString).toArray(String[]::new);
            Long addedCount = redisTemplate.opsForSet().add(key, seatIds);
            if (addedCount == null || addedCount != seatIds.length) {
                redisTemplate.opsForSet().remove(key, (Object[]) seatIds);
                throw new IllegalArgumentException("Some seats are no longer available.");
            }
        } else {
            for (var item : request.getGaItems()) {
                int updatedRows = ticketRepository.decreaseAvailableQuantity(item.getTicketId(), item.getQuantity());
                if (updatedRows == 0) {
                    // Rollback các vé đã trừ trước đó trong cùng request
                    for (var rolledBackItem : request.getGaItems()) {
                        if (rolledBackItem.getTicketId().equals(item.getTicketId())) break;
                        ticketRepository.increaseAvailableQuantity(rolledBackItem.getTicketId(), rolledBackItem.getQuantity());
                    }
                    throw new IllegalArgumentException("Not enough tickets available for one of the selected types.");
                }
            }
        }
    }

    private void releaseResources(HoldData holdData) {
        TicketHoldRequestDTO request = holdData.getRequest();
        if (request.getSelectionMode() == TicketSelectionModeEnum.RESERVED_SEATING) {
            String key = HELD_SEATS_KEY_PREFIX + holdData.getEventId();
            String[] seatIds = request.getSeatIds().stream().map(UUID::toString).toArray(String[]::new);
            redisTemplate.opsForSet().remove(key, (Object[]) seatIds);
        } else { // GENERAL_ADMISSION & ZONED_ADMISSION
            request.getGaItems().forEach(item ->
                    ticketRepository.increaseAvailableQuantity(item.getTicketId(), item.getQuantity())
            );
        }
    }
}