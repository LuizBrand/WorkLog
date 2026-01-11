package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.exception.Conflict.ClientAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
import br.com.luizbrand.worklog.system.SystemService;
import br.com.luizbrand.worklog.system.Systems;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

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

        clientResponse = new ClientResponse(
                publicId,
                "Test Client",
                true,
                OffsetDateTime.now(),
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
            ClientRequest request = new ClientRequest("Test Client", Collections.emptyList());
            
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
            ClientRequest request = new ClientRequest("Test Client", Collections.emptyList());
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
            ClientRequest request = new ClientRequest("Updated Name", Collections.emptyList());
            
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
            ClientRequest request = new ClientRequest("Updated Name", Collections.emptyList());
            when(clientRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThrows(ClientNotFoundException.class, () -> clientService.updateClient(publicId, request));
        }
    }

    @ActiveProfiles("test")
    @ExtendWith(MockitoExtension.class)
    static
    class ClientMapperTest {

        private final ClientMapper clientMapper = Mappers.getMapper(ClientMapper.class);

        private LocalDateTime dateTime;

        @BeforeEach
        void setUp() {
            dateTime = LocalDateTime.of(2025, 9, 27, 10, 30, 0);
        }

        @Nested
        @DisplayName("Method: toClient()")
        class ToClient {

            @Test
            @DisplayName("Should map ClientRequest and Systems to Client correctly")
            void shouldMapToClient() {
                UUID systemId = UUID.randomUUID();
                ClientRequest request = new ClientRequest("New Client", List.of(systemId));
                Systems system = new Systems();
                system.setPublicId(systemId);
                system.setName("System 1");
                List<Systems> systemsList = List.of(system);

                Client client = clientMapper.toClient(request, systemsList);

                assertNotNull(client);
                assertEquals(request.name(), client.getName());
                assertTrue(client.getIsEnabled());
                assertEquals(systemsList, client.getSystems());
                assertNull(client.getId());
                assertNull(client.getPublicId());
                assertNull(client.getCreatedAt());
                assertNull(client.getUpdatedAt());
            }
        }

        @Nested
        @DisplayName("Method: toClientResponse()")
        class ToClientResponse {

            @Test
            @DisplayName("Should map Client to ClientResponse correctly")
            void shouldMapToClientResponse() {
                UUID clientPublicId = UUID.randomUUID();
                UUID systemPublicId = UUID.randomUUID();

                Systems system = new Systems();
                system.setPublicId(systemPublicId);
                system.setName("System A");

                Client client = new Client();
                client.setId(1L);
                client.setPublicId(clientPublicId);
                client.setName("Client A");
                client.setIsEnabled(true);
                client.setCreatedAt(dateTime);
                client.setSystems(List.of(system));

                ClientResponse response = clientMapper.toClientResponse(client);

                assertNotNull(response);
                assertEquals(client.getPublicId(), response.publicId());
                assertEquals(client.getName(), response.name());
                assertEquals(client.getIsEnabled(), response.enabled());
                assertEquals(client.getCreatedAt().atOffset(ZoneOffset.UTC), response.createdAt());
                assertNotNull(response.systems());
                assertEquals(1, response.systems().size());
                assertEquals(system.getPublicId(), response.systems().get(0).publicId());
                assertEquals(system.getName(), response.systems().get(0).name());
            }

            @Test
            @DisplayName("Should return null when input client is null")
            void shouldReturnNullWhenClientIsNull() {
                assertNull(clientMapper.toClientResponse(null));
            }
        }

        @Nested
        @DisplayName("Method: updateClient()")
        class UpdateClient {

            @Test
            @DisplayName("Should update Client with values from ClientRequest")
            void shouldUpdateClient() {
                UUID systemId = UUID.randomUUID();
                ClientRequest request = new ClientRequest("Updated Name", List.of(systemId));
                Systems system = new Systems();
                system.setPublicId(systemId);
                system.setName("System Updated");
                List<Systems> systemsList = List.of(system);

                Client client = new Client();
                client.setName("Old Name");
                client.setSystems(new ArrayList<>());

                clientMapper.updateClient(request, systemsList, client);

                assertEquals("Updated Name", client.getName());
                assertEquals(systemsList, client.getSystems());
            }

            @Test
            @DisplayName("Should not update fields if source is null")
            void shouldNotUpdateClientWithNulls() {
                ClientRequest request = new ClientRequest(null, null);
                Client client = new Client();
                client.setName("Old Name");
                List<Systems> oldSystems = Collections.emptyList();
                client.setSystems(oldSystems);

                clientMapper.updateClient(request, null, client);

                assertEquals("Old Name", client.getName());
                assertEquals(oldSystems, client.getSystems());
            }
        }
    }
}