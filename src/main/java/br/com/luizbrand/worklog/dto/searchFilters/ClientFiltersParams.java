package br.com.luizbrand.worklog.dto.searchFilters;

import br.com.luizbrand.worklog.dto.enums.StatusFiltro;

import java.util.List;
import java.util.UUID;

public record ClientFiltersParams(
        String name,
        StatusFiltro status,
        List<UUID> systems
) {
}
