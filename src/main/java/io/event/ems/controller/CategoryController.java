package io.event.ems.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.CategoryDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieves all categories with pagination support.")
    public ResponseEntity<ApiResponse<Page<CategoryDTO>>> getAllCategories(@PageableDefault(size = 20) Pageable pageable){
        Page<CategoryDTO> categories = categoryService.getAllCategory(pageable);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by Id", description = "Retrieves a category by its Id")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryById(@PathVariable UUID id) throws ResourceNotFoundException{
        Optional<CategoryDTO> category = categoryService.getCategoryById(id);
        return category.map(dto -> new ResponseEntity<>(ApiResponse.success(dto), HttpStatus.OK))
                        .orElse(new ResponseEntity<>(ApiResponse.error(HttpStatus.NOT_FOUND, "Category not found with id: " + id), HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @Operation(summary = "Create a new category", description = "Create a new category and return the created category.")
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@RequestBody CategoryDTO categoryDTO){
        CategoryDTO createdCategory = categoryService.createCategory(categoryDTO);
        return ResponseEntity.ok(ApiResponse.success(createdCategory));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", description = "Updates an existing category by its Id.")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(@PathVariable UUID id, @RequestBody CategoryDTO categoryDTO){
        CategoryDTO updatedCategory = categoryService.updateCategory(id, categoryDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedCategory));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category", description = "Deletes a category by its Id.")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id){
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

}
