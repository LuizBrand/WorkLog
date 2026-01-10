package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.system.SystemService;
import br.com.luizbrand.worklog.exception.Conflict.ClientAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new ClientNotFoundException("Client with public ID: " + publicId + " not found"));

    }

    @Transactional
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

    @Transactional
    public ClientResponse updateClient(UUID publicId, ClientRequest clientRequest) {
        Optional<Client> clientOpt = clientRepository.findByPublicId(publicId);

        if (clientOpt.isEmpty()) {
            throw new ClientNotFoundException("Client with public ID: " + publicId + " not found");
        }

        Client client = clientOpt.get();
        List<Systems> associatedSystems = null;
        List<UUID> systemsPublicIds = clientRequest.systemsPublicIds();

        if (systemsPublicIds != null) {
            associatedSystems = new ArrayList<>(); // se não veio null, o padrão é uma lista vazia
            if (!systemsPublicIds.isEmpty()) { // se não estiver vazia, buscar os sistemas pelo id vindo dela
                associatedSystems = systemService.findAllByPublicIds(systemsPublicIds);
            }
        }

        clientMapper.updateClient(clientRequest, associatedSystems, client);
        Client savedClient = clientRepository.save(client);
        return clientMapper.toClientResponse(savedClient);
    }

}
