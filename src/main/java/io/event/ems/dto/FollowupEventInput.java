package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowupEventInput {

    private String name;
    private Map<String, Object> parameters;
    private String languageCode;
}
