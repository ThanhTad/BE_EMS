package io.event.ems.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.event.ems.dto.UserSettingsDTO;
import io.event.ems.model.UserSettings;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserSettingsMapper {

    UserSettingsDTO toDTO(UserSettings userSettings);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromDto(UserSettingsDTO dto, @MappingTarget UserSettings entity);

}
