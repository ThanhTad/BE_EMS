package io.event.ems.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.NotificationDTO;
import io.event.ems.exception.UnauthorizedException;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "APIs for fetching and making user notifications as read")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Count unread notifications", description = "Returns the total number of unread notifications for the current user")
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> countUnread(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @Operation(summary = "List unread notifications", description = "Retrieves a paginated list of unread notifications for the current user, sorted by createdAt desc")
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getUnread(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        Page<NotificationDTO> page = notificationService.getUnread(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @Operation(summary = "Mark notifications as read", description = "Mark the given list of notification IDs as read for the current user")
    @PostMapping("/mark-read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Parameter(description = "List of notification IDs to mark as read") List<Long> ids) {
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("ids list must not be empty");
        }
        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        notificationService.markAsRead(userId, ids);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
