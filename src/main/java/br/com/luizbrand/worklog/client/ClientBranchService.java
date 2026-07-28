package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.client.validation.DocumentoChecksum;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Conflict.DocumentoAlreadyExistsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Filiais como sub-recurso de um cliente. Só existe a partir da fase 4: até
 * então uma filial nascia junto com o cliente e era imutável pela API.
 */
@Service
public class ClientBranchService {

    private final ClientService clientService;
    private final ClientBranchRepository clientBranchRepository;
    private final ClientMapper clientMapper;

    public ClientBranchService(ClientService clientService, ClientBranchRepository clientBranchRepository,
                               ClientMapper clientMapper) {
        this.clientService = clientService;
        this.clientBranchRepository = clientBranchRepository;
        this.clientMapper = clientMapper;
    }

    /**
     * Sem {@code status} devolve ativas e inativas, espelhando o
     * {@code GET /clients}. Exige apenas que o cliente exista — inspecionar as
     * filiais de um cliente desativado é legítimo; só a criação exige cliente
     * ativo.
     */
    public List<BranchResponse> findBranchesByClient(UUID clientPublicId, StatusFiltro status) {
        clientService.findByPublicId(clientPublicId);

        List<ClientBranch> branches = switch (status == null ? StatusFiltro.TODOS : status) {
            case ATIVO -> clientBranchRepository.findByClientPublicIdAndIsEnabled(clientPublicId, true);
            case INATIVO -> clientBranchRepository.findByClientPublicIdAndIsEnabled(clientPublicId, false);
            case TODOS -> clientBranchRepository.findByClientPublicId(clientPublicId);
        };

        return branches.stream()
                .map(clientMapper::toBranchResponse)
                .toList();
    }

    @Transactional
    public BranchResponse createBranch(UUID clientPublicId, BranchRequest branchRequest) {
        Client client = clientService.findActiveClient(clientPublicId);

        applyCreateRules(client, branchRequest);

        ClientBranch branch = clientMapper.toClientBranch(branchRequest);
        branch.setClient(client);
        // O @AfterMapping do mapper só dispara para alvo Client, então a
        // referência de volta dos contatos precisa ser feita aqui — sem ela o
        // insert falha na FK NOT NULL de client_contacts.branch_id.
        if (branch.getContatos() != null) {
            branch.getContatos().forEach(contato -> contato.setBranch(branch));
        }

        return clientMapper.toBranchResponse(clientBranchRepository.save(branch));
    }

    /**
     * Regras B1–B6 da fase 4A. A ordem importa: as checagens que não tocam o
     * banco vêm antes das que consultam.
     */
    private void applyCreateRules(Client client, BranchRequest branchRequest) {
        // B2 — PF tem uma única unidade, que é a matriz criada junto com o cliente.
        if (client.getTipo() == ClientType.PF) {
            throw new BusinessException("Cliente pessoa física deve ter apenas a matriz");
        }

        // B3 — trocar a matriz é operação deliberada (PATCH), não efeito colateral
        // de um cadastro.
        if (Boolean.TRUE.equals(branchRequest.isMatriz())) {
            throw new BusinessException("A matriz não pode ser definida no cadastro da filial; "
                    + "promova a filial depois de criada");
        }

        String documento = branchRequest.documento();

        // B4 — só as filiais de legado podem ficar sem documento.
        if (documento == null) {
            throw new BusinessException("O documento é obrigatório para toda filial nova");
        }

        // B5 — o tipo vem do cliente persistido, não do corpo: pedir de novo
        // permitiria ao chamador mentir sobre ele.
        if (!DocumentoChecksum.isValid(client.getTipo(), documento)) {
            throw new BusinessException(DocumentoChecksum.mensagemPara(client.getTipo()));
        }

        // B6 — a unicidade do documento é global, não por cliente.
        if (clientBranchRepository.existsByDocumento(documento)) {
            throw new DocumentoAlreadyExistsException("Documento: " + documento + " already exists");
        }
    }

}
