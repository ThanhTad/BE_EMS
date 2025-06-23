package io.event.ems.repository;

import io.event.ems.model.TicketQrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketQrCodeRepository extends JpaRepository<TicketQrCode, UUID> {
}
