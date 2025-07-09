package io.event.ems.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DialogflowWebhookResponse {

    @JsonProperty("fulfillmentText")
    private String fulfillmentText;
}
