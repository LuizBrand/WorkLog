package br.com.luizbrand.worklog.mapper;

import br.com.luizbrand.worklog.dto.request.RegisterRequest;
import br.com.luizbrand.worklog.dto.response.AuthResponse;
import br.com.luizbrand.worklog.dto.response.UserResponse;
import br.com.luizbrand.worklog.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    User toUser(RegisterRequest userRequest);
    AuthResponse toAuthResponse(User user);
    UserResponse toUserResponse(User user);

}
