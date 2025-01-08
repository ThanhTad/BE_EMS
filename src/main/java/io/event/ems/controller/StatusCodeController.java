package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.StatusCodeDTO;
import io.event.ems.exception.DuplicateResourceException;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.StatusCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/status")
@RequiredArgsConstructor
@Tag(name = "Status Code", description = "Status Code management APIs")
public class StatusCodeController {

    private final StatusCodeService statusCodeService;

    @GetMapping
    @Operation(summary = "Get all status codes", description = "Retrieves all status codes with pagination support.")
    public ResponseEntity<ApiResponse<Page<StatusCodeDTO>>> getAllStatusCodes(@PageableDefault(size = 20) Pageable pageable) {
        Page<StatusCodeDTO> statusCodes = statusCodeService.getAllStatusCodes(pageable);
        return ResponseEntity.ok(ApiResponse.success(statusCodes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get status code by ID", description = "Retrieves a status code by its Id.")
    public ResponseEntity<ApiResponse<StatusCodeDTO>> getStatusCodeById(@PathVariable Integer id) throws ResourceNotFoundException {
        Optional<StatusCodeDTO> statusCode = statusCodeService.getStatusCodeById(id);
        return ResponseEntity.ok(ApiResponse.success(statusCode.orElseThrow(() -> new ResourceNotFoundException("Status code not found with id: " + id))));
    }

    @PostMapping
    @Operation(summary = "Create a new status code", description = "Creates a new status code and returns the created status code.")
    public ResponseEntity<ApiResponse<StatusCodeDTO>> createStatusCode(@RequestBody StatusCodeDTO statusCodeDTO) throws DuplicateResourceException {
        StatusCodeDTO createdStatusCode = statusCodeService.createStatusCode(statusCodeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createdStatusCode));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update status code", description = "Updates an existing status code by its Id.")
    public ResponseEntity<ApiResponse<StatusCodeDTO>> updateStatusCode(@PathVariable Integer id, @RequestBody StatusCodeDTO statusCodeDTO) throws ResourceNotFoundException, DuplicateResourceException {
        StatusCodeDTO updatedStatusCode = statusCodeService.updateStatusCode(id, statusCodeDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedStatusCode));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete status code", description = "Deletes a status code by its Id.")
    public ResponseEntity<ApiResponse<Void>> deleteStatusCode(@PathVariable Integer id) throws ResourceNotFoundException {
        statusCodeService.deleteStatusCode(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
