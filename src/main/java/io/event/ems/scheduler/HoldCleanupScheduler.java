package io.event.ems.scheduler;

import io.event.ems.service.TicketHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class HoldCleanupScheduler {

    private final TicketHoldService ticketHoldService;

    @Scheduled(initialDelay = 120000, fixedRate = 60000)
    public void cleanupExpiredHolds() {
        log.info("Running scheduled task to clean up expired ticket holds...");
        try {
            // Service sẽ chứa logic để tìm và xóa các hold đã hết hạn
            ticketHoldService.cleanupExpiredHolds();
        } catch (Exception e) {
            log.error("Error during expired holds cleanup task", e);
        }
    }
}
