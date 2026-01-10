package br.com.luizbrand.worklog.service;

import br.com.luizbrand.worklog.dto.request.ClientRequest;
import br.com.luizbrand.worklog.dto.response.ClientResponse;
import br.com.luizbrand.worklog.entity.Client;
import br.com.luizbrand.worklog.entity.Systems;
import br.com.luizbrand.worklog.exception.Conflict.ClientAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
import br.com.luizbrand.worklog.mapper.ClientMapper;
import br.com.luizbrand.worklog.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
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
        client.setEnabled(true);

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
}