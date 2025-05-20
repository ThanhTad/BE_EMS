package io.event.ems.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.UserSettingsDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.exception.UnauthorizedException;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/me/settings")
@RequiredArgsConstructor
@Tag(name = "User Settings", description = "Manage current user's settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @GetMapping
    @Operation(summary = "Get current user's settings")
    public ResponseEntity<ApiResponse<UserSettingsDTO>> getCurrentUserSettings(
            @AuthenticationPrincipal CustomUserDetails currentUser) throws ResourceNotFoundException {
        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        UserSettingsDTO settings = userSettingsService.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    @Operation(summary = "Update current user's settings")
    public ResponseEntity<ApiResponse<UserSettingsDTO>> updateCurrentUserSettings(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody UserSettingsDTO settingsDTO) throws ResourceNotFoundException {
        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        UserSettingsDTO updatedSettings = userSettingsService.updateSettings(userId, settingsDTO);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", updatedSettings));
    }
}
