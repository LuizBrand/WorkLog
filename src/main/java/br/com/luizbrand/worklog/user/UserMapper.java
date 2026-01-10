package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.dto.AuthResponse;
import br.com.luizbrand.worklog.role.RoleMapper;
import br.com.luizbrand.worklog.user.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    User toUser(RegisterRequest userRequest);
    AuthResponse toAuthResponse(User user);
    UserResponse toUserResponse(User user);

}
