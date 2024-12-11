package io.event.ems.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.event.ems.dto.UserRequestDTO;
import io.event.ems.dto.UserResponseDTO;
import io.event.ems.model.User;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    UserResponseDTO toResponseDTO(User user);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "status", ignore = true)
    User toEntity(UserRequestDTO userRequestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "twoFactorEnabled", ignore = true)
    void updateUserFromDto(UserRequestDTO dto, @MappingTarget User entity);

}
