package br.com.luizbrand.worklog.system;

import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Conflict.SystemAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.SystemNotFoundException;
import br.com.luizbrand.worklog.system.dto.SystemRequest;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
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

    @Transactional
    public SystemResponse createSystem(SystemRequest systemRequest) {

        systemRepository.findByName(systemRequest.name())
              .ifPresent( existingSystem -> {
                  throw new SystemAlreadyExistsException("System with name: " + systemRequest.name() + " already exists");
              });

        Systems systemSaved = systemRepository.save(systemMapper.toSystem(systemRequest));
        return systemMapper.toSystemResponse(systemSaved);
    }

    @Transactional
    public SystemResponse updateSystem(SystemRequest systemRequest, UUID publicId) {

        Optional<Systems> optSytem = systemRepository.findByPublicId(publicId);

        if (optSytem.isEmpty()) {
            throw new SystemNotFoundException("System with public ID: " + publicId + " not found");
        }

        Systems system = optSytem.get();
        systemMapper.updateSystem(systemRequest, system);
        Systems savedSystem = systemRepository.save(system);
        return systemMapper.toSystemResponse(savedSystem);

    }


    public Systems findByPublicId(UUID publicId) {
        return systemRepository.findByPublicId(publicId)
                .orElseThrow(() -> new SystemNotFoundException("System with public ID: " + publicId + " not found"));
    }

    public Systems findActiveSystem(UUID publicId) {
        Systems system = this.findByPublicId(publicId);
        if(!system.getIsEnabled()) {
            throw new BusinessException("System is not active");
        }
        return system;
    }
}
