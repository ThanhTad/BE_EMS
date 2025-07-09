package io.event.ems.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DialogflowWebhookRequest {

    private String responseId;

    @JsonProperty("queryResult")
    private QueryResult queryResult;

    private String session;

    @JsonProperty("originalDetectIntentRequest")
    private OriginalDetectIntentRequest originalDetectIntentRequest;
}
