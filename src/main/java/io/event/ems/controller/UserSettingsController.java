package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.UserSettingsDTO;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/settings")
@RequiredArgsConstructor
@Tag(name = "User Settings", description = "Manage current user's settings")
@Slf4j
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @GetMapping
    @Operation(summary = "Get current user's settings")
    public ResponseEntity<ApiResponse<UserSettingsDTO>> getCurrentUserSettings(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UserSettingsDTO settings = userSettingsService.getSettings(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    @Operation(summary = "Update current user's settings")
    public ResponseEntity<ApiResponse<UserSettingsDTO>> updateCurrentUserSettings(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody UserSettingsDTO settingsDTO) {
        UserSettingsDTO updatedSettings = userSettingsService.updateSettings(currentUser.getId(), settingsDTO);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", updatedSettings));
    }
}
