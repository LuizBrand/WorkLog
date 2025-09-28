package br.com.luizbrand.worklog.mapper;

import br.com.luizbrand.worklog.dto.request.RegisterRequest;
import br.com.luizbrand.worklog.dto.response.RegisterResponse;
import br.com.luizbrand.worklog.dto.response.RoleResponse;
import br.com.luizbrand.worklog.dto.response.UserResponse;
import br.com.luizbrand.worklog.entity.Role;
import br.com.luizbrand.worklog.entity.User;
import br.com.luizbrand.worklog.enums.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void shouldMapRegisterRequestToUser() {
        //ARRANGE
        RegisterRequest request = new RegisterRequest("username", "user@gmail.com", "L4F6b7l4@");

        //ACT
        User user = userMapper.toUser(request);

        //ASSERT
        assertNotNull(user);
        assertEquals("username", user.getName());
        assertEquals("user@gmail.com", user.getEmail());
        assertEquals("L4F6b7l4@", user.getPassword());

    }

    @Test
    void shouldMapUserToRegisterResponse() {
        //ARRANGE
        LocalDateTime createdDate = LocalDateTime.of(2025, 9, 27, 10, 30, 00);
        String uuid = "0abcfc81-9411-40a6-8cbc-d3f690da4ef0";
        UUID publicId = UUID.fromString(uuid);

        User user = new User();
        user.setName("username");
        user.setEmail("user@gmail.com");
        user.setPublicId(publicId);
        user.setCreatedAt(createdDate);

        //ACT
        RegisterResponse userResponse = userMapper.toRegisterResponse(user);

        //ASSERT
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String expectedDateString = createdDate.format(formatter);

        assertNotNull(userResponse);
        assertEquals("username", userResponse.name());
        assertEquals("user@gmail.com", userResponse.email());
        assertEquals(user.getPublicId().toString(),  userResponse.publicId());
        assertEquals(expectedDateString, userResponse.createdAt());

    }

    @Test
    void shouldMapUserToUserResponse() {

        //ARRANGE
        UUID publicId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef0");
        Role userRole = new Role();
        userRole.setName(RoleName.USER);

        User user = new User();
        user.setPublicId(publicId);
        user.setName("username");
        user.setEmail("user@gmail.com");
        user.setRoles(Set.of(userRole));

        RoleResponse roleResponse = new RoleResponse(RoleName.USER);
        when(roleMapper.toRoleResponse(userRole)).thenReturn(roleResponse);

        //ACT
        UserResponse userResponse = userMapper.toUserResponse(user);

        //ASSERT
        assertNotNull(userResponse);
        assertEquals(user.getPublicId().toString(), userResponse.publicId());
        assertEquals(user.getName(), userResponse.name());
        assertEquals(user.getEmail(), userResponse.email());
        assertTrue(userResponse.roles().contains(roleResponse));

        verify(roleMapper, times(1)).toRoleResponse(userRole);

    }
}