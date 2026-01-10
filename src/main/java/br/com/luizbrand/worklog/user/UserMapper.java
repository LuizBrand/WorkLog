package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.auth.RegisterRequest;
import br.com.luizbrand.worklog.auth.AuthResponse;
import br.com.luizbrand.worklog.role.RoleMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    User toUser(RegisterRequest userRequest);
    AuthResponse toAuthResponse(User user);
    UserResponse toUserResponse(User user);

}
