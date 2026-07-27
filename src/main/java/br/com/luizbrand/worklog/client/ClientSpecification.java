package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
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
            boolean hasToManyJoin = false;

            if (filters.name() != null && !filters.name().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filters.name().toLowerCase() + "%"));

            }

            if (filters.status() != null){
                switch (filters.status()) {
                    case ATIVO:
                        predicates.add(criteriaBuilder.equal(root.get("isEnabled"), true));
                        break;
                    case INATIVO:
                        predicates.add(criteriaBuilder.equal(root.get("isEnabled"), false));
                        break;
                    case TODOS:
                        break;
                }
            }

            if (filters.tipo() != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipo"), filters.tipo()));
            }

            if (filters.systems() != null && !filters.systems().isEmpty()) {
                Join<Client, Systems> joinSystem = root.join("systems");
                predicates.add(joinSystem.get("publicId").in(filters.systems()));
                hasToManyJoin = true;
            }

            // O parâmetro chega cru da query string; a normalização é a mesma do
            // BranchRequest para casar com o formato gravado no banco.
            String documento = BranchRequest.normalizeDocumento(filters.documento());
            if (documento != null) {
                Join<Client, ClientBranch> joinBranch = root.join("branches");
                predicates.add(criteriaBuilder.equal(joinBranch.get("documento"), documento));
                hasToManyJoin = true;
            }

            // Sem distinct, um join para-muitos duplica o cliente na listagem.
            if (hasToManyJoin && query != null) {
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

    }

}
