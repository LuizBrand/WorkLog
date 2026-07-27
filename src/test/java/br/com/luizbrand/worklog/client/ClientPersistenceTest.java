package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.ContactType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class ClientPersistenceTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientContactRepository clientContactRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Client buildClientWithMatrizAndContacts() {
        Client client = Client.builder()
                .name("Empresa Cascata LTDA")
                .tipo(ClientType.PJ)
                .nomeFantasia("Cascata")
                .regimeTributario(RegimeTributario.SIMPLES_NACIONAL)
                .build();

        ClientBranch matriz = ClientBranch.builder()
                .client(client)
                .documento("12345678000199")
                .isMatriz(true)
                .apelido("Matriz")
                .inscricaoEstadual("ISENTO")
                .inscricaoMunicipal("123456")
                .address(Address.builder()
                        .cep("01001000")
                        .logradouro("Praca da Se")
                        .numero("1")
                        .complemento("Sala 2")
                        .bairro("Se")
                        .cidade("Sao Paulo")
                        .uf("SP")
                        .build())
                .build();

        ClientContact email = ClientContact.builder()
                .branch(matriz)
                .tipo(ContactType.EMAIL)
                .valor("financeiro@cascata.com.br")
                .descricao("financeiro")
                .principal(true)
                .build();

        ClientContact celular = ClientContact.builder()
                .branch(matriz)
                .tipo(ContactType.CELULAR)
                .valor("11999998888")
                .build();

        matriz.getContatos().addAll(List.of(email, celular));
        client.getBranches().add(matriz);

        return client;
    }

    @Test
    @DisplayName("Should cascade persist Client -> Branch -> Contacts saving only the client")
    void shouldCascadeFromClientToContacts() {
        Client client = buildClientWithMatrizAndContacts();

        clientRepository.saveAndFlush(client);
        entityManager.clear();

        Client reloaded = clientRepository.findByPublicId(client.getPublicId()).orElseThrow();

        assertThat(reloaded.getTipo()).isEqualTo(ClientType.PJ);
        assertThat(reloaded.getNomeFantasia()).isEqualTo("Cascata");
        assertThat(reloaded.getRegimeTributario()).isEqualTo(RegimeTributario.SIMPLES_NACIONAL);
        assertThat(reloaded.getBranches()).hasSize(1);

        ClientBranch matriz = reloaded.getBranches().get(0);
        assertThat(matriz.getId()).isNotNull();
        assertThat(matriz.getPublicId()).isNotNull();
        assertThat(matriz.getIsMatriz()).isTrue();
        assertThat(matriz.getInscricaoEstadual()).isEqualTo("ISENTO");
        assertThat(matriz.getContatos())
                .hasSize(2)
                .allSatisfy(contato -> {
                    assertThat(contato.getId()).isNotNull();
                    assertThat(contato.getPublicId()).isNotNull();
                    assertThat(contato.getBranch().getPublicId()).isEqualTo(matriz.getPublicId());
                });
        assertThat(matriz.getContatos())
                .extracting(ClientContact::getTipo)
                .containsExactlyInAnyOrder(ContactType.EMAIL, ContactType.CELULAR);
    }

    @Test
    @DisplayName("Should persist the embedded Address in the branch table")
    void shouldPersistEmbeddedAddress() {
        Client client = buildClientWithMatrizAndContacts();

        clientRepository.saveAndFlush(client);
        entityManager.clear();

        Client reloaded = clientRepository.findByPublicId(client.getPublicId()).orElseThrow();
        Address address = reloaded.getBranches().get(0).getAddress();

        assertThat(address).isNotNull();
        assertThat(address.getCep()).isEqualTo("01001000");
        assertThat(address.getLogradouro()).isEqualTo("Praca da Se");
        assertThat(address.getNumero()).isEqualTo("1");
        assertThat(address.getComplemento()).isEqualTo("Sala 2");
        assertThat(address.getBairro()).isEqualTo("Se");
        assertThat(address.getCidade()).isEqualTo("Sao Paulo");
        assertThat(address.getUf()).isEqualTo("SP");
    }

    @Test
    @DisplayName("Should store ContactType as String in the database")
    void shouldStoreContactTypeAsString() {
        Client client = buildClientWithMatrizAndContacts();

        clientRepository.saveAndFlush(client);
        entityManager.clear();

        Object tipo = entityManager.getEntityManager()
                .createNativeQuery("SELECT tipo FROM client_contacts WHERE valor = :valor")
                .setParameter("valor", "financeiro@cascata.com.br")
                .getSingleResult();

        assertThat(tipo).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("Should remove contacts from the database when detached from the branch (orphanRemoval)")
    void shouldRemoveOrphanContacts() {
        Client client = buildClientWithMatrizAndContacts();
        clientRepository.saveAndFlush(client);
        entityManager.clear();

        Client reloaded = clientRepository.findByPublicId(client.getPublicId()).orElseThrow();
        ClientBranch matriz = reloaded.getBranches().get(0);
        matriz.getContatos().remove(0);

        clientRepository.saveAndFlush(reloaded);
        entityManager.clear();

        assertThat(clientContactRepository.findAll()).hasSize(1);
    }

    private Client buildClientWithTwoBranches() {
        Client client = buildClientWithMatrizAndContacts();

        ClientBranch filial = ClientBranch.builder()
                .client(client)
                .documento("98765432000188")
                .isMatriz(false)
                .apelido("Filial Norte")
                .build();

        client.getBranches().add(filial);
        return client;
    }

    @Test
    @DisplayName("Should eagerly load branches on the filtered query (no N+1 on the listing)")
    void shouldFetchBranchesOnFilteredQuery() {
        clientRepository.saveAndFlush(buildClientWithTwoBranches());
        entityManager.clear();

        List<Client> found = clientRepository.findAll(
                ClientSpecification.findByFilter(new ClientFiltersParams(null, StatusFiltro.ATIVO, null, null, null)));

        assertThat(found).hasSize(1);
        assertThat(Hibernate.isInitialized(found.get(0).getBranches())).isTrue();
        assertThat(found.get(0).getBranches()).hasSize(2);
    }

    @Test
    @DisplayName("Should not duplicate the client when filtering by documento through the branch join")
    void shouldNotDuplicateClientWhenFilteringByDocumento() {
        clientRepository.saveAndFlush(buildClientWithTwoBranches());
        entityManager.clear();

        List<Client> found = clientRepository.findAll(
                ClientSpecification.findByFilter(
                        new ClientFiltersParams(null, null, null, "98.765.432/0001-88", null)));

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Empresa Cascata LTDA");
    }
}
