package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.system.Systems;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ClientMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),// Ignora o ID, será gerado pelo banco
            @Mapping(target = "publicId", ignore = true),// Ignora, será gerado pelo @PrePersist
            @Mapping(target = "createdAt", ignore = true), // Ignora, será gerado pelo @CreationTimestamp
            @Mapping(target = "updatedAt", ignore = true), // Ignora, será gerado pelo @UpdateTimestamp
            @Mapping(target = "systems", source = "systems")
    })
    public abstract Client toClient(ClientRequest clientRequest, List<Systems> systems);
    public abstract ClientResponse toClientResponse(Client client);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateClient(ClientRequest clientRequest, List<Systems> systems, @MappingTarget Client client);

    protected OffsetDateTime fromLocalDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

}
