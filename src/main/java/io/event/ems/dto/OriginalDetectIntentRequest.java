package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OriginalDetectIntentRequest {

    private String source;
    private String version;
    private Map<String, Object> payload;
}
