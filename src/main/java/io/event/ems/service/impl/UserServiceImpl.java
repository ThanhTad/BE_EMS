package io.event.ems.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.event.ems.dto.UserRequestDTO;
import io.event.ems.dto.UserResponseDTO;
import io.event.ems.exception.DuplicateEmailException;
import io.event.ems.exception.DuplicateUsernameException;
import io.event.ems.exception.InvalidPasswordException;
import io.event.ems.exception.StatusNotFoundException;
import io.event.ems.exception.UserNotFoundException;
import io.event.ems.mapper.UserMapper;
import io.event.ems.model.Role;
import io.event.ems.model.StatusCode;
import io.event.ems.model.User;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.UserRepository;
import io.event.ems.service.FileStorageService;
import io.event.ems.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StatusCodeRepository statusCodeRepository;
    private final FileStorageService fileStorageService;

    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        if (isUsernameExists(userRequestDTO.getUsername())) {
            throw new DuplicateUsernameException("Username already exists");
        }

        if (isUsernameExists(userRequestDTO.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }

        StatusCode userStatus = statusCodeRepository.findByEntityTypeAndStatus("USER", "ACTIVE")
                .orElseThrow(() -> new StatusNotFoundException("Status not found"));
        User user = userMapper.toEntity(userRequestDTO);

        if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        }

        user.setStatus(userStatus);
        user.setRole(Role.USER);
        return userMapper.toResponseDTO(userRepository.save(user));

    }

    @Override
    public UserResponseDTO updateUser(UUID id, UserRequestDTO userRequestDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (userRequestDTO.getUsername() != null) {
            if (isUsernameExists(userRequestDTO.getUsername())
                    && !userRequestDTO.getUsername().equals(user.getUsername())) {
                throw new DuplicateUsernameException("Username already exists");
            }
            user.setUsername(userRequestDTO.getUsername());
        }

        if (userRequestDTO.getEmail() != null) {
            if (isEmailExists(userRequestDTO.getEmail()) && !userRequestDTO.getEmail().equals(user.getEmail())) {
                throw new DuplicateEmailException("Email already exists");
            }
            user.setEmail(userRequestDTO.getEmail());
        }

        if (userRequestDTO.getFullName() != null)
            user.setFullName(userRequestDTO.getFullName());
        if (userRequestDTO.getPhone() != null)
            user.setPhone(userRequestDTO.getPhone());
        if (userRequestDTO.getAvatarUrl() != null)
            user.setAvatarUrl(userRequestDTO.getAvatarUrl());
        if (userRequestDTO.getRole() != null)
            user.setRole(Role.valueOf(userRequestDTO.getRole().toUpperCase()));

        userMapper.updateUserFromDto(userRequestDTO, user);
        return userMapper.toResponseDTO(userRepository.save(user));

    }

    @Override
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User does not exists");
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> getUserById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> searchUsers(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers(pageable);
        }
        return userRepository.searchUser(keyword, pageable)
                .map(userMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> getUserByUsername(String username) {
        return Optional.ofNullable(userRepository.findByUsername(username)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> getUserByEmail(String email) {
        return Optional.ofNullable(userRepository.findByEmail(email)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email)));
    }

    @Override
    public void enableUser(UUID id) {
        updateUserStatus(id, "ACTIVE");
    }

    @Override
    public void disableUser(UUID id) {
        updateUserStatus(id, "INACTIVE");
    }

    @Override
    public Optional<UserResponseDTO> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .map(userMapper::toResponseDTO);
    }

    @Override
    public void resetPassword(UUID id, String newPass) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
    }

    @Override
    public void changePassword(UUID id, String newPassword, String currentPassword) throws InvalidPasswordException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException("Incorrect current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void verifyEmail(UUID id) {
        throw new UnsupportedOperationException("Unimplemented method 'verifyEmail'");
    }

    private void updateUserStatus(UUID id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        StatusCode statusCode = statusCodeRepository.findByEntityTypeAndStatus("USER", status)
                .orElseThrow(() -> new StatusNotFoundException("Status not found"));
        user.setStatus(statusCode);
        userRepository.save(user);
    }

    @Override
    public String storeAvatar(UUID userId, MultipartFile file) {
        String url = fileStorageService.storeFile(file, "avatars/" + userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }

    @Override
    public Page<UserResponseDTO> getUsersByRole(String role, Pageable pageable) {
        return userRepository.findByRole(role, pageable)
                .map(userMapper::toResponseDTO);
    }

}
