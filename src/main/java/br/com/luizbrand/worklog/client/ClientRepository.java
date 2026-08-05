package br.com.luizbrand.worklog.client;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * Segunda etapa da listagem paginada. O fetch join do {@link EntityGraph} não
     * pode ir junto com o {@code Pageable}: o Hibernate desiste do {@code limit}
     * no SQL e pagina em memória (HHH000104). Então a página vem sem as filiais e
     * este método as carrega de uma vez só para os ids daquela página, dentro da
     * mesma sessão.
     */
    @EntityGraph(attributePaths = "branches")
    List<Client> findDistinctByIdIn(Collection<Long> ids);

}
