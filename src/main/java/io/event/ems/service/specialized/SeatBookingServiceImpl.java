package io.event.ems.service.specialized;

import io.event.ems.dto.HoldRequestDTO;
import io.event.ems.dto.HoldResponseDTO;
import io.event.ems.exception.SeatsNotAvailableException;
import io.event.ems.model.EventSeatStatus;
import io.event.ems.repository.EventSeatStatusRepository;
import io.event.ems.util.HoldInfo;
import io.event.ems.util.RedisKeyUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatBookingServiceImpl implements SeatBookingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EventSeatStatusRepository eventSeatStatusRepository;

    @Value("${booking.hold.duration-seconds}")
    private Long holdDurationSeconds;

    @Override
    @Transactional
    public HoldResponseDTO holdSeats(HoldRequestDTO request) {
        log.debug("Holding seats for request: {}", request);

        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new IllegalArgumentException("At least one seat ID must be provided");
        }

        List<EventSeatStatus> seatsToHold = eventSeatStatusRepository.findAllByIdInWithDetailsAndLock(request.getSeatIds());

        boolean allAvailable = seatsToHold.size() == request.getSeatIds().size() && seatsToHold.stream()
                .allMatch(s -> "AVAILABLE".equals(s.getStatus()));

        if (!allAvailable) {
            throw new SeatsNotAvailableException("One or more selected seats are no longer available.");
        }

        UUID holdId = UUID.randomUUID();
        String holdKey = RedisKeyUtil.getHoldKey(holdId);

        HoldInfo holdInfo = new HoldInfo("RESERVED", request.getUserId(), request.getEventId(), request.getSeatIds(), null, null);
        redisTemplate.opsForValue().set(holdKey, holdInfo, holdDurationSeconds, TimeUnit.SECONDS);

        for (EventSeatStatus seat : seatsToHold) {
            seat.setStatus("HOLD");
        }
        eventSeatStatusRepository.saveAll(seatsToHold);

        Instant expireAt = Instant.now().plusSeconds(holdDurationSeconds);
        log.info("Seats successfully held for hold ID: {}. Expires at: {}", holdId, expireAt);
        return new HoldResponseDTO(holdId, expireAt);
    }
}
