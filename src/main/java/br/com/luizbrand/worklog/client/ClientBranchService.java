package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.dto.BranchUpdateRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.client.validation.ContactRules;
import br.com.luizbrand.worklog.client.validation.DocumentoChecksum;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Conflict.DocumentoAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.BranchNotFoundException;
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

        if (ContactRules.hasMultiplePrincipais(branchRequest.contatos())) {
            throw new BusinessException(ContactRules.MENSAGEM_MULTIPLOS_PRINCIPAIS);
        }

        // B6 — a unicidade do documento é global, não por cliente.
        if (clientBranchRepository.existsByDocumento(documento)) {
            throw new DocumentoAlreadyExistsException("Documento: " + documento + " already exists");
        }
    }

    @Transactional
    public BranchResponse updateBranch(UUID clientPublicId, UUID branchPublicId,
                                       BranchUpdateRequest branchUpdateRequest) {
        Client client = clientService.findActiveClient(clientPublicId);
        ClientBranch branch = findBranchOfClient(client, branchPublicId);

        applyUpdateRules(client, branch, branchUpdateRequest);

        clientMapper.updateBranch(branchUpdateRequest, branch);

        // O mapper substitui a lista inteira quando ela vem preenchida (D7), mas
        // não conhece a referência de volta — sem ela o insert falha na FK.
        if (branch.getContatos() != null) {
            branch.getContatos().forEach(contato -> contato.setBranch(branch));
        }

        applyMatrizTransfer(clientPublicId, branch, branchUpdateRequest);

        return clientMapper.toBranchResponse(clientBranchRepository.save(branch));
    }

    /**
     * Regras B8, B9, B11 e 7.5. B13 sai de graça: o documento só é validado
     * quando vem no corpo, então editar uma filial de legado (documento nulo) sem
     * mexer nele é permitido.
     */
    private void applyUpdateRules(Client client, ClientBranch branch, BranchUpdateRequest request) {
        String documento = request.documento();

        if (documento != null) {
            // B9 — o tipo vem do cliente persistido, igual ao create.
            if (!DocumentoChecksum.isValid(client.getTipo(), documento)) {
                throw new BusinessException(DocumentoChecksum.mensagemPara(client.getTipo()));
            }
            // B8 — excluindo a própria filial, senão reenviar o mesmo documento
            // devolveria 409 contra ela mesma.
            if (clientBranchRepository.existsByDocumentoAndPublicIdNot(documento, branch.getPublicId())) {
                throw new DocumentoAlreadyExistsException("Documento: " + documento + " already exists");
            }
        }

        // B11 — rebaixar a matriz sem promover outra deixaria o cliente sem matriz.
        if (Boolean.FALSE.equals(request.isMatriz()) && Boolean.TRUE.equals(branch.getIsMatriz())) {
            throw new BusinessException("A matriz não pode ser rebaixada; promova outra filial a matriz");
        }

        if (Boolean.TRUE.equals(request.isMatriz()) && !Boolean.TRUE.equals(branch.getIsEnabled())) {
            throw new BusinessException("Uma filial inativa não pode ser promovida a matriz");
        }

        if (ContactRules.hasMultiplePrincipais(request.contatos())) {
            throw new BusinessException(ContactRules.MENSAGEM_MULTIPLOS_PRINCIPAIS);
        }
    }

    /**
     * B12 — promover rebaixa a matriz anterior. O {@code saveAndFlush} força o
     * UPDATE do rebaixamento a sair <b>antes</b> da promoção: com o índice único
     * parcial da V13, as duas linhas marcadas como matriz no mesmo instante
     * violariam a constraint.
     */
    private void applyMatrizTransfer(UUID clientPublicId, ClientBranch branch, BranchUpdateRequest request) {
        if (!Boolean.TRUE.equals(request.isMatriz()) || Boolean.TRUE.equals(branch.getIsMatriz())) {
            return;
        }

        clientBranchRepository.findByClientPublicIdAndIsMatriz(clientPublicId, true)
                .forEach(matrizAtual -> {
                    matrizAtual.setIsMatriz(false);
                    clientBranchRepository.saveAndFlush(matrizAtual);
                });

        branch.setIsMatriz(true);
    }

    /**
     * Inativa a filial (soft delete). O documento continua ocupando o UNIQUE
     * global — recadastrá-lo em outro cliente devolve 409 enquanto esta filial
     * existir. Consequência aceita conscientemente na decisão D3.
     */
    @Transactional
    public void deactivateBranch(UUID clientPublicId, UUID branchPublicId) {
        Client client = clientService.findActiveClient(clientPublicId);
        ClientBranch branch = findBranchOfClient(client, branchPublicId);

        // B10 — sem matriz o cadastro fica inconsistente com a regra do create.
        if (Boolean.TRUE.equals(branch.getIsMatriz())) {
            throw new BusinessException("A matriz não pode ser inativada; promova outra filial a matriz antes");
        }

        branch.setIsEnabled(false);
        clientBranchRepository.save(branch);
    }

    /**
     * Reativa uma filial inativa. Contrapartida do
     * {@link #deactivateBranch(UUID, UUID)}.
     *
     * <p><b>Não há checagem de documento aqui de propósito:</b> a filial inativa
     * nunca soltou o UNIQUE global (consequência aceita na decisão D3), então
     * ninguém pôde ter tomado aquele documento no intervalo.
     *
     * <p>Pelo mesmo motivo não existe o caso "reativar a matriz": a B10 impede
     * inativá-la, então nenhuma filial inativa é matriz.
     */
    @Transactional
    public void activateBranch(UUID clientPublicId, UUID branchPublicId) {
        Client client = clientService.findActiveClient(clientPublicId);
        ClientBranch branch = findBranchOfClient(client, branchPublicId);

        // R4 — idempotente. Precisa vir ANTES da regra de PF, senão reativar uma
        // filial já ativa de um cliente PF devolveria 422 sem motivo.
        if (Boolean.TRUE.equals(branch.getIsEnabled())) {
            return;
        }

        // R3 — reativar não pode deixar o cadastro num estado que o create
        // recusaria (B2).
        if (client.getTipo() == ClientType.PF
                && !clientBranchRepository.findByClientPublicIdAndIsEnabled(clientPublicId, true).isEmpty()) {
            throw new BusinessException("Cliente pessoa física deve ter apenas a matriz; "
                    + "não é possível reativar uma segunda filial");
        }

        branch.setIsEnabled(true);
        clientBranchRepository.save(branch);
    }

    /**
     * B7 — filial de outro cliente responde 404, não 403: o 403 confirmaria que
     * aquele id existe em algum lugar.
     */
    private ClientBranch findBranchOfClient(Client client, UUID branchPublicId) {
        ClientBranch branch = clientBranchRepository.findByPublicId(branchPublicId)
                .orElseThrow(() -> new BranchNotFoundException(
                        "Branch with public ID: " + branchPublicId + " not found"));

        if (branch.getClient() == null
                || !client.getPublicId().equals(branch.getClient().getPublicId())) {
            throw new BranchNotFoundException("Branch with public ID: " + branchPublicId + " not found");
        }

        return branch;
    }

}
