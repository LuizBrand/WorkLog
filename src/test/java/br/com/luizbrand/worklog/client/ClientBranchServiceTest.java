package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.dto.BranchUpdateRequest;
import br.com.luizbrand.worklog.client.dto.ContactRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.ContactType;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Conflict.DocumentoAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.BranchNotFoundException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_ALFANUMERICO_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CPF_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.aContact;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.aPrincipalContact;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.filial;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.matriz;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientBranchService")
class ClientBranchServiceTest {

    @Mock
    private ClientService clientService;

    @Mock
    private ClientBranchRepository clientBranchRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientBranchService clientBranchService;

    private UUID clientPublicId;
    private Client client;

    @BeforeEach
    void setUp() {
        clientPublicId = UUID.randomUUID();
        client = new Client();
        client.setPublicId(clientPublicId);
        client.setName("Test Client");
        client.setTipo(ClientType.PJ);
        client.setIsEnabled(true);
    }

    private static ClientBranch aBranch(String documento, boolean isMatriz) {
        ClientBranch branch = new ClientBranch();
        branch.setPublicId(UUID.randomUUID());
        branch.setDocumento(documento);
        branch.setIsMatriz(isMatriz);
        branch.setIsEnabled(true);
        return branch;
    }

    private static BranchResponse aBranchResponse(ClientBranch branch) {
        return new BranchResponse(branch.getPublicId(), branch.getDocumento(), branch.getIsMatriz(),
                branch.getApelido(), null, null, null, List.of(), branch.getIsEnabled());
    }

    @Nested
    @DisplayName("findBranchesByClient")
    class FindBranchesByClient {

        @Test
        @DisplayName("Deve listar as filiais do cliente quando ele existe")
        void shouldListBranchesOfExistingClient() {
            ClientBranch matriz = aBranch(CNPJ_VALIDO, true);
            when(clientService.findByPublicId(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByClientPublicId(clientPublicId)).thenReturn(List.of(matriz));
            when(clientMapper.toBranchResponse(matriz)).thenReturn(aBranchResponse(matriz));

            List<BranchResponse> result = clientBranchService.findBranchesByClient(clientPublicId, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).documento()).isEqualTo(CNPJ_VALIDO);
            assertThat(result.get(0).isMatriz()).isTrue();
        }

