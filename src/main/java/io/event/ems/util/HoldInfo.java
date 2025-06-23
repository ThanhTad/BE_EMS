package io.event.ems.util;

import io.event.ems.dto.HoldRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoldInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String type;// "RESERVED" or "GENERAL_ADMISSION"
    private UUID userId;
    private UUID eventId;

    // For RESERVED
    private List<UUID> seatIds;

    // Thay thế các trường GA cũ bằng một list
    private List<HoldRequestDTO.GeneralAdmissionHoldItem> gaItems;
}
