package io.event.ems.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.event.ems.dto.UserDTO;
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
    
    @Override
    public UserDTO createUser(UserDTO userDTO) {
        if(isUsernameExists(userDTO.getUsername())){
            throw new DuplicateUsernameException("Username already exists");
        }

        if(isUsernameExists(userDTO.getEmail())){
            throw new DuplicateEmailException("Email already exists");
        }

        StatusCode userStatus = statusCodeRepository.findByEntityTypeAndStatus("USER", "ACTIVE")
                                    .orElseThrow(() -> new StatusNotFoundException("Status not found"));
        User user = userMapper.toEntity(userDTO);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setStatus(userStatus);
        user.setRole(Role.USER);
        return userMapper.toDTO(userRepository.save(user));
        
    }

    @Override
    public UserDTO updateUser(UUID id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                        .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        if(userDTO.getUsername() != null){
            if(isUsernameExists(userDTO.getUsername()) && !userDTO.getUsername().equals(user.getUsername())){
                throw new DuplicateUsernameException("Username already exists");
            }
            user.setUsername(userDTO.getUsername());
        }

        if(userDTO.getEmail() != null){
            if(isEmailExists(userDTO.getEmail()) && !userDTO.getEmail().equals(user.getEmail())){
                throw new DuplicateEmailException("Email already exists");
            }
            user.setEmail(userDTO.getEmail());
        }

        if(userDTO.getFullName() != null)
            user.setFullName(userDTO.getFullName());
        if (userDTO.getPhone() != null)
            user.setPhone(userDTO.getPhone());
        if (userDTO.getAvatarUrl() != null)
            user.setAvatarUrl(userDTO.getAvatarUrl());
        if (userDTO.getRole() != null)
            user.setRole(Role.valueOf(userDTO.getRole().toUpperCase()));

        userMapper.updateUserFromDto(userDTO, user);
        return userMapper.toDTO(userRepository.save(user));

    }

    @Override
    public void delete(UUID id) {
        if(!userRepository.existsById(id)){
            throw new UserNotFoundException("User does not exists");
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserById(UUID id) {
        if(!userRepository.existsById(id)){
            throw new UserNotFoundException("User does not exists");
        }
        return userRepository.findById(id)
                    .map(userMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                    .map(userMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> searchUsers(String keyword, Pageable pageable) {
        if(keyword == null || keyword.trim().isEmpty()){
            return getAllUsers(pageable);
        }
        return userRepository.searchUser(keyword, pageable)
                    .map(userMapper::toDTO);
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
    public Optional<UserDTO> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                    .map(userMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                    .map(userMapper::toDTO);
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
    public Optional<UserDTO> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                    .map(userMapper::toDTO);
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
        if(!passwordEncoder.matches(currentPassword, user.getPassword())){
            throw new InvalidPasswordException("Incorrect current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void verifyEmail(UUID id) {
        throw new UnsupportedOperationException("Unimplemented method 'verifyEmail'");
    }

    private void updateUserStatus(UUID id, String status){
        User user = userRepository.findById(id)
                        .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        StatusCode statusCode = statusCodeRepository.findByEntityTypeAndStatus("USER", status)
                                    .orElseThrow(() -> new StatusNotFoundException("Status not found"));
        user.setStatus(statusCode);
        userRepository.save(user);
    }
    
}
