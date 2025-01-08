package io.event.ems.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.event.ems.dto.CategoryDTO;
import io.event.ems.model.Category;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy =  NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {

    CategoryDTO toDTO(Category category);
    
    Category toEntity(CategoryDTO categoryDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateCategoryFromDTO(CategoryDTO dto, @MappingTarget Category entity);

}
