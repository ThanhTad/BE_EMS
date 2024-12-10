package io.event.ems.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.UserRequestDTO;
import io.event.ems.dto.UserResponseDTO;
import io.event.ems.exception.InvalidPasswordException;

public interface UserService {

    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    UserResponseDTO updateUser(UUID id, UserRequestDTO userRequestDTO);

    void delete(UUID id);

    Optional<UserResponseDTO> getUserById(UUID id);

    Page<UserResponseDTO> getAllUsers(Pageable pageable);

    Page<UserResponseDTO> searchUsers(String keyword, Pageable pageable);

    boolean isUsernameExists(String username);

    boolean isEmailExists(String email);

    Optional<UserResponseDTO> getUserByUsername(String username);

    Optional<UserResponseDTO> getUserByEmail(String email);

    void enableUser(UUID id);

    void disableUser(UUID id);

    Optional<UserResponseDTO> findByUsernameOrEmail(String usernameOrEmail);

    void resetPassword(UUID id, String newPass);

    void changePassword(UUID id, String newPassword, String currentPassword) throws InvalidPasswordException;

    void verifyEmail(UUID id);
}
