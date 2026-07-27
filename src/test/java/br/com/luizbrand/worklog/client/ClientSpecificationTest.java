package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.system.Systems;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientSpecificationTest {

    @Mock
    private Root<Client> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder criteriaBuilder;
    @Mock
    private Path<String> namePath;
    @Mock
    private Path<Boolean> enabledPath;
    @Mock
    private Path<ClientType> tipoPath;
    @Mock
    private Expression<String> loweredName;
    @Mock
    private Join<Client, Systems> systemsJoin;
    @Mock
    private Join<Client, ClientBranch> branchesJoin;
    @Mock
    private Path<Object> systemsPublicIdPath;
    @Mock
    private Path<String> documentoPath;
    @Mock
    private Predicate predicate;

    /**
     * Cada atributo tem seu próprio {@code Path} mockado de propósito: com um mock
     * único para qualquer nome, um erro no nome do atributo passava despercebido
     * no teste e só estourava em runtime.
     */
    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void wireCriteriaChain() {
        when(root.get("name")).thenReturn((Path) namePath);
        when(root.get("isEnabled")).thenReturn((Path) enabledPath);
        when(root.get("tipo")).thenReturn((Path) tipoPath);
        when(root.join("systems")).thenReturn((Join) systemsJoin);
        when(root.join("branches")).thenReturn((Join) branchesJoin);
        when(systemsJoin.get("publicId")).thenReturn((Path) systemsPublicIdPath);
        when(branchesJoin.get("documento")).thenReturn((Path) documentoPath);
        when(criteriaBuilder.lower(any())).thenReturn(loweredName);
        when(criteriaBuilder.like(any(), anyString())).thenReturn(predicate);
        when(criteriaBuilder.equal(any(), any())).thenReturn(predicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);
        when(systemsPublicIdPath.in(any(List.class))).thenReturn(predicate);
    }

    private void apply(ClientFiltersParams filters) {
        ClientSpecification.findByFilter(filters).toPredicate(root, query, criteriaBuilder);
    }

    private static ClientFiltersParams filters(String name, StatusFiltro status, List<UUID> systems) {
        return new ClientFiltersParams(name, status, systems, null, null);
    }

    private static ClientFiltersParams byDocumento(String documento) {
        return new ClientFiltersParams(null, null, null, documento, null);
    }

    private static ClientFiltersParams byTipo(ClientType tipo) {
        return new ClientFiltersParams(null, null, null, null, tipo);
    }

    @Test
    @DisplayName("Should add no predicates when all filters are null")
    void shouldAddNoPredicatesWhenAllFiltersNull() {
        apply(filters(null, null, null));

        verify(criteriaBuilder, never()).like(any(), anyString());
        verify(criteriaBuilder, never()).equal(any(), any());
        verify(root, never()).join(anyString());
    }

    @Test
    @DisplayName("Should skip the name predicate when name is an empty string")
    void shouldSkipNamePredicateWhenEmpty() {
        apply(filters("", null, null));

        verify(criteriaBuilder, never()).like(any(), anyString());
    }

    @Test
    @DisplayName("Should add a case-insensitive LIKE predicate when name is provided")
    void shouldAddCaseInsensitiveLikeWhenNameProvided() {
        apply(filters("Acme", null, null));

        verify(criteriaBuilder).lower(namePath);
        verify(criteriaBuilder, times(1)).like(eq(loweredName), eq("%acme%"));
    }

    @Test
    @DisplayName("Should add equal(isEnabled, true) when status is ATIVO")
    void shouldFilterByEnabledTrueForAtivo() {
        apply(filters(null, StatusFiltro.ATIVO, null));

        verify(root).get("isEnabled");
        verify(criteriaBuilder, times(1)).equal(enabledPath, true);
    }

    @Test
    @DisplayName("Should add equal(isEnabled, false) when status is INATIVO")
    void shouldFilterByEnabledFalseForInativo() {
        apply(filters(null, StatusFiltro.INATIVO, null));

        verify(root).get("isEnabled");
        verify(criteriaBuilder, times(1)).equal(enabledPath, false);
    }

    @Test
    @DisplayName("Should add no enabled predicate when status is TODOS")
    void shouldAddNoEnabledPredicateForTodos() {
        apply(filters(null, StatusFiltro.TODOS, null));

        verify(criteriaBuilder, never()).equal(any(), any());
    }

    @Test
    @DisplayName("Should join systems and add an IN predicate when system ids are provided")
    void shouldJoinSystemsWhenIdsProvided() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());

        apply(filters(null, null, ids));

        verify(root, times(1)).join("systems");
        verify(systemsJoin).get("publicId");
        verify(systemsPublicIdPath, times(1)).in(ids);
    }

    @Test
    @DisplayName("Should not join systems when the id list is empty")
    void shouldSkipJoinWhenSystemsEmpty() {
        apply(filters(null, null, List.of()));

        verify(root, never()).join(anyString());
    }

    @Test
    @DisplayName("Should join branches and compare the documento when documento is provided")
    void shouldJoinBranchesWhenDocumentoProvided() {
        apply(byDocumento("12345678000195"));

        verify(root, times(1)).join("branches");
        verify(branchesJoin).get("documento");
        verify(criteriaBuilder, times(1)).equal(documentoPath, "12345678000195");
    }

    @Test
    @DisplayName("Should normalize a masked documento before comparing")
    void shouldNormalizeMaskedDocumento() {
        apply(byDocumento("12.345.678/0001-95"));

        verify(criteriaBuilder, times(1)).equal(documentoPath, "12345678000195");
    }

    @Test
    @DisplayName("Should skip the documento filter when it is blank")
    void shouldSkipDocumentoWhenBlank() {
        apply(byDocumento("   "));

        verify(root, never()).join(anyString());
        verify(criteriaBuilder, never()).equal(any(), any());
    }

    @Test
    @DisplayName("Should add equal(tipo) without any join when tipo is provided")
    void shouldFilterByTipoWithoutJoin() {
        apply(byTipo(ClientType.PF));

        verify(root).get("tipo");
        verify(criteriaBuilder, times(1)).equal(tipoPath, ClientType.PF);
        verify(root, never()).join(anyString());
    }

    @Test
    @DisplayName("Should mark the query as distinct when joining systems")
    void shouldMarkDistinctWhenJoiningSystems() {
        apply(filters(null, null, List.of(UUID.randomUUID())));

        verify(query, times(1)).distinct(true);
    }

    @Test
    @DisplayName("Should mark the query as distinct when joining branches")
    void shouldMarkDistinctWhenJoiningBranches() {
        apply(byDocumento("11144477735"));

        verify(query, times(1)).distinct(true);
    }

    @Test
    @DisplayName("Should not touch distinct when no to-many join is added")
    void shouldNotMarkDistinctWithoutJoin() {
        apply(filters("Acme", StatusFiltro.ATIVO, null));

        verify(query, never()).distinct(anyBoolean());
    }
}
