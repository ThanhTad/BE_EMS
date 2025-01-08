package io.event.ems.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.StatusCodeDTO;
import io.event.ems.exception.DuplicateResourceException;
import io.event.ems.exception.ResourceNotFoundException;

public interface StatusCodeService {

    Page<StatusCodeDTO> getAllStatusCodes(Pageable pageable);
    Optional<StatusCodeDTO> getStatusCodeById(Integer id) throws ResourceNotFoundException;
    StatusCodeDTO createStatusCode(StatusCodeDTO statusCodeDTO) throws DuplicateResourceException;
    StatusCodeDTO updateStatusCode(Integer id, StatusCodeDTO statusCodeDTO) throws ResourceNotFoundException, DuplicateResourceException;
    void deleteStatusCode(Integer id) throws ResourceNotFoundException;

}