package io.event.ems.service.specialized;

import io.event.ems.dto.HoldRequestDTO;
import io.event.ems.dto.HoldResponseDTO;
import io.event.ems.exception.SeatsNotAvailableException;
import io.event.ems.model.Ticket;
import io.event.ems.repository.TicketRepository;
import io.event.ems.util.HoldInfo;
import io.event.ems.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralAdmissionBookingServiceImpl implements GeneralAdmissionBookingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TicketRepository ticketRepository;

    @Value("${booking.hold.duration-seconds}")
    private Long holdDurationSeconds;


    @Override
    @Transactional(readOnly = true)
    public HoldResponseDTO holdTickets(HoldRequestDTO request) {
        log.debug("Holding tickets for request: {}", request);

        List<HoldRequestDTO.GeneralAdmissionHoldItem> itemsToHold = request.getGaItems();

        if (itemsToHold == null || itemsToHold.isEmpty()) {
            throw new IllegalArgumentException("General admission items must be provided.");
        }


        // 1. Lấy thông tin tất cả các loại vé cần giữ từ DB trong một lần truy vấn
        List<UUID> ticketIds = itemsToHold.stream()
                .map(HoldRequestDTO.GeneralAdmissionHoldItem::getTicketId)
                .collect(Collectors.toList());

        Map<UUID, Ticket> ticketMapFromDb = ticketRepository.findAllById(ticketIds).stream()
                .collect(Collectors.toMap(Ticket::getId, Function.identity()));

        // 2. KIỂM TRA SỐ LƯỢNG CHO TỪNG LOẠI VÉ
        for (HoldRequestDTO.GeneralAdmissionHoldItem item : itemsToHold) {
            UUID ticketId = item.getTicketId();
            int quantityToHold = item.getQuantity();

            Ticket ticketInDb = ticketMapFromDb.get(ticketId);
            if (ticketInDb == null) {
                throw new IllegalArgumentException("Ticket with ID " + ticketId + " not found.");
            }

            // Lấy số lượng vé đang được giữ (held) từ Redis
            String heldCountKey = RedisKeyUtil.getGeneralAdmissionHeldCountKey(ticketId);
            // Dùng opsForValue().get() an toàn hơn increment(0) vì nó trả về null nếu key không tồn tại
            Object heldCountObj = redisTemplate.opsForValue().get(heldCountKey);
            long currentlyHeld = (heldCountObj instanceof Number) ? ((Number) heldCountObj).longValue() : 0L;

            // Kiểm tra xem số lượng còn lại có đủ không
            if (ticketInDb.getAvailableQuantity() - currentlyHeld < quantityToHold) {
                throw new SeatsNotAvailableException(
                        "Not enough tickets available for '" + ticketInDb.getName() + "'. " +
                                "Requested: " + quantityToHold +
                                ", Available: " + (ticketInDb.getAvailableQuantity() - currentlyHeld)
                );
            }
        }

        // 3. NẾU TẤT CẢ ĐỀU HỢP LỆ, TIẾN HÀNH GIỮ CHỖ
        UUID holdId = UUID.randomUUID();
        String holdKey = RedisKeyUtil.getHoldKey(holdId);

        // Tạo đối tượng HoldInfo chứa toàn bộ thông tin
        HoldInfo holdInfo = new HoldInfo(
                "GENERAL_ADMISSION",
                request.getUserId(),
                request.getEventId(),
                null, // seatIds là null
                itemsToHold
        );

        // Lưu vào Redis với TTL
        redisTemplate.opsForValue().set(holdKey, holdInfo, holdDurationSeconds, TimeUnit.SECONDS);

        // 4. CẬP NHẬT (TĂNG) BỘ ĐẾM SỐ LƯỢNG ĐANG GIỮ TRONG REDIS
        for (HoldRequestDTO.GeneralAdmissionHoldItem item : itemsToHold) {
            String heldCountKey = RedisKeyUtil.getGeneralAdmissionHeldCountKey(item.getTicketId());
            redisTemplate.opsForValue().increment(heldCountKey, item.getQuantity());
        }

        // 5. TRẢ VỀ KẾT QUẢ
        Instant expiresAt = Instant.now().plusSeconds(holdDurationSeconds);
        return new HoldResponseDTO(holdId, expiresAt);
    }
}
