package br.com.luizbrand.worklog.mapper;

import br.com.luizbrand.worklog.dto.response.RoleResponse;
import br.com.luizbrand.worklog.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "role", source = "name")
    RoleResponse toRoleResponse(Role role);


}
