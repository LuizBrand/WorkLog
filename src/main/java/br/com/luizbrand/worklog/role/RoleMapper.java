package br.com.luizbrand.worklog.role;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "role", source = "name")
    RoleResponse toRoleResponse(Role role);


}