        @Test
        @DisplayName("Sem filtro de status, deve trazer ativas e inativas (D4)")
        void shouldReturnAllBranchesWhenStatusIsNull() {
            when(clientService.findByPublicId(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByClientPublicId(clientPublicId)).thenReturn(List.of());

            clientBranchService.findBranchesByClient(clientPublicId, null);

            verify(clientBranchRepository).findByClientPublicId(clientPublicId);
            verify(clientBranchRepository, never()).findByClientPublicIdAndIsEnabled(any(), any());
        }

        @Test
        @DisplayName("StatusFiltro.TODOS também deve trazer ativas e inativas")
        void shouldReturnAllBranchesWhenStatusIsTodos() {
            when(clientService.findByPublicId(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByClientPublicId(clientPublicId)).thenReturn(List.of());

            clientBranchService.findBranchesByClient(clientPublicId, StatusFiltro.TODOS);

            verify(clientBranchRepository).findByClientPublicId(clientPublicId);
            verify(clientBranchRepository, never()).findByClientPublicIdAndIsEnabled(any(), any());
        }

        @Test
        @DisplayName("StatusFiltro.ATIVO deve trazer apenas as habilitadas")
        void shouldReturnOnlyEnabledBranchesWhenStatusIsAtivo() {
            when(clientService.findByPublicId(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByClientPublicIdAndIsEnabled(clientPublicId, true))
                    .thenReturn(List.of());

            clientBranchService.findBranchesByClient(clientPublicId, StatusFiltro.ATIVO);

            verify(clientBranchRepository).findByClientPublicIdAndIsEnabled(clientPublicId, true);
            verify(clientBranchRepository, never()).findByClientPublicId(any());
        }

        @Test
        @DisplayName("StatusFiltro.INATIVO deve trazer apenas as desabilitadas")
        void shouldReturnOnlyDisabledBranchesWhenStatusIsInativo() {
            when(clientService.findByPublicId(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByClientPublicIdAndIsEnabled(clientPublicId, false))
                    .thenReturn(List.of());

            clientBranchService.findBranchesByClient(clientPublicId, StatusFiltro.INATIVO);

            verify(clientBranchRepository).findByClientPublicIdAndIsEnabled(clientPublicId, false);
            verify(clientBranchRepository, never()).findByClientPublicId(any());
        }

        @Test
        @DisplayName("Cliente inexistente deve propagar ClientNotFoundException (404)")
        void shouldPropagateNotFoundWhenClientDoesNotExist() {
            when(clientService.findByPublicId(clientPublicId))
                    .thenThrow(new ClientNotFoundException("Client with public ID: " + clientPublicId + " not found"));

            assertThatThrownBy(() -> clientBranchService.findBranchesByClient(clientPublicId, null))
                    .isInstanceOf(ClientNotFoundException.class);

            verifyNoInteractions(clientBranchRepository);
        }

        @Test
        @DisplayName("Listar filiais de cliente inativo é permitido — só o POST exige cliente ativo")
        void shouldListBranchesOfDisabledClient() {
            client.setIsEnabled(false);
            when(clientService.findByPublicId(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByClientPublicId(clientPublicId)).thenReturn(List.of());

            List<BranchResponse> result = clientBranchService.findBranchesByClient(clientPublicId, null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createBranch")
    class CreateBranch {

        @Test
        @DisplayName("Deve criar a filial e devolver a resposta mapeada (caminho feliz)")
        void shouldCreateBranch() {
            BranchRequest request = filial(CNPJ_ALFANUMERICO_VALIDO);
            ClientBranch mapped = aBranch(CNPJ_ALFANUMERICO_VALIDO, false);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.existsByDocumento(CNPJ_ALFANUMERICO_VALIDO)).thenReturn(false);
            when(clientMapper.toClientBranch(request)).thenReturn(mapped);
            when(clientBranchRepository.save(mapped)).thenReturn(mapped);
            when(clientMapper.toBranchResponse(mapped)).thenReturn(aBranchResponse(mapped));

            BranchResponse result = clientBranchService.createBranch(clientPublicId, request);

            assertThat(result.documento()).isEqualTo(CNPJ_ALFANUMERICO_VALIDO);
            verify(clientBranchRepository).save(mapped);
        }

        @Test
        @DisplayName("Deve vincular a filial ao cliente do path antes de salvar")
        void shouldWireBranchToClientBeforeSaving() {
            BranchRequest request = filial(CNPJ_ALFANUMERICO_VALIDO);
            ClientBranch mapped = aBranch(CNPJ_ALFANUMERICO_VALIDO, false);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.existsByDocumento(anyString())).thenReturn(false);
            when(clientMapper.toClientBranch(request)).thenReturn(mapped);
            when(clientBranchRepository.save(any(ClientBranch.class))).thenReturn(mapped);
            when(clientMapper.toBranchResponse(any(ClientBranch.class))).thenReturn(aBranchResponse(mapped));

            clientBranchService.createBranch(clientPublicId, request);

            ArgumentCaptor<ClientBranch> captor = ArgumentCaptor.forClass(ClientBranch.class);
            verify(clientBranchRepository).save(captor.capture());
            assertThat(captor.getValue().getClient()).isSameAs(client);
        }

        @Test
        @DisplayName("Deve preencher a referência de volta dos contatos — o @AfterMapping só cobre Client")
        void shouldWireContactsBackReferenceBeforeSaving() {
            ContactRequest contactRequest = aContact(ContactType.EMAIL, "filial@empresa.com");
            BranchRequest request = new BranchRequest(CNPJ_ALFANUMERICO_VALIDO, false, "Filial SP",
                    null, null, null, List.of(contactRequest));
            ClientBranch mapped = aBranch(CNPJ_ALFANUMERICO_VALIDO, false);
            ClientContact contact = new ClientContact();
            contact.setTipo(ContactType.EMAIL);
            contact.setValor("filial@empresa.com");
            mapped.setContatos(new java.util.ArrayList<>(List.of(contact)));

            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.existsByDocumento(anyString())).thenReturn(false);
            when(clientMapper.toClientBranch(request)).thenReturn(mapped);
            when(clientBranchRepository.save(any(ClientBranch.class))).thenReturn(mapped);
            when(clientMapper.toBranchResponse(any(ClientBranch.class))).thenReturn(aBranchResponse(mapped));

            clientBranchService.createBranch(clientPublicId, request);

            assertThat(contact.getBranch()).isSameAs(mapped);
        }

        @Test
        @DisplayName("B1 — cliente inexistente deve propagar ClientNotFoundException (404)")
        void shouldPropagateNotFoundWhenClientDoesNotExist() {
            when(clientService.findActiveClient(clientPublicId))
                    .thenThrow(new ClientNotFoundException("Client with public ID: " + clientPublicId + " not found"));

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, filial(CNPJ_ALFANUMERICO_VALIDO)))
                    .isInstanceOf(ClientNotFoundException.class);

            verifyNoInteractions(clientBranchRepository, clientMapper);
        }

        @Test
        @DisplayName("B1 — cliente inativo deve propagar BusinessException (422)")
        void shouldPropagateBusinessExceptionWhenClientIsDisabled() {
            when(clientService.findActiveClient(clientPublicId))
                    .thenThrow(new BusinessException("Client is not active"));

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, filial(CNPJ_ALFANUMERICO_VALIDO)))
                    .isInstanceOf(BusinessException.class);

            verifyNoInteractions(clientBranchRepository, clientMapper);
        }

        @Test
        @DisplayName("B2 — cliente PF não aceita filial adicional (422)")
        void shouldRejectAdditionalBranchForPessoaFisica() {
            client.setTipo(ClientType.PF);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, filial(CPF_VALIDO)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pessoa física");

            verifyNoInteractions(clientMapper);
            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("B3 — isMatriz=true no POST deve ser rejeitado (422, D2)")
        void shouldRejectIsMatrizTrueOnCreate() {
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, matriz(CNPJ_ALFANUMERICO_VALIDO)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("matriz");

            verifyNoInteractions(clientMapper);
            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("B4 — documento é obrigatório na filial nova (422)")
        void shouldRejectMissingDocumento() {
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, filial(null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("documento");

            verifyNoInteractions(clientMapper);
            verify(clientBranchRepository, never()).existsByDocumento(any());
        }

        @Test
        @DisplayName("B5 — documento com checksum inválido para o tipo do cliente (422, D1)")
        void shouldRejectDocumentoWithInvalidChecksum() {
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, filial("12345678000100")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CNPJ inválido");

            verifyNoInteractions(clientMapper);
            verify(clientBranchRepository, never()).existsByDocumento(any());
        }

        @Test
        @DisplayName("B5 — o checksum usa o tipo do cliente persistido, não o do corpo")
        void shouldValidateChecksumAgainstPersistedClientType() {
            // CPF_VALIDO tem checksum de CPF; num cliente PJ ele precisa falhar como CNPJ.
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, filial(CPF_VALIDO)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CNPJ inválido");
        }

        @Test
        @DisplayName("B6 — documento já cadastrado deve dar 409")
        void shouldRejectDuplicatedDocumento() {
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.existsByDocumento(CNPJ_ALFANUMERICO_VALIDO)).thenReturn(true);

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, filial(CNPJ_ALFANUMERICO_VALIDO)))
                    .isInstanceOf(DocumentoAlreadyExistsException.class)
                    .hasMessageContaining(CNPJ_ALFANUMERICO_VALIDO);

            verifyNoInteractions(clientMapper);
            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("existsByDocumento deve ser consultado uma única vez")
        void shouldQueryExistsByDocumentoOnce() {
            BranchRequest request = filial(CNPJ_ALFANUMERICO_VALIDO);
            ClientBranch mapped = aBranch(CNPJ_ALFANUMERICO_VALIDO, false);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.existsByDocumento(CNPJ_ALFANUMERICO_VALIDO)).thenReturn(false);
            when(clientMapper.toClientBranch(request)).thenReturn(mapped);
            when(clientBranchRepository.save(mapped)).thenReturn(mapped);
            when(clientMapper.toBranchResponse(mapped)).thenReturn(aBranchResponse(mapped));

            clientBranchService.createBranch(clientPublicId, request);

            verify(clientBranchRepository, times(1)).existsByDocumento(CNPJ_ALFANUMERICO_VALIDO);
        }

        @Test
        @DisplayName("Cliente PF é rejeitado antes de qualquer consulta de documento")
        void shouldRejectPessoaFisicaBeforeQueryingDocumento() {
            client.setTipo(ClientType.PF);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, filial(CPF_VALIDO)))
                    .isInstanceOf(BusinessException.class);

