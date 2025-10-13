package br.com.luizbrand.worklog.mapper;

import br.com.luizbrand.worklog.dto.request.ClientRequest;
import br.com.luizbrand.worklog.dto.response.ClientResponse;
import br.com.luizbrand.worklog.dto.response.SystemResponse;
import br.com.luizbrand.worklog.entity.Client;
import br.com.luizbrand.worklog.entity.Systems;
import br.com.luizbrand.worklog.repository.SystemRepository;
import br.com.luizbrand.worklog.service.SystemService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class ClientMapper {

    @Autowired
    protected SystemRepository systemRepository;

    @Mappings({
            @Mapping(target = "id", ignore = true),// Ignora o ID, será gerado pelo banco
            @Mapping(target = "publicId", ignore = true),// Ignora, será gerado pelo @PrePersist
            @Mapping(target = "createdAt", ignore = true), // Ignora, será gerado pelo @CreationTimestamp
            @Mapping(target = "updatedAt", ignore = true), // Ignora, será gerado pelo @UpdateTimestamp
            @Mapping(target = "enabled", constant = "true"), // Define um valor padrão "true" para novos clientes
            @Mapping(target = "systems", source = "systems")
    })
    public abstract Client toClient(ClientRequest clientRequest, List<Systems> systems);
    public abstract ClientResponse toClientResponse(Client client);


/*    @Named("publicIdsToSystems")
    protected List<Systems> publicIdsToSystems(List<UUID> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // Usamos o repositório para buscar todas as entidades pelo publicId
        return publicIds.stream()
                .map(pid -> systemRepository.findByPublicId(pid)
                        .orElseThrow(() -> new RuntimeException("System not found with publicId: " + pid)))
                .collect(Collectors.toList());
    }*/

    protected OffsetDateTime fromLocalDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

}
