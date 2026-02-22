package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterResponse;
import br.com.luizbrand.worklog.role.RoleMapper;
import br.com.luizbrand.worklog.user.dto.UserResponse;
import br.com.luizbrand.worklog.user.dto.UserSummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    User toUser(RegisterRequest userRequest);
    RegisterResponse toAuthResponse(User user);
    UserResponse toUserResponse(User user);
    UserSummary toUserSummary(User user);

}
