package br.com.luizbrand.worklog.mapper;

import br.com.luizbrand.worklog.dto.request.SystemRequest;
import br.com.luizbrand.worklog.dto.response.SystemResponse;
import br.com.luizbrand.worklog.entity.Systems;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class SystemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Systems toSystem(SystemRequest systemRequest);

    public abstract SystemResponse toSystemResponse(Systems system);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void updateSystem(SystemRequest systemRequest, @MappingTarget Systems system);
}
