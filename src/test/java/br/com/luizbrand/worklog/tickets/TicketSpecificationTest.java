package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.tickets.dto.TicketFiltersParams;
import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import br.com.luizbrand.worklog.user.User;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
class TicketSpecificationTest {

    @Mock
    private Root<Ticket> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder criteriaBuilder;
    @Mock
    private Path<String> attributePath;
    @Mock
    private Expression<String> loweredTitle;
    @Mock
    private Join<Ticket, Client> clientJoin;
    @Mock
    private Join<Ticket, Systems> systemJoin;
    @Mock
    private Join<Ticket, User> userJoin;
    @Mock
    private Path<Object> relationPublicIdPath;
    @Mock
    private Predicate predicate;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void wireCriteriaChain() {
        when(root.get(anyString())).thenReturn((Path) attributePath);
        when(root.join("client")).thenReturn((Join) clientJoin);
        when(root.join("system")).thenReturn((Join) systemJoin);
        when(root.join("user")).thenReturn((Join) userJoin);
        when(clientJoin.get(anyString())).thenReturn((Path) relationPublicIdPath);
        when(systemJoin.get(anyString())).thenReturn((Path) relationPublicIdPath);
        when(userJoin.get(anyString())).thenReturn((Path) relationPublicIdPath);
        when(criteriaBuilder.lower(any())).thenReturn(loweredTitle);
        when(criteriaBuilder.like(any(), anyString())).thenReturn(predicate);
        when(criteriaBuilder.equal(any(), any())).thenReturn(predicate);
        when(criteriaBuilder.greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
        when(criteriaBuilder.lessThan(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);
    }

    private void apply(TicketFiltersParams filters) {
        TicketSpecification.findByFilter(filters).toPredicate(root, query, criteriaBuilder);
    }

    @Test
    @DisplayName("Should add no predicates when all filters are null")
    void shouldAddNoPredicatesWhenAllFiltersNull() {
        apply(new TicketFiltersParams(null, null, null, null, null, null, null));

        verify(criteriaBuilder, never()).like(any(), anyString());
        verify(criteriaBuilder, never()).equal(any(), any());
        verify(criteriaBuilder, never()).greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class));
        verify(criteriaBuilder, never()).lessThan(any(Expression.class), any(LocalDateTime.class));
        verify(root, never()).join(anyString());
    }

    @Test
    @DisplayName("Should skip the title predicate when title is an empty string")
    void shouldSkipTitlePredicateWhenEmpty() {
        apply(new TicketFiltersParams("", null, null, null, null, null, null));

        verify(criteriaBuilder, never()).like(any(), anyString());
    }

    @Test
    @DisplayName("Should add a case-insensitive LIKE predicate when title is provided")
    void shouldAddCaseInsensitiveLikeWhenTitleProvided() {
        apply(new TicketFiltersParams("Login", null, null, null, null, null, null));

        verify(criteriaBuilder).lower(attributePath);
        verify(criteriaBuilder, times(1)).like(eq(loweredTitle), eq("%login%"));
    }

    @Test
    @DisplayName("Should add equal(status, value) when status is provided")
    void shouldFilterByStatus() {
        apply(new TicketFiltersParams(null, TicketStatus.PENDING, null, null, null, null, null));

        verify(root).get("status");
        verify(criteriaBuilder, times(1)).equal(attributePath, TicketStatus.PENDING);
    }

    @Test
    @DisplayName("Should join client and filter by publicId when clientId is provided")
    void shouldFilterByClientId() {
        UUID clientId = UUID.randomUUID();

        apply(new TicketFiltersParams(null, null, clientId, null, null, null, null));

        verify(root, times(1)).join("client");
        verify(clientJoin).get("publicId");
        verify(criteriaBuilder, times(1)).equal(relationPublicIdPath, clientId);
    }

    @Test
    @DisplayName("Should join system and filter by publicId when systemId is provided")
    void shouldFilterBySystemId() {
        UUID systemId = UUID.randomUUID();

        apply(new TicketFiltersParams(null, null, null, systemId, null, null, null));

        verify(root, times(1)).join("system");
        verify(systemJoin).get("publicId");
        verify(criteriaBuilder, times(1)).equal(relationPublicIdPath, systemId);
    }

    @Test
    @DisplayName("Should join user and filter by publicId when userId is provided")
    void shouldFilterByUserId() {
        UUID userId = UUID.randomUUID();

        apply(new TicketFiltersParams(null, null, null, null, userId, null, null));

        verify(root, times(1)).join("user");
        verify(userJoin).get("publicId");
        verify(criteriaBuilder, times(1)).equal(relationPublicIdPath, userId);
    }

    @Test
    @DisplayName("Should add a greaterThanOrEqualTo predicate on createdAt when only createdFrom is provided")
    void shouldFilterByCreatedFromOnly() {
        LocalDate from = LocalDate.of(2026, 4, 1);

        apply(new TicketFiltersParams(null, null, null, null, null, from, null));

        verify(root).get("createdAt");
        verify(criteriaBuilder, times(1))
                .greaterThanOrEqualTo(any(Expression.class), eq(from.atStartOfDay()));
        verify(criteriaBuilder, never()).lessThan(any(Expression.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should add a lessThan predicate on createdAt pinned to the start of the next day when only createdTo is provided")
    void shouldFilterByCreatedToOnly() {
        LocalDate to = LocalDate.of(2026, 4, 30);

        apply(new TicketFiltersParams(null, null, null, null, null, null, to));

        verify(root).get("createdAt");
        verify(criteriaBuilder, times(1))
                .lessThan(any(Expression.class), eq(to.plusDays(1).atStartOfDay()));
        verify(criteriaBuilder, never())
                .greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should add both range predicates when createdFrom and createdTo are provided")
    void shouldFilterByCreatedRange() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);

        apply(new TicketFiltersParams(null, null, null, null, null, from, to));

        verify(criteriaBuilder, times(1))
                .greaterThanOrEqualTo(any(Expression.class), eq(from.atStartOfDay()));
        verify(criteriaBuilder, times(1))
                .lessThan(any(Expression.class), eq(to.plusDays(1).atStartOfDay()));
    }
}
