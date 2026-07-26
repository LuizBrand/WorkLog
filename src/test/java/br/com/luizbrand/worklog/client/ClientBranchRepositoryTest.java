package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.enums.ClientType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataJpaTest
class ClientBranchRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientBranchRepository clientBranchRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Client client;

    @BeforeEach
    void setUp() {
        client = clientRepository.saveAndFlush(
                Client.builder()
                        .name("Empresa Teste LTDA")
                        .tipo(ClientType.PJ)
                        .build()
        );
    }

    private ClientBranch persistBranch(String documento, boolean isMatriz) {
        ClientBranch branch = ClientBranch.builder()
                .client(client)
                .documento(documento)
                .isMatriz(isMatriz)
                .apelido(isMatriz ? "Matriz" : "Filial")
                .build();

        return clientBranchRepository.saveAndFlush(branch);
    }

    @Nested
    @DisplayName("Persistência de filial vinculada ao cliente")
    class PersistBranch {

        @Test
        @DisplayName("Should persist a client with its matriz branch and keep the link")
        void shouldPersistClientWithMatriz() {
            ClientBranch matriz = ClientBranch.builder()
                    .client(client)
                    .documento("12345678000199")
                    .isMatriz(true)
                    .apelido("Matriz")
                    .build();
            client.getBranches().add(matriz);

            // save() em cliente ja gerenciado passa por merge(): o publicId gerado
            // fica na copia retornada, nao na instancia local
            Client saved = clientRepository.saveAndFlush(client);
            UUID matrizPublicId = saved.getBranches().get(0).getPublicId();
            entityManager.clear();

            ClientBranch found = clientBranchRepository.findByPublicId(matrizPublicId).orElseThrow();

            assertThat(found.getId()).isNotNull();
            assertThat(found.getPublicId()).isNotNull();
            assertThat(found.getIsMatriz()).isTrue();
            assertThat(found.getApelido()).isEqualTo("Matriz");
            assertThat(found.getDocumento()).isEqualTo("12345678000199");
            assertThat(found.getIsEnabled()).isTrue();
            assertThat(found.getClient().getPublicId()).isEqualTo(client.getPublicId());
        }

        @Test
        @DisplayName("Should default isMatriz to false when not informed")
        void shouldDefaultIsMatrizToFalse() {
            ClientBranch branch = ClientBranch.builder()
                    .client(client)
                    .documento("98765432000155")
                    .build();

            ClientBranch saved = clientBranchRepository.saveAndFlush(branch);
            entityManager.clear();

            ClientBranch found = clientBranchRepository.findByPublicId(saved.getPublicId()).orElseThrow();

            assertThat(found.getIsMatriz()).isFalse();
        }

        @Test
        @DisplayName("Should return empty when publicId does not exist")
        void shouldReturnEmptyWhenPublicIdNotFound() {
            Optional<ClientBranch> found = clientBranchRepository.findByPublicId(UUID.randomUUID());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Method: existsByDocumento()")
    class ExistsByDocumento {

        @Test
        @DisplayName("Should return true when documento is already persisted")
        void shouldReturnTrueWhenDocumentoExists() {
            persistBranch("12345678000199", true);

            assertThat(clientBranchRepository.existsByDocumento("12345678000199")).isTrue();
        }

        @Test
        @DisplayName("Should return false when documento does not exist")
        void shouldReturnFalseWhenDocumentoDoesNotExist() {
            persistBranch("12345678000199", true);

            assertThat(clientBranchRepository.existsByDocumento("00000000000000")).isFalse();
        }
    }

    @Nested
    @DisplayName("Method: findByDocumento()")
    class FindByDocumento {

        @Test
        @DisplayName("Should return the branch that owns the documento")
        void shouldReturnBranchByDocumento() {
            persistBranch("12345678000199", true);
            ClientBranch filial = persistBranch("12345678000280", false);
            entityManager.clear();

            ClientBranch found = clientBranchRepository.findByDocumento("12345678000280").orElseThrow();

            assertThat(found.getPublicId()).isEqualTo(filial.getPublicId());
            assertThat(found.getIsMatriz()).isFalse();
            assertThat(found.getClient().getPublicId()).isEqualTo(client.getPublicId());
        }

        @Test
        @DisplayName("Should return empty when no branch owns the documento")
        void shouldReturnEmptyWhenDocumentoNotFound() {
            persistBranch("12345678000199", true);

            assertThat(clientBranchRepository.findByDocumento("00000000000000")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Unicidade do documento")
    class DocumentoUniqueness {

        @Test
        @DisplayName("Should reject two branches sharing the same documento")
        void shouldRejectDuplicatedDocumento() {
            persistBranch("12345678000199", true);

            ClientBranch duplicada = ClientBranch.builder()
                    .client(client)
                    .documento("12345678000199")
                    .apelido("Filial duplicada")
                    .build();

            assertThatThrownBy(() -> clientBranchRepository.saveAndFlush(duplicada))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should allow multiple branches with null documento (legacy backfill)")
        void shouldAllowMultipleNullDocumentos() {
            assertThatCode(() -> {
                persistBranch(null, true);
                persistBranch(null, false);
            }).doesNotThrowAnyException();

            entityManager.clear();

            assertThat(clientBranchRepository.findAll())
                    .hasSize(2)
                    .allSatisfy(branch -> assertThat(branch.getDocumento()).isNull());
        }
    }
}
