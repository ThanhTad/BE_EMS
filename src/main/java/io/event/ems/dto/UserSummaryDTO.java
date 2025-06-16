package io.event.ems.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserSummaryDTO {

    private UUID id;
    private String fullName;
    private String avatarUrl;

}
