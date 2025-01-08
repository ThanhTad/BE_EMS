package io.event.ems.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.event.ems.dto.StatusCodeDTO;
import io.event.ems.model.StatusCode;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StatusCodeMapper {

    StatusCodeDTO toDTO(StatusCode statusCode);

    StatusCode toEntity(StatusCodeDTO statusCodeDTO);

    @Mapping(target = "id", ignore = true)
    void updateStatusCodeFromDTO(StatusCodeDTO dto, @MappingTarget StatusCode entity);


}
