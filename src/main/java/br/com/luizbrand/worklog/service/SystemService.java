package br.com.luizbrand.worklog.service;

import br.com.luizbrand.worklog.dto.request.SystemRequest;
import br.com.luizbrand.worklog.dto.response.SystemResponse;
import br.com.luizbrand.worklog.entity.Systems;
import br.com.luizbrand.worklog.exception.SystemAlreadyExistsException;
import br.com.luizbrand.worklog.exception.SystemNotFoundException;
import br.com.luizbrand.worklog.mapper.SystemMapper;
import br.com.luizbrand.worklog.repository.SystemRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class SystemService {

    private final SystemRepository systemRepository;
    private final SystemMapper systemMapper;

    public SystemService(SystemRepository systemRepository, SystemMapper systemMapper) {
        this.systemRepository = systemRepository;
        this.systemMapper = systemMapper;
    }

    //Method to verify if a system already exist, but accept an empty value
    public Optional<SystemResponse> findSystemByPublicId(UUID publicId) {
        if (publicId == null) {
            return Optional.empty();
        }
        return systemRepository.findByPublicId(publicId)
                .map(systemMapper::toSystemResponse);

    }

    //Method to verify if a system already exist, but cannot return an empty value
    public SystemResponse getSystemByPublicId(UUID publicId) {

        return systemRepository.findByPublicId(publicId)
                .map(systemMapper::toSystemResponse)
                .orElseThrow(() -> new SystemNotFoundException("Systems with public ID: " + publicId + " not found"));

    }

    public SystemResponse createSystem(SystemRequest systemRequest) {

        systemRepository.findByName(systemRequest.name())
              .ifPresent( existingSystem -> {
                  throw new SystemAlreadyExistsException("Systems with name: " + systemRequest.name() + " already exists");
              });

        Systems systemSaved = systemRepository.save(systemMapper.toSystem(systemRequest));
        return systemMapper.toSystemResponse(systemSaved);
    }


}
