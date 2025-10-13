package br.com.luizbrand.worklog.mapper;

import br.com.luizbrand.worklog.dto.request.SystemRequest;
import br.com.luizbrand.worklog.dto.response.SystemResponse;
import br.com.luizbrand.worklog.entity.Systems;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SystemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Systems toSystem(SystemRequest systemRequest);

    SystemResponse toSystemResponse(Systems system);

}
