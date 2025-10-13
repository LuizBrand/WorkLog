package br.com.luizbrand.worklog.service;

import br.com.luizbrand.worklog.dto.request.SystemRequest;
import br.com.luizbrand.worklog.dto.response.SystemResponse;
import br.com.luizbrand.worklog.entity.Systems;
import br.com.luizbrand.worklog.exception.Conflict.SystemAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.SystemNotFoundException;
import br.com.luizbrand.worklog.mapper.SystemMapper;
import br.com.luizbrand.worklog.repository.SystemRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class SystemService {

    private final SystemRepository systemRepository;
    private final SystemMapper systemMapper;

    public SystemService(SystemRepository systemRepository, SystemMapper systemMapper) {
        this.systemRepository = systemRepository;
        this.systemMapper = systemMapper;
    }

    public List<SystemResponse> findAllSystems() {
        List<Systems> systems = systemRepository.findAll();
        return systems.stream()
                .map(systemMapper::toSystemResponse)
                .toList();
    }

    public List<Systems> findAllByPublicIds(List<UUID> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Systems> foundSystems = systemRepository.findAllByPublicIdIn(publicIds);
        if (foundSystems.size() != publicIds.size()) {
            throw new RuntimeException("Um ou mais sistemas não foram encontrados para os IDs fornecidos.");
        }
        return foundSystems;
    }

    //Method to verify if a system already exist, but cannot return an empty value
    public SystemResponse getSystemByPublicId(UUID publicId) {

        return systemRepository.findByPublicId(publicId)
                .map(systemMapper::toSystemResponse)
                .orElseThrow(() -> new SystemNotFoundException("System with public ID: " + publicId + " not found"));

    }

    public SystemResponse createSystem(SystemRequest systemRequest) {

        systemRepository.findByName(systemRequest.name())
              .ifPresent( existingSystem -> {
                  throw new SystemAlreadyExistsException("System with name: " + systemRequest.name() + " already exists");
              });

        Systems systemSaved = systemRepository.save(systemMapper.toSystem(systemRequest));
        return systemMapper.toSystemResponse(systemSaved);
    }

    //Method to verify if a system already exist, but accept an empty value
/*    public Optional<SystemResponse>findSystemByPublicId(UUID publicId) {
        if (publicId == null) {
            return Optional.empty();
        }
        return systemRepository.findByPublicId(publicId)
                .map(systemMapper::toSystemResponse);

    }*/


}
