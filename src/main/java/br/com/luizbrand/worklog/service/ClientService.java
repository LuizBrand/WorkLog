package br.com.luizbrand.worklog.service;

import br.com.luizbrand.worklog.dto.request.ClientRequest;
import br.com.luizbrand.worklog.dto.response.ClientResponse;
import br.com.luizbrand.worklog.dto.searchFilters.ClientFiltersParams;
import br.com.luizbrand.worklog.entity.Client;
import br.com.luizbrand.worklog.entity.Systems;
import br.com.luizbrand.worklog.exception.Conflict.ClientAlreadyExistsException;
import br.com.luizbrand.worklog.mapper.ClientMapper;
import br.com.luizbrand.worklog.repository.ClientRepository;
import br.com.luizbrand.worklog.repository.specification.ClientSpecification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final SystemService systemService;

    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper, SystemService systemService) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
        this.systemService = systemService;
    }

    public List<ClientResponse> findAllClients(ClientFiltersParams filtersParams) {

        List<Client> clients = clientRepository.findAll(ClientSpecification.findByFilter(filtersParams));

        return clients.stream()
                .map(clientMapper::toClientResponse)
                .toList();

    }

    public Optional<ClientResponse> findClientByPublicId(UUID publicId) {
        if (publicId == null) {
            return Optional.empty();
        }
        return clientRepository.findByPublicId(publicId)
                .map(clientMapper::toClientResponse);

    }

    public ClientResponse getClientByPublicId(UUID publicId) {
        return clientRepository.findByPublicId(publicId)
                .map(clientMapper::toClientResponse)
                .orElseThrow(() -> new ClientAlreadyExistsException("Client with public ID: " + publicId + " not found"));

    }

    public ClientResponse createClient(ClientRequest clientRequest) {
        clientRepository.findByName(clientRequest.name())
                .ifPresent(existingClient -> {
                    throw new ClientAlreadyExistsException("Client with name: " + clientRequest.name() + " already exists");
                });

        List<Systems> associatedSystems = new ArrayList<>();
        List<UUID> systemsPublicIds = clientRequest.systemsPublicIds();

        if (systemsPublicIds != null && !systemsPublicIds.isEmpty()) {
            associatedSystems = systemService.findAllByPublicIds(systemsPublicIds);
        }

        Client clientSaved = clientRepository.save(clientMapper.toClient(clientRequest, associatedSystems));
        return clientMapper.toClientResponse(clientSaved);
    }
}
