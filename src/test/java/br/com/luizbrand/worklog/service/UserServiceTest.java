package br.com.luizbrand.worklog.service;

import br.com.luizbrand.worklog.dto.response.RoleResponse;
import br.com.luizbrand.worklog.dto.response.UserResponse;
import br.com.luizbrand.worklog.entity.Role;
import br.com.luizbrand.worklog.entity.User;
import br.com.luizbrand.worklog.enums.RoleName;
import br.com.luizbrand.worklog.exception.NotFound.UserNotFoundException;
import br.com.luizbrand.worklog.mapper.UserMapper;
import br.com.luizbrand.worklog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserResponse userResponse;
    private UUID nonExistenId;

    @BeforeEach
    void setUp() {

        nonExistenId = UUID.randomUUID();

        LocalDateTime createdDate = LocalDateTime.of(2025, 9, 27, 10, 30, 00);
        UUID publicId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef0");
        Role userRole = new Role();
        userRole.setName(RoleName.USER);

        user = new User();
        user.setPublicId(publicId);
        user.setName("username");
        user.setEmail("user@gmail.com");
        user.setRoles(Set.of(userRole));
        user.setCreatedAt(createdDate);
        user.setUserEnabled(true);
        RoleResponse roleResponse = new RoleResponse(RoleName.USER);

        userResponse = new UserResponse(
                "0abcfc81-9411-40a6-8cbc-d3f690da4ef0",
                "user@gmail.com",
                "username",
                Set.of(roleResponse),
                createdDate.toString());

    }

    @Nested
    @DisplayName("Method: findAll()")
    class FindAllUsersTests {

        @Test
        @DisplayName("Should Return a List with all Users when exists.")
        void shouldReturnAListOfAllUsersWhenExists() {

            //ARRANGE
            when(userRepository.findAll()).thenReturn(List.of(user));
            when(userMapper.toUserResponse(any(User.class)))
                    .thenReturn(userResponse);

            //ACT
            List<UserResponse> users = userService.findAll();

            //ASSERT
            assertNotNull(users);
            assertEquals(1, users.size());
            assertEquals(userResponse, users.get(0));

            verify(userRepository, times(1)).findAll();
            verify(userMapper, times(1)).toUserResponse(user);

        }

        @Test
        @DisplayName("Should Return an Empty List when no Users exist.")
        void shouldReturnAnEmptyListOfUsersWhenNoUsersExist() {
            //ARRANGE
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            //ACT
            List<UserResponse> users = userService.findAll();

            //ASSERT
            assertNotNull(users);
            assertEquals(0, users.size());

            verify(userRepository, times(1)).findAll();
            verify(userMapper, never()).toUserResponse(any(User.class));
        }
    }

    @Nested
    @DisplayName("Method: findUserByPublicId()")
    class FindUserByPublicIdTests {

        @Test
        @DisplayName("Should Return an User when exists.")
        void shouldReturnAnUserByPublicIdWhenExists() {

            //ARRANGE
            when(userRepository.findByPublicId(user.getPublicId()))
                    .thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(any(User.class)))
                    .thenReturn(userResponse);

            //ACT
            UserResponse response = userService.findByPublicId(user.getPublicId());

            //ASSERT
            assertNotNull(response);

            verify(userRepository, times(1)).findByPublicId(user.getPublicId());
            verify(userMapper, times(1)).toUserResponse(user);

        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not exists")
        void shouldThrowUserNotFoundExceptionWhenFindingNonExistentUser() {

            //ARRANGE
            String expectedMessage = "User with id: " + nonExistenId + " not found";

            when(userRepository.findByPublicId(nonExistenId))
                    .thenReturn(Optional.empty());

            //ACT & ASSERT
            UserNotFoundException exception = assertThrows(
                    UserNotFoundException.class, () -> userService.findByPublicId(nonExistenId));
            assertEquals(expectedMessage, exception.getMessage() );

            verify(userRepository, times(1)).findByPublicId(nonExistenId);

        }
    }

    @Nested
    @DisplayName("Method: deactiveUser()")
    class DeactiveUserTests {

        @Test
        @DisplayName("Should deactivate user when exists")
        void shouldDeactivateWhenUserExists() {
            //ARRANGE
            user.setUserEnabled(true);
            when(userRepository.findByPublicId(user.getPublicId()))
                    .thenReturn(Optional.of(user));
            ArgumentCaptor<User> userCaptor = forClass(User.class);

            //ACT
            userService.deactiveUser(user.getPublicId());

            //ASSERT
            verify(userRepository, times(1)).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertFalse(savedUser.isUserEnabled());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not exists")
        void shouldThrowUserNotFoundExceptionWhenDeactivingNonExistenUser() {

            String expectedMessage = "User with id: " + nonExistenId + " not found";

            when(userRepository.findByPublicId(nonExistenId)).thenReturn(Optional.empty());

            UserNotFoundException exception = assertThrows(
                    UserNotFoundException.class, () -> userService.findByPublicId(nonExistenId));
            assertEquals(expectedMessage, exception.getMessage() );

            verify(userRepository, never()).save(any(User.class));

        }
    }


}