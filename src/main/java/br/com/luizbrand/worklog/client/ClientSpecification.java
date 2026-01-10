package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.system.Systems;
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
                Join<Client, Systems> joinSystem = root.join("systems");
                predicates.add(joinSystem.get("publicId").in(filters.systems()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

    }

}
