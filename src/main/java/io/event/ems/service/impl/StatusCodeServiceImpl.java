package io.event.ems.service.impl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.event.ems.dto.StatusCodeDTO;
import io.event.ems.exception.DuplicateResourceException;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.StatusCodeMapper;
import io.event.ems.model.StatusCode;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.service.StatusCodeService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatusCodeServiceImpl implements StatusCodeService {

    private final StatusCodeRepository statusCodeRepository;
    private final StatusCodeMapper statusCodeMapper;

    @Override
    public Page<StatusCodeDTO> getAllStatusCodes(Pageable pageable) {
        return statusCodeRepository.findAll(pageable)
                    .map(statusCodeMapper::toDTO);
    }

    @Override
    public Optional<StatusCodeDTO> getStatusCodeById(Integer id) throws ResourceNotFoundException {
        return statusCodeRepository.findById(id)
                    .map(statusCodeMapper::toDTO);
    }

    @Override
    public StatusCodeDTO createStatusCode(StatusCodeDTO statusCodeDTO) throws DuplicateResourceException {
        if(statusCodeRepository.existsByEntityTypeAndStatus(statusCodeDTO.getEntityType(), statusCodeDTO.getStatus())){
            throw new DuplicateResourceException("StatusCode with entityType: " + statusCodeDTO.getEntityType() +
                    " and status: " + statusCodeDTO.getStatus() + " already exists");
        }

        StatusCode statusCode = statusCodeMapper.toEntity(statusCodeDTO);
        statusCode = statusCodeRepository.save(statusCode);
        return statusCodeMapper.toDTO(statusCode);
    }

    @Override
    public StatusCodeDTO updateStatusCode(Integer id, StatusCodeDTO statusCodeDTO)
            throws ResourceNotFoundException, DuplicateResourceException {
        StatusCode statusCode = statusCodeRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("StatusCode not found with id: " + id));

        if (!statusCode.getEntityType().equals(statusCodeDTO.getEntityType()) || !statusCode.getStatus().equals(statusCodeDTO.getStatus())) {
                if (statusCodeRepository.existsByEntityTypeAndStatus(statusCodeDTO.getEntityType(), statusCodeDTO.getStatus())) {
                    throw new DuplicateResourceException("StatusCode with entityType: " + statusCodeDTO.getEntityType() +
                        " and status: " + statusCodeDTO.getStatus() + " already exists");
            }
        }
        
        statusCodeMapper.updateStatusCodeFromDTO(statusCodeDTO, statusCode);
        statusCode = statusCodeRepository.save(statusCode);
        return statusCodeMapper.toDTO(statusCode);

    }

    @Override
    public void deleteStatusCode(Integer id) throws ResourceNotFoundException {
        if(!statusCodeRepository.existsById(id)){
            throw new ResourceNotFoundException("Status code not found with id: " + id);
        }
        statusCodeRepository.deleteById(id);
    }
}
