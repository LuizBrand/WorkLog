package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.dto.ContactRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.ContactType;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Conflict.DocumentoAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_ALFANUMERICO_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CPF_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.aContact;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.filial;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.matriz;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    }
}
