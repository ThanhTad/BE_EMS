package io.event.ems.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {

    private String queryText;
    private String action;
    private Map<String, Object> parameters;
    private Intent intent;
    private String languageCode;
    private Float intentDetectionConfidence;

    @JsonProperty("fulfillmentText")
    private String fulfillmentText;

    @JsonProperty("fulfillmentMessages")
    private List<Map<String, Object>> fulfillmentMessages;

}
