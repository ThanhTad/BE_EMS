package io.event.ems.controller;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Sort;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.UserRequestDTO;
import io.event.ems.dto.UserResponseDTO;
import io.event.ems.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO userResponseDTO = userService.createUser(userRequestDTO);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", userResponseDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable UUID id) {
        Optional<UserResponseDTO> userDTO = userService.getUserById(id);
        return ResponseEntity.ok(
                userDTO.map(dto -> ApiResponse.success("User found", dto))
                        .orElse(ApiResponse.error(HttpStatus.NOT_FOUND, "User not found")));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(value = "role", required = false) String role,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserResponseDTO> users;
        if (role != null) {
            users = userService.getUsersByRole(role, pageable);
        } else {
            users = userService.getAllUsers(pageable);
        }
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> searchUsers(@RequestParam("keyword") String keyword,
            @PageableDefault(size = 10, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<UserResponseDTO> users = userService.searchUsers(keyword, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Users found", users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable UUID id,
            @Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO user = userService.updateUser(id, userRequestDTO);
        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.success("User deleted successfully", null));
    }

    @PostMapping("/{id}/changepass")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable UUID id,
            @RequestParam("newPassword") String newPass,
            @RequestParam("currentPassword") String currentPass) {
        userService.changePassword(id, newPass, currentPass);
        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/{id}/resetpass")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable UUID id,
            @RequestParam("newPassword") String newPass) {
        userService.resetPassword(id, newPass);
        return ResponseEntity.ok(
                ApiResponse.success("Password reset successfully", null));
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable UUID id) {
        userService.enableUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User enabled successfully", null));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable UUID id) {
        userService.disableUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User disabled successfully", null));
    }

    @GetMapping("/by-username-or-email")
    public ResponseEntity<ApiResponse<UserResponseDTO>> findByUsernameOrEmail(
            @RequestParam("usernameOrEmail") String usernameOrEmail) {
        Optional<UserResponseDTO> users = userService.findByUsernameOrEmail(usernameOrEmail);
        return ResponseEntity.ok(
                users.map(dto -> ApiResponse.success("User found", dto))
                        .orElse(ApiResponse.error(HttpStatus.NOT_FOUND, "User not found")));
    }

    @PostMapping("/{id}/avatar")
    @Operation(summary = "Upload and update avatar")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(@PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        String url = userService.storeAvatar(id, file);
        return ResponseEntity.ok(
                ApiResponse.success("Avatar uploaded successfully", Map.of("url", url)));
    }

}
