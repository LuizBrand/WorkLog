package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
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
    private Path<String> attributePath;
    @Mock
    private Expression<String> loweredName;
    @Mock
    private Join<Client, Systems> systemsJoin;
    @Mock
    private Path<Object> systemsPublicIdPath;
    @Mock
    private Predicate predicate;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void wireCriteriaChain() {
        when(root.get(anyString())).thenReturn((Path) attributePath);
        when(root.join(anyString())).thenReturn((Join) systemsJoin);
        when(systemsJoin.get(anyString())).thenReturn((Path) systemsPublicIdPath);
        when(criteriaBuilder.lower(any())).thenReturn(loweredName);
        when(criteriaBuilder.like(any(), anyString())).thenReturn(predicate);
        when(criteriaBuilder.equal(any(), any())).thenReturn(predicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);
        when(systemsPublicIdPath.in(any(List.class))).thenReturn(predicate);
    }

    private void apply(ClientFiltersParams filters) {
        ClientSpecification.findByFilter(filters).toPredicate(root, query, criteriaBuilder);
    }

    @Test
    @DisplayName("Should add no predicates when all filters are null")
    void shouldAddNoPredicatesWhenAllFiltersNull() {
        apply(new ClientFiltersParams(null, null, null));

        verify(criteriaBuilder, never()).like(any(), anyString());
        verify(criteriaBuilder, never()).equal(any(), any());
        verify(root, never()).join(anyString());
    }

    @Test
    @DisplayName("Should skip the name predicate when name is an empty string")
    void shouldSkipNamePredicateWhenEmpty() {
        apply(new ClientFiltersParams("", null, null));

        verify(criteriaBuilder, never()).like(any(), anyString());
    }

    @Test
    @DisplayName("Should add a case-insensitive LIKE predicate when name is provided")
    void shouldAddCaseInsensitiveLikeWhenNameProvided() {
        apply(new ClientFiltersParams("Acme", null, null));

        verify(criteriaBuilder).lower(attributePath);
        verify(criteriaBuilder, times(1)).like(eq(loweredName), eq("%acme%"));
    }

    @Test
    @DisplayName("Should add equal(enabled, true) when status is ATIVO")
    void shouldFilterByEnabledTrueForAtivo() {
        apply(new ClientFiltersParams(null, StatusFiltro.ATIVO, null));

        verify(criteriaBuilder, times(1)).equal(attributePath, true);
    }

    @Test
    @DisplayName("Should add equal(enabled, false) when status is INATIVO")
    void shouldFilterByEnabledFalseForInativo() {
        apply(new ClientFiltersParams(null, StatusFiltro.INATIVO, null));

        verify(criteriaBuilder, times(1)).equal(attributePath, false);
    }

    @Test
    @DisplayName("Should add no enabled predicate when status is TODOS")
    void shouldAddNoEnabledPredicateForTodos() {
        apply(new ClientFiltersParams(null, StatusFiltro.TODOS, null));

        verify(criteriaBuilder, never()).equal(any(), any());
    }

    @Test
    @DisplayName("Should join systems and add an IN predicate when system ids are provided")
    void shouldJoinSystemsWhenIdsProvided() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());

        apply(new ClientFiltersParams(null, null, ids));

        verify(root, times(1)).join("systems");
        verify(systemsJoin).get("publicId");
        verify(systemsPublicIdPath, times(1)).in(ids);
    }

    @Test
    @DisplayName("Should not join systems when the id list is empty")
    void shouldSkipJoinWhenSystemsEmpty() {
        apply(new ClientFiltersParams(null, null, List.of()));

        verify(root, never()).join(anyString());
    }
}
