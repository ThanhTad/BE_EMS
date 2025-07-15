package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentMessage {

    private String platform;
    private Map<String, Object> text;
    private Map<String, Object> card;
    private Map<String, Object> quickReplies;
}
