package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.client.dto.ClientUpdateRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.system.SystemService;
import br.com.luizbrand.worklog.exception.Conflict.ClientAlreadyExistsException;
import br.com.luizbrand.worklog.exception.Conflict.DocumentoAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientBranchRepository clientBranchRepository;
    private final ClientMapper clientMapper;
    private final SystemService systemService;

    public ClientService(ClientRepository clientRepository, ClientBranchRepository clientBranchRepository,
                         ClientMapper clientMapper, SystemService systemService) {
        this.clientRepository = clientRepository;
        this.clientBranchRepository = clientBranchRepository;
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

        ClientRequest validatedRequest = applyBranchRules(clientRequest);

        List<Systems> associatedSystems = new ArrayList<>();
        List<UUID> systemsPublicIds = validatedRequest.systemsPublicIds();

        if (systemsPublicIds != null && !systemsPublicIds.isEmpty()) {
            associatedSystems = systemService.findAllByPublicIds(systemsPublicIds);
        }

        Client clientSaved = clientRepository.save(clientMapper.toClient(validatedRequest, associatedSystems));
        return clientMapper.toClientResponse(clientSaved);
    }

    /**
     * Regras que nem o banco nem o Bean Validation conseguem expressar. Lidas do
     * request (e não da entidade mapeada) porque a regra é sobre o que o cliente
     * da API pediu. Devolve o request possivelmente ajustado — hoje só o caso da
     * matriz implícita de PF muda alguma coisa.
     */
    private ClientRequest applyBranchRules(ClientRequest clientRequest) {
        List<BranchRequest> branches = clientRequest.branches();

        if (branches == null || branches.isEmpty()) {
            throw new BusinessException("Informe ao menos a matriz");
        }

        // PF não tem filial: a única unidade é a matriz, mesmo que o request não diga.
        if (clientRequest.tipo() == ClientType.PF) {
            if (branches.size() > 1) {
                throw new BusinessException("Cliente pessoa física deve ter apenas a matriz");
            }
            branches = List.of(asMatriz(branches.get(0)));
        }

        long matrizes = branches.stream()
                .filter(branch -> Boolean.TRUE.equals(branch.isMatriz()))
                .count();

        if (matrizes != 1) {
            throw new BusinessException("O cliente deve ter exatamente uma filial marcada como matriz");
        }

        // Os documentos já chegam normalizados pelo compact constructor do BranchRequest,
        // então a comparação é direta.
        Set<String> documentos = new LinkedHashSet<>();
        for (BranchRequest branch : branches) {
            if (branch.documento() == null) {
                throw new BusinessException("O documento é obrigatório para toda filial nova");
            }
            if (!documentos.add(branch.documento())) {
                throw new BusinessException("Documento repetido no mesmo cadastro: " + branch.documento());
            }
        }

        for (String documento : documentos) {
            if (clientBranchRepository.existsByDocumento(documento)) {
                throw new DocumentoAlreadyExistsException("Documento: " + documento + " already exists");
            }
        }

        return withBranches(clientRequest, branches);
    }

    private static BranchRequest asMatriz(BranchRequest branch) {
        if (Boolean.TRUE.equals(branch.isMatriz())) {
            return branch;
        }
        return new BranchRequest(branch.documento(), true, branch.apelido(), branch.inscricaoEstadual(),
                branch.inscricaoMunicipal(), branch.address(), branch.contatos());
    }

    private static ClientRequest withBranches(ClientRequest clientRequest, List<BranchRequest> branches) {
        if (branches == clientRequest.branches()) {
            return clientRequest;
        }
        return new ClientRequest(clientRequest.tipo(), clientRequest.name(), clientRequest.nomeFantasia(),
                clientRequest.regimeTributario(), clientRequest.systemsPublicIds(), branches,
                clientRequest.enabled());
    }

    @Transactional
    public ClientResponse updateClient(UUID publicId, ClientUpdateRequest clientUpdateRequest) {
        Optional<Client> clientOpt = clientRepository.findByPublicId(publicId);

        if (clientOpt.isEmpty()) {
            throw new ClientNotFoundException("Client with public ID: " + publicId + " not found");
        }

        Client client = clientOpt.get();
        List<Systems> associatedSystems = null;
        List<UUID> systemsPublicIds = clientUpdateRequest.systemsPublicIds();

        if (systemsPublicIds != null) {
            associatedSystems = new ArrayList<>(); // se não veio null, o padrão é uma lista vazia
            if (!systemsPublicIds.isEmpty()) { // se não estiver vazia, buscar os sistemas pelo id vindo dela
                associatedSystems = systemService.findAllByPublicIds(systemsPublicIds);
            }
        }

        clientMapper.updateClient(clientUpdateRequest, associatedSystems, client);
        Client savedClient = clientRepository.save(client);
        return clientMapper.toClientResponse(savedClient);
    }

    public Client findByPublicId(UUID publicId) {
        return clientRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ClientNotFoundException("Client with public ID: " + publicId + " not found"));
    }

    @Transactional
    public void softDeleteClient(UUID publicId) {
        Client client = clientRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ClientNotFoundException("Client with public ID: " + publicId + " not found"));
        client.setIsEnabled(false);
        clientRepository.save(client);
    }

    public Client findActiveClient(UUID publicId) {
        Client client = this.findByPublicId(publicId);
        if(!client.getIsEnabled()) {
            throw new BusinessException("Client is not active");
        }
        return client;
    }

}
