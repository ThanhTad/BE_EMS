package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutputContext {

    private String name;
    private String lifespanCount;
    private Map<String, Object> parameters;
}
