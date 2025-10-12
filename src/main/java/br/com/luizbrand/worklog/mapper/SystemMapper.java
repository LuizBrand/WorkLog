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
    Systems toSystem(SystemRequest systemRequest);

    @Mapping(target = "id", ignore = true)
    Systems toSystem(SystemResponse systemResponse);
    SystemResponse toSystemResponse(Systems system);

}
