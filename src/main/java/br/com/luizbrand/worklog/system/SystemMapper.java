package br.com.luizbrand.worklog.system;

import br.com.luizbrand.worklog.system.dto.SystemRequest;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class SystemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Systems toSystem(SystemRequest systemRequest);

    public abstract SystemResponse toSystemResponse(Systems system);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void updateSystem(SystemRequest systemRequest, @MappingTarget Systems system);
}
