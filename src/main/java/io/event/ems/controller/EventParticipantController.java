package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.EventParticipantRequestDTO;
import io.event.ems.dto.EventParticipantResponseDTO;
import io.event.ems.service.EventParticipantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/participants")
@RequiredArgsConstructor
@Tag(name = "Event Participant", description = "Event Participant management APIs")
public class EventParticipantController {

    private final EventParticipantService service;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<EventParticipantResponseDTO>> register(
            @Valid @RequestBody EventParticipantRequestDTO request) {
        EventParticipantResponseDTO responseDto = service.registerParticipant(request);
        return new ResponseEntity<>(ApiResponse.success(responseDto), HttpStatus.CREATED);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<Page<EventParticipantResponseDTO>>> getParticipantsByEvent(
            @PathVariable UUID eventId,
            @PageableDefault(size = 20, sort = "registrationDate") Pageable pageable) {
        Page<EventParticipantResponseDTO> responsePage = service.getParticipantsByEvent(eventId, pageable);
        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<EventParticipantResponseDTO>>> getEventsByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "registrationDate") Pageable pageable) {
        Page<EventParticipantResponseDTO> responsePage = service.getEventsByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    @DeleteMapping("/unregister/{participantId}")
    public ResponseEntity<ApiResponse<Void>> unregister(@PathVariable UUID participantId) {
        service.unregisterParticipant(participantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
