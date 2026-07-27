package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.client.dto.ClientUpdateRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Conflict.ClientAlreadyExistsException;
import br.com.luizbrand.worklog.exception.Conflict.DocumentoAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
import br.com.luizbrand.worklog.support.ClientTestBuilder;
import br.com.luizbrand.worklog.system.SystemService;
import br.com.luizbrand.worklog.system.Systems;
import org.springframework.data.jpa.domain.Specification;
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

import java.time.OffsetDateTime;
import java.util.*;

import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_ALFANUMERICO_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CPF_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.aClientRequest;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.branch;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.filial;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.matriz;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private SystemService systemService;

    @Mock
    private ClientBranchRepository clientBranchRepository;

    @InjectMocks
    private ClientService clientService;

    private Client client;
    private ClientResponse clientResponse;
    private UUID publicId;

    @BeforeEach
    void setUp() {
        publicId = UUID.randomUUID();
        client = new Client();
        client.setPublicId(publicId);
        client.setName("Test Client");
        client.setIsEnabled(true);

        clientResponse = aClientResponse(publicId, "Test Client");
    }

    private static ClientUpdateRequest anUpdateRequest(String name, Boolean enabled) {
        return new ClientUpdateRequest(null, name, null, null, Collections.emptyList(), enabled);
    }

    private static ClientResponse aClientResponse(UUID publicId, String name) {
        return new ClientResponse(
                publicId,
                ClientType.PJ,
                name,
                null,
                null,
                true,
                OffsetDateTime.now(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Nested
    @DisplayName("Method: getClientByPublicId()")
    class GetClientByPublicId {

        @Test
        @DisplayName("Should return ClientResponse when client exists")
        void shouldReturnClientResponseWhenExists() {
            when(clientRepository.findByPublicId(publicId)).thenReturn(Optional.of(client));
            when(clientMapper.toClientResponse(client)).thenReturn(clientResponse);

            ClientResponse result = clientService.getClientByPublicId(publicId);

            assertNotNull(result);
            assertEquals(clientResponse.publicId(), result.publicId());
            verify(clientRepository).findByPublicId(publicId);
        }

        @Test
        @DisplayName("Should throw ClientNotFoundException when client does not exist")
        void shouldThrowExceptionWhenClientNotFound() {
            when(clientRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThrows(ClientNotFoundException.class, () -> clientService.getClientByPublicId(publicId));
        }
    }

    @Nested
    @DisplayName("Method: createClient()")
    class CreateClient {

        @Test
        @DisplayName("Should create and return ClientResponse when name is unique")
        void shouldCreateClientWhenNameIsUnique() {
            ClientRequest request = aClientRequest().withName("Test Client").build();


            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());
            when(clientMapper.toClient(any(ClientRequest.class), anyList())).thenReturn(client);
            when(clientRepository.save(client)).thenReturn(client);
            when(clientMapper.toClientResponse(client)).thenReturn(clientResponse);

            ClientResponse result = clientService.createClient(request);

            assertNotNull(result);
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("Should throw ClientAlreadyExistsException when name already exists")
        void shouldThrowExceptionWhenNameExists() {
            ClientRequest request = aClientRequest().withName("Test Client").build();
            when(clientRepository.findByName(request.name())).thenReturn(Optional.of(client));

            assertThrows(ClientAlreadyExistsException.class, () -> clientService.createClient(request));
            verify(clientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Method: updateClient()")
    class UpdateClient {

        @Test
        @DisplayName("Should update and return ClientResponse when client exists")
        void shouldUpdateClientWhenExists() {
            ClientUpdateRequest request = anUpdateRequest("Updated Name", null);


            when(clientRepository.findByPublicId(publicId)).thenReturn(Optional.of(client));
            when(clientRepository.save(client)).thenReturn(client);
            when(clientMapper.toClientResponse(client)).thenReturn(clientResponse);

            ClientResponse result = clientService.updateClient(publicId, request);

            assertNotNull(result);
            verify(clientMapper).updateClient(eq(request), anyList(), eq(client));
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("Should throw ClientNotFoundException when updating non-existent client")
        void shouldThrowExceptionWhenUpdatingNonExistentClient() {
            ClientUpdateRequest request = anUpdateRequest("Updated Name", null);
            when(clientRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThrows(ClientNotFoundException.class, () -> clientService.updateClient(publicId, request));
        }

        @Test
        @DisplayName("Should forward enabled=false in the request to the mapper")
        void shouldForwardEnabledFalseToMapper() {
            ClientUpdateRequest request = anUpdateRequest("Updated Name", false);

            when(clientRepository.findByPublicId(publicId)).thenReturn(Optional.of(client));
            when(clientRepository.save(client)).thenReturn(client);
            when(clientMapper.toClientResponse(client)).thenReturn(clientResponse);

            clientService.updateClient(publicId, request);

            verify(clientMapper).updateClient(eq(request), anyList(), eq(client));
        }
    }

    @Nested
    @DisplayName("Method: findAllClients()")
    class FindAllClients {

        @Test
        @DisplayName("Should delegate to the repository with the filter specification and map each result")
        void shouldReturnMappedList() {
            ClientFiltersParams filters = new ClientFiltersParams("acme", StatusFiltro.ATIVO, Collections.emptyList(), null, null);
            Client other = ClientTestBuilder.aClient().withName("Other").build();
            ClientResponse otherResponse = aClientResponse(other.getPublicId(), other.getName());

            when(clientRepository.findAll(any(Specification.class))).thenReturn(List.of(client, other));
            when(clientMapper.toClientResponse(client)).thenReturn(clientResponse);
            when(clientMapper.toClientResponse(other)).thenReturn(otherResponse);

            List<ClientResponse> result = clientService.findAllClients(filters);

            assertEquals(2, result.size());
            assertEquals(clientResponse, result.get(0));
            assertEquals(otherResponse, result.get(1));
        }

        @Test
        @DisplayName("Should return an empty list when the repository finds nothing")
        void shouldReturnEmptyListWhenNoMatches() {
            ClientFiltersParams filters = new ClientFiltersParams(null, null, null, null, null);
            when(clientRepository.findAll(any(Specification.class))).thenReturn(List.of());

            List<ClientResponse> result = clientService.findAllClients(filters);

            assertTrue(result.isEmpty());
            verifyNoInteractions(clientMapper);
        }
    }

    @Nested
    @DisplayName("Method: createClient() with associated systems")
    class CreateClientWithSystems {

        @Test
        @DisplayName("Should resolve associated systems when systemsPublicIds is provided")
        void shouldResolveSystemsWhenPublicIdsProvided() {
            UUID systemId = UUID.randomUUID();
            ClientRequest request = aClientRequest().withName("New Client").withSystem(systemId).build();
            Systems system = new Systems();
            system.setPublicId(systemId);
            List<Systems> associated = List.of(system);

            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());
            when(systemService.findAllByPublicIds(request.systemsPublicIds())).thenReturn(associated);
            when(clientMapper.toClient(request, associated)).thenReturn(client);
            when(clientRepository.save(client)).thenReturn(client);
            when(clientMapper.toClientResponse(client)).thenReturn(clientResponse);

            ClientResponse result = clientService.createClient(request);

            assertNotNull(result);
            verify(systemService, times(1)).findAllByPublicIds(request.systemsPublicIds());
            verify(clientMapper, times(1)).toClient(request, associated);
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("Should not call SystemService when systemsPublicIds is null")
        void shouldNotResolveSystemsWhenPublicIdsNull() {
            ClientRequest request = aClientRequest().withName("New Client").withSystems(null).build();

            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());
            when(clientMapper.toClient(eq(request), anyList())).thenReturn(client);
            when(clientRepository.save(client)).thenReturn(client);
            when(clientMapper.toClientResponse(client)).thenReturn(clientResponse);

            clientService.createClient(request);

            verifyNoInteractions(systemService);
        }
    }

    @Nested
    @DisplayName("Method: createClient() — regras de filial e documento")
    class CreateClientBusinessRules {

        private void stubHappyPath(ClientRequest request) {
            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());
            when(clientMapper.toClient(any(ClientRequest.class), anyList())).thenReturn(client);
            when(clientRepository.save(client)).thenReturn(client);
            when(clientMapper.toClientResponse(client)).thenReturn(clientResponse);
        }

        @Test
        @DisplayName("Should persist a PJ client with exactly one matriz")
        void shouldCreatePjWithSingleMatriz() {
            ClientRequest request = aClientRequest()
                    .withBranches(matriz(CNPJ_VALIDO))
                    .build();
            stubHappyPath(request);

            ClientResponse result = clientService.createClient(request);

            assertNotNull(result);
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("Should reject a payload with two branches flagged as matriz (R2)")
        void shouldRejectTwoMatrizes() {
            ClientRequest request = aClientRequest()
                    .withBranches(matriz(CNPJ_VALIDO), matriz(CNPJ_ALFANUMERICO_VALIDO))
                    .build();
            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> clientService.createClient(request));
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject a payload with no branch flagged as matriz (R2)")
        void shouldRejectWithoutMatriz() {
            ClientRequest request = aClientRequest()
                    .withBranches(filial(CNPJ_VALIDO), filial(CNPJ_ALFANUMERICO_VALIDO))
                    .build();
            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> clientService.createClient(request));
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject a PF client with more than one branch (R3)")
        void shouldRejectPfWithMultipleBranches() {
            ClientRequest request = aClientRequest()
                    .withTipo(ClientType.PF)
                    .withBranches(matriz(CPF_VALIDO), filial(CNPJ_VALIDO))
                    .build();
            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> clientService.createClient(request));
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should promote the single branch of a PF client to matriz (R3)")
        void shouldPromoteSingleBranchToMatrizForPf() {
            ClientRequest request = aClientRequest()
                    .withTipo(ClientType.PF)
                    .withBranches(branch(CPF_VALIDO, null))
                    .build();
            stubHappyPath(request);

            clientService.createClient(request);

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(clientMapper).toClient(captor.capture(), anyList());
            assertEquals(1, captor.getValue().branches().size());
            assertTrue(captor.getValue().branches().get(0).isMatriz());
        }

        @Test
        @DisplayName("Should reject a new branch without documento (R4)")
        void shouldRejectBranchWithoutDocumento() {
            ClientRequest request = aClientRequest()
                    .withBranches(matriz(null))
                    .build();
            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> clientService.createClient(request));
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject the same documento twice inside the payload (R5)")
        void shouldRejectDuplicateDocumentoInPayload() {
            ClientRequest request = aClientRequest()
                    .withBranches(matriz(CNPJ_VALIDO), filial(CNPJ_VALIDO))
                    .build();
            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> clientService.createClient(request));
            verifyNoInteractions(clientBranchRepository);
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DocumentoAlreadyExistsException when the documento is taken (R6)")
        void shouldRejectDocumentoAlreadyPersisted() {
            ClientRequest request = aClientRequest()
                    .withBranches(matriz(CNPJ_VALIDO))
                    .build();
            when(clientRepository.findByName(request.name())).thenReturn(Optional.empty());
            when(clientBranchRepository.existsByDocumento(CNPJ_VALIDO)).thenReturn(true);

            assertThrows(DocumentoAlreadyExistsException.class, () -> clientService.createClient(request));
            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should check existsByDocumento once per branch in the payload (R6)")
        void shouldCheckEveryBranchDocumento() {
            ClientRequest request = aClientRequest()
                    .withBranches(matriz(CNPJ_VALIDO), filial(CNPJ_ALFANUMERICO_VALIDO))
                    .build();
            stubHappyPath(request);

            clientService.createClient(request);

            verify(clientBranchRepository, times(1)).existsByDocumento(CNPJ_VALIDO);
            verify(clientBranchRepository, times(1)).existsByDocumento(CNPJ_ALFANUMERICO_VALIDO);
        }

        @Test
        @DisplayName("Should reject a duplicated name before querying any documento")
        void shouldCheckNameBeforeDocumento() {
            ClientRequest request = aClientRequest()
                    .withBranches(matriz(CNPJ_VALIDO))
                    .build();
            when(clientRepository.findByName(request.name())).thenReturn(Optional.of(client));

            assertThrows(ClientAlreadyExistsException.class, () -> clientService.createClient(request));
            verifyNoInteractions(clientBranchRepository);
        }
    }

    @Nested
    @DisplayName("Method: softDeleteClient()")
    class SoftDeleteClient {

        @Test
        @DisplayName("Should set isEnabled to false and persist the client")
        void shouldSoftDeleteWhenClientExists() {
            Client existing = ClientTestBuilder.aClient().build();
            existing.setIsEnabled(true);

            when(clientRepository.findByPublicId(existing.getPublicId())).thenReturn(Optional.of(existing));
            when(clientRepository.save(existing)).thenReturn(existing);

            clientService.softDeleteClient(existing.getPublicId());

            assertFalse(existing.getIsEnabled());
            verify(clientRepository, times(1)).save(existing);
        }

        @Test
        @DisplayName("Should throw ClientNotFoundException when the client does not exist")
        void shouldThrowWhenMissing() {
            UUID missing = UUID.randomUUID();
            when(clientRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            ClientNotFoundException ex = assertThrows(ClientNotFoundException.class,
                    () -> clientService.softDeleteClient(missing));

            assertTrue(ex.getMessage().contains(missing.toString()));
            verify(clientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Method: findActiveClient()")
    class FindActiveClient {

        @Test
        @DisplayName("Should return the entity when the client is enabled")
        void shouldReturnActiveClient() {
            Client enabled = ClientTestBuilder.aClient().build();
            when(clientRepository.findByPublicId(enabled.getPublicId())).thenReturn(Optional.of(enabled));

            Client result = clientService.findActiveClient(enabled.getPublicId());

            assertEquals(enabled, result);
        }

        @Test
        @DisplayName("Should throw BusinessException when the client is disabled")
        void shouldThrowWhenDisabled() {
            Client disabled = ClientTestBuilder.aClient().disabled().build();
            when(clientRepository.findByPublicId(disabled.getPublicId())).thenReturn(Optional.of(disabled));

            assertThrows(BusinessException.class,
                    () -> clientService.findActiveClient(disabled.getPublicId()));
        }

        @Test
        @DisplayName("Should propagate ClientNotFoundException when the client does not exist")
        void shouldPropagateNotFound() {
            UUID missing = UUID.randomUUID();
            when(clientRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            assertThrows(ClientNotFoundException.class,
                    () -> clientService.findActiveClient(missing));
        }
    }
}
