package br.com.luizbrand.worklog.mapper;

import br.com.luizbrand.worklog.dto.request.ClientRequest;
import br.com.luizbrand.worklog.dto.response.ClientResponse;
import br.com.luizbrand.worklog.entity.Client;
import br.com.luizbrand.worklog.entity.Systems;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
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
            assertTrue(client.isEnabled());
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
            client.setEnabled(true);
            client.setCreatedAt(dateTime);
            client.setSystems(List.of(system));

            ClientResponse response = clientMapper.toClientResponse(client);

            assertNotNull(response);
            assertEquals(client.getPublicId(), response.publicId());
            assertEquals(client.getName(), response.name());
            assertEquals(client.isEnabled(), response.enabled());
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
