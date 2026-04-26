package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.tickets.dto.TicketFiltersParams;
import br.com.luizbrand.worklog.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TicketSpecification {

    public static Specification<Ticket> findByFilter(TicketFiltersParams filters) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filters.title() != null && !filters.title().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%" + filters.title().toLowerCase() + "%"));
            }

            if (filters.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filters.status()));
            }

            if (filters.clientId() != null) {
                Join<Ticket, Client> clientJoin = root.join("client");
                predicates.add(criteriaBuilder.equal(clientJoin.get("publicId"), filters.clientId()));
            }

            if (filters.systemId() != null) {
                Join<Ticket, Systems> systemJoin = root.join("system");
                predicates.add(criteriaBuilder.equal(systemJoin.get("publicId"), filters.systemId()));
            }

            if (filters.userId() != null) {
                Join<Ticket, User> userJoin = root.join("user");
                predicates.add(criteriaBuilder.equal(userJoin.get("publicId"), filters.userId()));
            }

            if (filters.createdFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), filters.createdFrom().atStartOfDay()));
            }

            if (filters.createdTo() != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("createdAt"), filters.createdTo().plusDays(1).atStartOfDay()));
            }

            if (filters.visibility() == StatusFiltro.ATIVO) {
                predicates.add(criteriaBuilder.equal(root.get("isEnabled"), true));
            } else if (filters.visibility() == StatusFiltro.INATIVO) {
                predicates.add(criteriaBuilder.equal(root.get("isEnabled"), false));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
