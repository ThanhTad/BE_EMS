package io.event.ems.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.UserDTO;
import io.event.ems.exception.InvalidPasswordException;

public interface UserService {

    UserDTO createUser(UserDTO userDTO);

    UserDTO updateUser(UUID id, UserDTO userDTO);

    void delete(UUID id);

    Optional<UserDTO> getUserById(UUID id);

    Page<UserDTO> getAllUsers(Pageable pageable);

    Page<UserDTO> searchUsers(String keyword, Pageable pageable);

    boolean isUsernameExists(String username);

    boolean isEmailExists(String email);

    Optional<UserDTO> getUserByUsername(String username);

    Optional<UserDTO> getUserByEmail(String email);

    void enableUser(UUID id);

    void disableUser(UUID id);

    Optional<UserDTO> findByUsernameOrEmail(String usernameOrEmail);

    void resetPassword(UUID id, String newPass);

    void changePassword(UUID id, String newPassword, String currentPassword) throws InvalidPasswordException;

    void verifyEmail(UUID id);
}
