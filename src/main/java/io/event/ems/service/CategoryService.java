package io.event.ems.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.CategoryDTO;
import io.event.ems.exception.DuplicateResourceException;
import io.event.ems.exception.ResourceNotFoundException;

public interface CategoryService {

    Page<CategoryDTO> getAllCategory(Pageable pageable);
    Optional<CategoryDTO> getCategoryById(UUID id) throws ResourceNotFoundException;
    CategoryDTO createCategory(CategoryDTO categoryDTO) throws DuplicateResourceException;
    CategoryDTO updateCategory(UUID id, CategoryDTO categoryDTO) throws ResourceNotFoundException, DuplicateResourceException;
    void deleteCategory(UUID id) throws ResourceNotFoundException;

}
