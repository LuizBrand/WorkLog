package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.enums.StatusFiltro;

import java.util.List;
import java.util.UUID;

public record ClientFiltersParams(
        String name,
        StatusFiltro status,
        List<UUID> systems
) {
}
