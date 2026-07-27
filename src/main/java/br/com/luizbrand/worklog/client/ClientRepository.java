package br.com.luizbrand.worklog.client;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    Optional<Client> findByPublicId(UUID publicId);
    Optional<Client> findByName(String name);

    /**
     * A listagem devolve as filiais no response; sem o fetch, cada cliente
     * dispararia uma query própria (N+1). O {@code distinct} necessário para o
     * join vem do {@link ClientSpecification}.
     * <p>
     * {@code branches.contatos} fica de fora de propósito: as duas coleções são
     * {@code List} (bags) e o Hibernate proíbe o fetch simultâneo de duas bags
     * ({@code MultipleBagFetchException}). Os contatos seguem lazy — débito
     * registrado.
     */
    @Override
    @EntityGraph(attributePaths = "branches")
    List<Client> findAll(Specification<Client> spec);

}
