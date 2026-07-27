package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.AddressRequest;
import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.client.dto.ClientUpdateRequest;
import br.com.luizbrand.worklog.client.dto.ContactRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.ContactType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;
import br.com.luizbrand.worklog.support.ClientRequestTestBuilder;
import br.com.luizbrand.worklog.system.Systems;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.aClientRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientMapper")
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
            ClientRequest request = aClientRequest()
                    .withName("New Client")
                    .withSystem(systemId)
                    .build();
            Systems system = new Systems();
            system.setPublicId(systemId);
            system.setName("System 1");
            List<Systems> systemsList = List.of(system);

            Client client = clientMapper.toClient(request, systemsList);

            assertThat(client).isNotNull();
            assertThat(client.getName()).isEqualTo("New Client");
            assertThat(client.getIsEnabled()).isTrue();
            assertThat(client.getSystems()).isEqualTo(systemsList);
            assertThat(client.getId()).isNull();
            assertThat(client.getPublicId()).isNull();
            assertThat(client.getCreatedAt()).isNull();
            assertThat(client.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("Should map the whole tree: type, trade name, tax regime, branch, address and contacts")
        void shouldMapNestedTree() {
            ContactRequest email = new ContactRequest(ContactType.EMAIL, "financeiro@acme.com", "Financeiro", true);
            ContactRequest celular = new ContactRequest(ContactType.CELULAR, "11999998888", null, false);
            AddressRequest address = ClientRequestTestBuilder.anAddress();
            BranchRequest matriz = new BranchRequest(CNPJ_VALIDO, true, "Matriz", "IE123", "IM456",
                    address, List.of(email, celular));

            ClientRequest request = aClientRequest()
                    .withTipo(ClientType.PJ)
                    .withName("Acme Ltda")
                    .withNomeFantasia("Acme")
                    .withRegimeTributario(RegimeTributario.SIMPLES_NACIONAL)
                    .withBranches(matriz)
                    .build();

            Client client = clientMapper.toClient(request, List.of());

            assertThat(client.getTipo()).isEqualTo(ClientType.PJ);
            assertThat(client.getNomeFantasia()).isEqualTo("Acme");
            assertThat(client.getRegimeTributario()).isEqualTo(RegimeTributario.SIMPLES_NACIONAL);
            assertThat(client.getBranches()).hasSize(1);

            ClientBranch branch = client.getBranches().get(0);
            assertThat(branch.getDocumento()).isEqualTo(CNPJ_VALIDO);
            assertThat(branch.getIsMatriz()).isTrue();
            assertThat(branch.getApelido()).isEqualTo("Matriz");
            assertThat(branch.getInscricaoEstadual()).isEqualTo("IE123");
            assertThat(branch.getInscricaoMunicipal()).isEqualTo("IM456");
            assertThat(branch.getPublicId()).isNull();
            assertThat(branch.getAddress()).isNotNull();
            assertThat(branch.getAddress().getCep()).isEqualTo("01310100");
            assertThat(branch.getAddress().getLogradouro()).isEqualTo("Av. Paulista");
            assertThat(branch.getAddress().getUf()).isEqualTo("SP");

            assertThat(branch.getContatos()).hasSize(2);
            ClientContact firstContact = branch.getContatos().get(0);
            assertThat(firstContact.getTipo()).isEqualTo(ContactType.EMAIL);
            assertThat(firstContact.getValor()).isEqualTo("financeiro@acme.com");
            assertThat(firstContact.getDescricao()).isEqualTo("Financeiro");
            assertThat(firstContact.getPrincipal()).isTrue();
            assertThat(firstContact.getPublicId()).isNull();
        }

        @Test
        @DisplayName("Should wire back references so the cascade insert works")
        void shouldWireBackReferences() {
            BranchRequest matriz = new BranchRequest(CNPJ_VALIDO, true, null, null, null, null,
                    List.of(new ContactRequest(ContactType.EMAIL, "a@b.com", null, null)));
            ClientRequest request = aClientRequest().withBranches(matriz).build();

            Client client = clientMapper.toClient(request, List.of());

            ClientBranch branch = client.getBranches().get(0);
            assertThat(branch.getClient()).isSameAs(client);
            assertThat(branch.getContatos().get(0).getBranch()).isSameAs(branch);
        }

        @Test
        @DisplayName("Should default isMatriz and principal to false when absent in the request")
        void shouldDefaultFlagsToFalse() {
            BranchRequest filial = new BranchRequest(CNPJ_VALIDO, null, null, null, null, null,
                    List.of(new ContactRequest(ContactType.EMAIL, "a@b.com", null, null)));
            ClientRequest request = aClientRequest().withBranches(filial).build();

            Client client = clientMapper.toClient(request, List.of());

            ClientBranch branch = client.getBranches().get(0);
            assertThat(branch.getIsMatriz()).isFalse();
            assertThat(branch.getContatos().get(0).getPrincipal()).isFalse();
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

            assertThat(response).isNotNull();
            assertThat(response.publicId()).isEqualTo(clientPublicId);
            assertThat(response.name()).isEqualTo("Client A");
            assertThat(response.enabled()).isTrue();
            assertThat(response.createdAt()).isEqualTo(dateTime.atOffset(ZoneOffset.UTC));
            assertThat(response.systems()).hasSize(1);
            assertThat(response.systems().get(0).publicId()).isEqualTo(systemPublicId);
            assertThat(response.systems().get(0).name()).isEqualTo("System A");
        }

        @Test
        @DisplayName("Should map the nested branch, address and contacts")
        void shouldMapNestedTree() {
            UUID contactPublicId = UUID.randomUUID();
            UUID branchPublicId = UUID.randomUUID();

            ClientContact contact = ClientContact.builder()
                    .publicId(contactPublicId)
                    .tipo(ContactType.EMAIL)
                    .valor("financeiro@acme.com")
                    .descricao("Financeiro")
                    .principal(true)
                    .build();

            ClientBranch branch = ClientBranch.builder()
                    .publicId(branchPublicId)
                    .documento(CNPJ_VALIDO)
                    .isMatriz(true)
                    .apelido("Matriz")
                    .inscricaoEstadual("IE123")
                    .inscricaoMunicipal("IM456")
                    .address(Address.builder()
                            .cep("01310100")
                            .logradouro("Av. Paulista")
                            .numero("1000")
                            .complemento("Sala 1")
                            .bairro("Bela Vista")
                            .cidade("São Paulo")
                            .uf("SP")
                            .build())
                    .contatos(new ArrayList<>(List.of(contact)))
                    .build();

            Client client = Client.builder()
                    .publicId(UUID.randomUUID())
                    .name("Acme Ltda")
                    .tipo(ClientType.PJ)
                    .nomeFantasia("Acme")
                    .regimeTributario(RegimeTributario.LUCRO_PRESUMIDO)
                    .createdAt(dateTime)
                    .systems(List.of())
                    .branches(new ArrayList<>(List.of(branch)))
                    .build();

            ClientResponse response = clientMapper.toClientResponse(client);

            assertThat(response.tipo()).isEqualTo(ClientType.PJ);
            assertThat(response.nomeFantasia()).isEqualTo("Acme");
            assertThat(response.regimeTributario()).isEqualTo(RegimeTributario.LUCRO_PRESUMIDO);
            assertThat(response.branches()).hasSize(1);

            BranchResponse branchResponse = response.branches().get(0);
            assertThat(branchResponse.publicId()).isEqualTo(branchPublicId);
            assertThat(branchResponse.documento()).isEqualTo(CNPJ_VALIDO);
            assertThat(branchResponse.isMatriz()).isTrue();
            assertThat(branchResponse.apelido()).isEqualTo("Matriz");
            assertThat(branchResponse.inscricaoEstadual()).isEqualTo("IE123");
            assertThat(branchResponse.inscricaoMunicipal()).isEqualTo("IM456");
            assertThat(branchResponse.enabled()).isTrue();
            assertThat(branchResponse.address()).isNotNull();
            assertThat(branchResponse.address().cep()).isEqualTo("01310100");
            assertThat(branchResponse.address().cidade()).isEqualTo("São Paulo");
            assertThat(branchResponse.contatos()).hasSize(1);
            assertThat(branchResponse.contatos().get(0).publicId()).isEqualTo(contactPublicId);
            assertThat(branchResponse.contatos().get(0).tipo()).isEqualTo(ContactType.EMAIL);
            assertThat(branchResponse.contatos().get(0).valor()).isEqualTo("financeiro@acme.com");
            assertThat(branchResponse.contatos().get(0).principal()).isTrue();
        }

        @Test
        @DisplayName("Should expose an empty branch list instead of null when the client has none")
        void shouldReturnEmptyBranchesWhenClientHasNone() {
            Client client = new Client();
            client.setPublicId(UUID.randomUUID());
            client.setName("No branches");
            client.setBranches(null);

            ClientResponse response = clientMapper.toClientResponse(client);

            assertThat(response.branches()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return null when input client is null")
        void shouldReturnNullWhenClientIsNull() {
            assertThat(clientMapper.toClientResponse(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Method: updateClient()")
    class UpdateClient {

        @Test
        @DisplayName("Should update Client with values from ClientUpdateRequest")
        void shouldUpdateClient() {
            UUID systemId = UUID.randomUUID();
            ClientUpdateRequest request = new ClientUpdateRequest(ClientType.PF, "Updated Name",
                    "Fantasia Nova", RegimeTributario.MEI, List.of(systemId), null);
            Systems system = new Systems();
            system.setPublicId(systemId);
            system.setName("System Updated");
            List<Systems> systemsList = List.of(system);

            Client client = new Client();
            client.setName("Old Name");
            client.setSystems(new ArrayList<>());

            clientMapper.updateClient(request, systemsList, client);

            assertThat(client.getName()).isEqualTo("Updated Name");
            assertThat(client.getNomeFantasia()).isEqualTo("Fantasia Nova");
            assertThat(client.getTipo()).isEqualTo(ClientType.PF);
            assertThat(client.getRegimeTributario()).isEqualTo(RegimeTributario.MEI);
            assertThat(client.getSystems()).isEqualTo(systemsList);
        }

        @Test
        @DisplayName("Should not update fields if source is null")
        void shouldNotUpdateClientWithNulls() {
            ClientUpdateRequest request = new ClientUpdateRequest(null, null, null, null, null, null);
            Client client = new Client();
            client.setName("Old Name");
            client.setTipo(ClientType.PJ);
            client.setNomeFantasia("Old Fantasia");
            client.setRegimeTributario(RegimeTributario.LUCRO_REAL);
            List<Systems> oldSystems = Collections.emptyList();
            client.setSystems(oldSystems);
            client.setIsEnabled(true);

            clientMapper.updateClient(request, null, client);

            assertThat(client.getName()).isEqualTo("Old Name");
            assertThat(client.getTipo()).isEqualTo(ClientType.PJ);
            assertThat(client.getNomeFantasia()).isEqualTo("Old Fantasia");
            assertThat(client.getRegimeTributario()).isEqualTo(RegimeTributario.LUCRO_REAL);
            assertThat(client.getSystems()).isEqualTo(oldSystems);
            assertThat(client.getIsEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should not touch the existing branches")
        void shouldKeepBranchesUntouched() {
            ClientBranch branch = ClientBranch.builder().documento(CNPJ_VALIDO).isMatriz(true).build();
            Client client = new Client();
            client.setName("Old Name");
            client.setSystems(new ArrayList<>());
            client.setBranches(new ArrayList<>(List.of(branch)));

            clientMapper.updateClient(new ClientUpdateRequest(null, "New Name", null, null, null, null),
                    null, client);

            assertThat(client.getBranches()).containsExactly(branch);
        }

        @Test
        @DisplayName("Should flip isEnabled to false when request.enabled is false")
        void shouldFlipIsEnabledWhenEnabledFalse() {
            ClientUpdateRequest request = new ClientUpdateRequest(null, "Same Name", null, null, List.of(), false);
            Client client = new Client();
            client.setName("Same Name");
            client.setIsEnabled(true);
            client.setSystems(new ArrayList<>());

            clientMapper.updateClient(request, List.of(), client);

            assertThat(client.getIsEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should re-enable isEnabled when request.enabled is true")
        void shouldEnableIsEnabledWhenEnabledTrue() {
            ClientUpdateRequest request = new ClientUpdateRequest(null, "Same Name", null, null, List.of(), true);
            Client client = new Client();
            client.setName("Same Name");
            client.setIsEnabled(false);
            client.setSystems(new ArrayList<>());

            clientMapper.updateClient(request, List.of(), client);

            assertThat(client.getIsEnabled()).isTrue();
        }
    }
}