            verify(clientBranchRepository, never()).existsByDocumento(any());
        }

        @Test
        @DisplayName("Cliente PJ aceita CNPJ puramente numérico")
        void shouldAcceptNumericCnpj() {
            BranchRequest request = filial(CNPJ_VALIDO);
            ClientBranch mapped = aBranch(CNPJ_VALIDO, false);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.existsByDocumento(CNPJ_VALIDO)).thenReturn(false);
            when(clientMapper.toClientBranch(request)).thenReturn(mapped);
            when(clientBranchRepository.save(mapped)).thenReturn(mapped);
            when(clientMapper.toBranchResponse(mapped)).thenReturn(aBranchResponse(mapped));

            BranchResponse result = clientBranchService.createBranch(clientPublicId, request);

            assertThat(result.documento()).isEqualTo(CNPJ_VALIDO);
        }

        @Test
        @DisplayName("7.5 — dois contatos marcados como principal devem ser rejeitados (422)")
        void shouldRejectMultiplePrincipalContactsOnCreate() {
            BranchRequest request = new BranchRequest(CNPJ_ALFANUMERICO_VALIDO, false, null, null, null, null,
                    List.of(aPrincipalContact(ContactType.EMAIL, "a@empresa.com"),
                            aPrincipalContact(ContactType.TELEFONE, "1133334444")));
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);

            assertThatThrownBy(() -> clientBranchService.createBranch(clientPublicId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("principal");

            verifyNoInteractions(clientMapper);
            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("7.5 — um único contato principal é aceito")
        void shouldAcceptSinglePrincipalContactOnCreate() {
            BranchRequest request = new BranchRequest(CNPJ_ALFANUMERICO_VALIDO, false, null, null, null, null,
                    List.of(aPrincipalContact(ContactType.EMAIL, "a@empresa.com"),
                            aContact(ContactType.TELEFONE, "1133334444")));
            ClientBranch mapped = aBranch(CNPJ_ALFANUMERICO_VALIDO, false);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.existsByDocumento(anyString())).thenReturn(false);
            when(clientMapper.toClientBranch(request)).thenReturn(mapped);
            when(clientBranchRepository.save(mapped)).thenReturn(mapped);
            when(clientMapper.toBranchResponse(mapped)).thenReturn(aBranchResponse(mapped));

            assertThat(clientBranchService.createBranch(clientPublicId, request)).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateBranch")
    class UpdateBranch {

        private UUID branchPublicId;
        private ClientBranch branch;

        @BeforeEach
        void setUpBranch() {
            branch = aBranch(CNPJ_ALFANUMERICO_VALIDO, false);
            branch.setClient(client);
            branch.setApelido("Filial SP");
            branchPublicId = branch.getPublicId();
        }

        private BranchUpdateRequest onlyApelido(String apelido) {
            return new BranchUpdateRequest(null, null, apelido, null, null, null, null);
        }

        @Test
        @DisplayName("Deve aplicar o PATCH parcial e devolver a filial atualizada")
        void shouldApplyPartialUpdate() {
            BranchUpdateRequest request = onlyApelido("Filial Campinas");
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.save(branch)).thenReturn(branch);
            when(clientMapper.toBranchResponse(branch)).thenReturn(aBranchResponse(branch));

            BranchResponse result = clientBranchService.updateBranch(clientPublicId, branchPublicId, request);

            assertThat(result).isNotNull();
            verify(clientMapper).updateBranch(request, branch);
            verify(clientBranchRepository).save(branch);
        }

        @Test
        @DisplayName("PATCH sem mexer no documento NÃO deve consultar unicidade — o bug do 409 contra si mesma")
        void shouldNotCheckUniquenessWhenDocumentoIsUnchanged() {
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.save(branch)).thenReturn(branch);
            when(clientMapper.toBranchResponse(branch)).thenReturn(aBranchResponse(branch));

            clientBranchService.updateBranch(clientPublicId, branchPublicId, onlyApelido("Novo apelido"));

            verify(clientBranchRepository, never()).existsByDocumento(any());
            verify(clientBranchRepository, never()).existsByDocumentoAndPublicIdNot(any(), any());
        }

        @Test
        @DisplayName("Reenviar o PRÓPRIO documento não deve dar 409")
        void shouldAcceptResendingOwnDocumento() {
            BranchUpdateRequest request = new BranchUpdateRequest(CNPJ_ALFANUMERICO_VALIDO, null, null,
                    null, null, null, null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.existsByDocumentoAndPublicIdNot(CNPJ_ALFANUMERICO_VALIDO, branchPublicId))
                    .thenReturn(false);
            when(clientBranchRepository.save(branch)).thenReturn(branch);
            when(clientMapper.toBranchResponse(branch)).thenReturn(aBranchResponse(branch));

            assertThat(clientBranchService.updateBranch(clientPublicId, branchPublicId, request)).isNotNull();

            verify(clientBranchRepository).existsByDocumentoAndPublicIdNot(CNPJ_ALFANUMERICO_VALIDO, branchPublicId);
        }

        @Test
        @DisplayName("B8 — documento pertencente a OUTRA filial deve dar 409")
        void shouldRejectDocumentoOwnedByAnotherBranch() {
            BranchUpdateRequest request = new BranchUpdateRequest(CNPJ_VALIDO, null, null, null, null, null, null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.existsByDocumentoAndPublicIdNot(CNPJ_VALIDO, branchPublicId))
                    .thenReturn(true);

            assertThatThrownBy(() -> clientBranchService.updateBranch(clientPublicId, branchPublicId, request))
                    .isInstanceOf(DocumentoAlreadyExistsException.class);

            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("B9 — documento novo com checksum inválido deve dar 422")
        void shouldRejectNewDocumentoWithInvalidChecksum() {
            BranchUpdateRequest request = new BranchUpdateRequest("12345678000100", null, null, null, null, null, null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            assertThatThrownBy(() -> clientBranchService.updateBranch(clientPublicId, branchPublicId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CNPJ inválido");

            verify(clientBranchRepository, never()).existsByDocumentoAndPublicIdNot(any(), any());
        }

        @Test
        @DisplayName("B7 — filial de outro cliente deve dar 404, não 403")
        void shouldReturnNotFoundWhenBranchBelongsToAnotherClient() {
            Client outroCliente = new Client();
            outroCliente.setPublicId(UUID.randomUUID());
            branch.setClient(outroCliente);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            assertThatThrownBy(() -> clientBranchService.updateBranch(clientPublicId, branchPublicId,
                    onlyApelido("Tentativa")))
                    .isInstanceOf(BranchNotFoundException.class);

            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Filial inexistente deve dar 404")
        void shouldReturnNotFoundWhenBranchDoesNotExist() {
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientBranchService.updateBranch(clientPublicId, branchPublicId,
                    onlyApelido("Tentativa")))
                    .isInstanceOf(BranchNotFoundException.class);
        }

        @Test
        @DisplayName("B13 — editar filial de legado SEM documento é permitido")
        void shouldAllowEditingLegacyBranchWithoutDocumento() {
            branch.setDocumento(null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.save(branch)).thenReturn(branch);
            when(clientMapper.toBranchResponse(branch)).thenReturn(aBranchResponse(branch));

            assertThat(clientBranchService.updateBranch(clientPublicId, branchPublicId,
                    onlyApelido("Matriz Legado"))).isNotNull();
        }

        @Test
        @DisplayName("B11 — rebaixar a matriz sem promover outra deve dar 422")
        void shouldRejectDemotingMatrizWithoutPromotingAnother() {
            branch.setIsMatriz(true);
            BranchUpdateRequest request = new BranchUpdateRequest(null, false, null, null, null, null, null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            assertThatThrownBy(() -> clientBranchService.updateBranch(clientPublicId, branchPublicId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("matriz");

            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("isMatriz=false numa filial comum é inócuo, não deve dar erro")
        void shouldAcceptIsMatrizFalseOnRegularBranch() {
            BranchUpdateRequest request = new BranchUpdateRequest(null, false, null, null, null, null, null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.save(branch)).thenReturn(branch);
            when(clientMapper.toBranchResponse(branch)).thenReturn(aBranchResponse(branch));

            assertThat(clientBranchService.updateBranch(clientPublicId, branchPublicId, request)).isNotNull();
            assertThat(branch.getIsMatriz()).isFalse();
        }

        @Test
        @DisplayName("B12 — promover filial a matriz deve rebaixar a anterior")
        void shouldDemotePreviousMatrizWhenPromoting() {
            ClientBranch matrizAtual = aBranch(CNPJ_VALIDO, true);
            matrizAtual.setClient(client);
            BranchUpdateRequest request = new BranchUpdateRequest(null, true, null, null, null, null, null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.findByClientPublicIdAndIsMatriz(clientPublicId, true))
                    .thenReturn(List.of(matrizAtual));
            when(clientBranchRepository.save(branch)).thenReturn(branch);
            when(clientMapper.toBranchResponse(branch)).thenReturn(aBranchResponse(branch));

            clientBranchService.updateBranch(clientPublicId, branchPublicId, request);

            assertThat(matrizAtual.getIsMatriz()).isFalse();
            assertThat(branch.getIsMatriz()).isTrue();
        }

        @Test
        @DisplayName("B12 — a matriz anterior deve ser gravada com flush ANTES da promoção (índice único da V13)")
        void shouldFlushDemotionBeforePromotion() {
            ClientBranch matrizAtual = aBranch(CNPJ_VALIDO, true);
            matrizAtual.setClient(client);
            BranchUpdateRequest request = new BranchUpdateRequest(null, true, null, null, null, null, null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.findByClientPublicIdAndIsMatriz(clientPublicId, true))
                    .thenReturn(List.of(matrizAtual));
            when(clientBranchRepository.save(branch)).thenReturn(branch);
            when(clientMapper.toBranchResponse(branch)).thenReturn(aBranchResponse(branch));

            clientBranchService.updateBranch(clientPublicId, branchPublicId, request);

            InOrder inOrder = inOrder(clientBranchRepository);
            inOrder.verify(clientBranchRepository).saveAndFlush(matrizAtual);
            inOrder.verify(clientBranchRepository).save(branch);
        }

        @Test
        @DisplayName("B12 — promover a filial que JÁ é matriz não deve rebaixar ninguém")
        void shouldNotDemoteAnythingWhenBranchIsAlreadyMatriz() {
            branch.setIsMatriz(true);
            BranchUpdateRequest request = new BranchUpdateRequest(null, true, null, null, null, null, null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.save(branch)).thenReturn(branch);
            when(clientMapper.toBranchResponse(branch)).thenReturn(aBranchResponse(branch));

            clientBranchService.updateBranch(clientPublicId, branchPublicId, request);

            verify(clientBranchRepository, never()).findByClientPublicIdAndIsMatriz(any(), any());
            verify(clientBranchRepository, never()).saveAndFlush(any());
            assertThat(branch.getIsMatriz()).isTrue();
        }

        @Test
        @DisplayName("Não deve promover filial inativa a matriz (422)")
        void shouldRejectPromotingDisabledBranch() {
            branch.setIsEnabled(false);
            BranchUpdateRequest request = new BranchUpdateRequest(null, true, null, null, null, null, null);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            assertThatThrownBy(() -> clientBranchService.updateBranch(clientPublicId, branchPublicId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("inativa");
        }

        @Test
        @DisplayName("Deve preencher a referência de volta dos contatos substituídos")
        void shouldWireReplacedContactsBackReference() {
            ClientContact contact = new ClientContact();
            contact.setTipo(ContactType.EMAIL);
            contact.setValor("novo@empresa.com");
            branch.setContatos(new java.util.ArrayList<>(List.of(contact)));
            BranchUpdateRequest request = new BranchUpdateRequest(null, null, null, null, null, null,
                    List.of(aContact(ContactType.EMAIL, "novo@empresa.com")));
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));
            when(clientBranchRepository.save(branch)).thenReturn(branch);
            when(clientMapper.toBranchResponse(branch)).thenReturn(aBranchResponse(branch));

            clientBranchService.updateBranch(clientPublicId, branchPublicId, request);

            assertThat(contact.getBranch()).isSameAs(branch);
        }

        @Test
        @DisplayName("7.5 — dois contatos principal no PATCH devem ser rejeitados (422)")
        void shouldRejectMultiplePrincipalContactsOnUpdate() {
            BranchUpdateRequest request = new BranchUpdateRequest(null, null, null, null, null, null,
                    List.of(aPrincipalContact(ContactType.EMAIL, "a@empresa.com"),
                            aPrincipalContact(ContactType.TELEFONE, "1133334444")));
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            assertThatThrownBy(() -> clientBranchService.updateBranch(clientPublicId, branchPublicId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("principal");

            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cliente inativo deve propagar BusinessException (422)")
        void shouldPropagateBusinessExceptionWhenClientIsDisabled() {
            when(clientService.findActiveClient(clientPublicId))
                    .thenThrow(new BusinessException("Client is not active"));

            assertThatThrownBy(() -> clientBranchService.updateBranch(clientPublicId, branchPublicId,
                    onlyApelido("Tentativa")))
                    .isInstanceOf(BusinessException.class);

            verifyNoInteractions(clientMapper);
        }
    }

    @Nested
    @DisplayName("deactivateBranch")
    class DeactivateBranch {

        private UUID branchPublicId;
        private ClientBranch branch;

        @BeforeEach
        void setUpBranch() {
            branch = aBranch(CNPJ_ALFANUMERICO_VALIDO, false);
            branch.setClient(client);
            branchPublicId = branch.getPublicId();
        }

        @Test
        @DisplayName("Deve inativar a filial (soft delete, D3)")
        void shouldDeactivateBranch() {
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            clientBranchService.deactivateBranch(clientPublicId, branchPublicId);

            assertThat(branch.getIsEnabled()).isFalse();
            verify(clientBranchRepository).save(branch);
        }

        @Test
        @DisplayName("Não deve apagar o documento ao inativar — o UNIQUE global segue ocupado (D3)")
        void shouldKeepDocumentoWhenDeactivating() {
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            clientBranchService.deactivateBranch(clientPublicId, branchPublicId);

            assertThat(branch.getDocumento()).isEqualTo(CNPJ_ALFANUMERICO_VALIDO);
        }

        @Test
        @DisplayName("B10 — a matriz não pode ser inativada (422)")
        void shouldRejectDeactivatingMatriz() {
            branch.setIsMatriz(true);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            assertThatThrownBy(() -> clientBranchService.deactivateBranch(clientPublicId, branchPublicId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("matriz");

            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("B7 — filial de outro cliente deve dar 404")
        void shouldReturnNotFoundWhenBranchBelongsToAnotherClient() {
            Client outroCliente = new Client();
            outroCliente.setPublicId(UUID.randomUUID());
            branch.setClient(outroCliente);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            assertThatThrownBy(() -> clientBranchService.deactivateBranch(clientPublicId, branchPublicId))
                    .isInstanceOf(BranchNotFoundException.class);

            verify(clientBranchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Filial inexistente deve dar 404")
        void shouldReturnNotFoundWhenBranchDoesNotExist() {
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientBranchService.deactivateBranch(clientPublicId, branchPublicId))
                    .isInstanceOf(BranchNotFoundException.class);
        }

        @Test
        @DisplayName("Inativar filial já inativa é idempotente")
        void shouldBeIdempotent() {
            branch.setIsEnabled(false);
            when(clientService.findActiveClient(clientPublicId)).thenReturn(client);
            when(clientBranchRepository.findByPublicId(branchPublicId)).thenReturn(Optional.of(branch));

            clientBranchService.deactivateBranch(clientPublicId, branchPublicId);

            assertThat(branch.getIsEnabled()).isFalse();
        }
    }
}
