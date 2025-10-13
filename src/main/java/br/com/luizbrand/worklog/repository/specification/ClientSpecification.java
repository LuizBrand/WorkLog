package br.com.luizbrand.worklog.repository.specification;

import br.com.luizbrand.worklog.dto.searchFilters.ClientFiltersParams;
import br.com.luizbrand.worklog.entity.Client;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ClientSpecification {

    public static Specification<Client> findByFilter(ClientFiltersParams filters) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filters.name() != null && !filters.name().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filters.name().toLowerCase() + "%"));

            }

            if (filters.status() != null){
                switch (filters.status()) {
                    case ATIVO:
                        predicates.add(criteriaBuilder.equal(root.get("enabled"), true));
                        break;
                    case INATIVO:
                        predicates.add(criteriaBuilder.equal(root.get("enabled"), false));
                        break;
                    case TODOS:
                        break;
                }
            }

            if (filters.systems() != null && !filters.systems().isEmpty()) {
                Join<Client, System> joinSystem = root.join("systems");
                predicates.add(joinSystem.get("publicId").in(filters.systems()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

    }

}
