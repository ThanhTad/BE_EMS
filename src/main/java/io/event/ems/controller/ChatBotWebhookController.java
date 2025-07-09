package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.DialogflowWebhookRequest;
import io.event.ems.dto.DialogflowWebhookResponse;
import io.event.ems.service.ChatbotIntentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ChatBot Webhook", description = "ChatBot Webhook APIs")
public class ChatBotWebhookController {

    private final ChatbotIntentService chatbotIntentService;

    @PostMapping("/webhook")
    @Operation(summary = "ChatBot Webhook", description = "ChatBot Webhook")
    public ResponseEntity<DialogflowWebhookResponse> handleWebhook(@RequestBody DialogflowWebhookRequest request) {
        String intentName = request.getQueryResult().getIntent().getDisplayName();
        String queryText = request.getQueryResult().getQueryText();
        Map<String, Object> parameters = request.getQueryResult().getParameters();

        log.info("Intent name: {}, Query text: {}, Parameters: {}", intentName, queryText, parameters);

        String responseText;
        try {
            responseText = processIntent(intentName, parameters);
        } catch (Exception e) {
            log.error("Error while processing intent: {}", intentName, e);
            responseText = "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau hoặc liên hệ với chúng tôi để được hỗ trợ.";
        }
        return ResponseEntity.ok(new DialogflowWebhookResponse(responseText));
    }

    private String processIntent(String intentName, Map<String, Object> parameters) {
        return switch (intentName) {
            case "GetEventLocation" -> chatbotIntentService.handleGetEventLocation(parameters);
            case "CheckTicketAvailability" -> chatbotIntentService.handleCheckTicketAvailability(parameters);
            case "GetEventSchedule" -> chatbotIntentService.handleGetEventSchedule(parameters);
            case "GetTicketPrices" -> chatbotIntentService.handleGetTicketPrices(parameters);
            default -> {
                log.warn("Intent name: {} not found", intentName);
                yield """
                        Xin lỗi, tôi chưa hiểu câu hỏi của bạn. Bạn có thể thử hỏi về:
                        • Địa điểm sự kiện
                        • Tình trạng vé
                        • Lịch trình sự kiện
                        • Giá vé
                        Hoặc liên hệ với chúng tôi để được hỗ trợ trực tiếp.""";
            }
        };
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Health Check")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Chatbot webhook is running!"));
    }
}
