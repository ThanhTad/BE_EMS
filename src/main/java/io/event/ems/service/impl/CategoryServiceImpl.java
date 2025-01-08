package io.event.ems.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.event.ems.dto.CategoryDTO;
import io.event.ems.exception.DuplicateResourceException;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.CategoryMapper;
import io.event.ems.model.Category;
import io.event.ems.repository.CategoryRepository;
import io.event.ems.service.CategoryService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    
    @Override
    public Page<CategoryDTO> getAllCategory(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                    .map(categoryMapper::toDTO);
    }

    @Override
    public Optional<CategoryDTO> getCategoryById(UUID id) throws ResourceNotFoundException {
        return categoryRepository.findById(id)
                    .map(categoryMapper::toDTO);
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) throws DuplicateResourceException {
        if(categoryRepository.existsByName(categoryDTO.getName())){
            throw new DuplicateResourceException("Category with name: " + categoryDTO.getName() + " already exists");
        }
        Category category = categoryMapper.toEntity(categoryDTO);
        category = categoryRepository.save(category);
        return categoryMapper.toDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(UUID id, CategoryDTO categoryDTO)
            throws ResourceNotFoundException, DuplicateResourceException {
        Category category = categoryRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if(!category.getName().equals(categoryDTO.getName()) && categoryRepository.existsByName(categoryDTO.getName())){
            throw new DuplicateResourceException("Category with name: " + categoryDTO.getName() + " already exists");
        }

        categoryMapper.updateCategoryFromDTO(categoryDTO, category);
        category = categoryRepository.save(category);
        return categoryMapper.toDTO(category);
    }

    @Override
    public void deleteCategory(UUID id) throws ResourceNotFoundException {
        if(!categoryRepository.existsById(id)){
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

}
