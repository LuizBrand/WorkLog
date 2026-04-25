package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.auth.refreshtoken.RefreshToken;
import br.com.luizbrand.worklog.auth.refreshtoken.RefreshTokenService;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.role.Role;
import br.com.luizbrand.worklog.role.dto.RoleResponse;
import br.com.luizbrand.worklog.role.enums.RoleName;
import br.com.luizbrand.worklog.exception.NotFound.UserNotFoundException;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.user.dto.ChangePasswordRequest;
import br.com.luizbrand.worklog.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

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
        user.setIsEnabled(true);
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
            user.setIsEnabled(true);
            when(userRepository.findByPublicId(user.getPublicId()))
                    .thenReturn(Optional.of(user));
            ArgumentCaptor<User> userCaptor = forClass(User.class);

            //ACT
            userService.deactiveUser(user.getPublicId());

            //ASSERT
            verify(userRepository, times(1)).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertFalse(savedUser.isEnabled());
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

    @Nested
    @DisplayName("Method: findEntityByPublicId()")
    class FindEntityByPublicIdTests {

        @Test
        @DisplayName("Should return the entity when the user exists")
        void shouldReturnEntityWhenFound() {
            when(userRepository.findByPublicId(user.getPublicId())).thenReturn(Optional.of(user));

            User result = userService.findEntityByPublicId(user.getPublicId());

            assertEquals(user, result);
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when the user does not exist")
        void shouldThrowWhenMissing() {
            String expectedMessage = "User with public ID: " + nonExistenId + " not found";
            when(userRepository.findByPublicId(nonExistenId)).thenReturn(Optional.empty());

            UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                    () -> userService.findEntityByPublicId(nonExistenId));

            assertEquals(expectedMessage, ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Method: findActiveUser()")
    class FindActiveUserTests {

        @Test
        @DisplayName("Should return the entity when the user is enabled")
        void shouldReturnActiveUser() {
            user.setIsEnabled(true);
            when(userRepository.findByPublicId(user.getPublicId())).thenReturn(Optional.of(user));

            User result = userService.findActiveUser(user.getPublicId());

            assertEquals(user, result);
        }

        @Test
        @DisplayName("Should throw BusinessException when the user is disabled")
        void shouldThrowWhenDisabled() {
            user.setIsEnabled(false);
            when(userRepository.findByPublicId(user.getPublicId())).thenReturn(Optional.of(user));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.findActiveUser(user.getPublicId()));

            assertEquals("User is not active", ex.getMessage());
        }

        @Test
        @DisplayName("Should propagate UserNotFoundException when the user does not exist")
        void shouldPropagateNotFound() {
            when(userRepository.findByPublicId(nonExistenId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.findActiveUser(nonExistenId));
        }
    }

    @Nested
    @DisplayName("Method: findUserByEmail()")
    class FindUserByEmailTests {

        @Test
        @DisplayName("Should return Optional with user when the email exists")
        void shouldReturnUserWhenEmailExists() {
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            Optional<User> result = userService.findUserByEmail(user.getEmail());

            assertTrue(result.isPresent());
            assertEquals(user, result.get());
        }

        @Test
        @DisplayName("Should return an empty Optional when the email does not exist")
        void shouldReturnEmptyWhenEmailMissing() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            Optional<User> result = userService.findUserByEmail("ghost@example.com");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Method: changeMyPassword()")
    class ChangeMyPassword {

        private User currentUser;
        private RefreshToken storedToken;
        private ChangePasswordRequest request;

        @BeforeEach
        void prepare() {
            currentUser = UserTestBuilder.aUser()
                    .withEmail("user@gmail.com")
                    .withPassword("encoded-current-password")
                    .build();

            storedToken = new RefreshToken("user@gmail.com", 60_000L);
            request = new ChangePasswordRequest(
                    "current-plain",
                    "new-strong-password",
                    storedToken.getId());
        }

        @Test
        @DisplayName("Should re-hash the password, persist the user and revoke the supplied refresh token on success")
        void shouldRotatePasswordAndDeleteRefreshTokenOnHappyPath() {
            when(passwordEncoder.matches("current-plain", "encoded-current-password"))
                    .thenReturn(true);
            when(refreshTokenService.findByToken(storedToken.getId()))
                    .thenReturn(Optional.of(storedToken));
            when(passwordEncoder.encode("new-strong-password"))
                    .thenReturn("encoded-new-password");

            userService.changeMyPassword(currentUser, request);

            ArgumentCaptor<User> userCaptor = forClass(User.class);
            verify(userRepository, times(1)).save(userCaptor.capture());
            assertEquals("encoded-new-password", userCaptor.getValue().getPassword());
            verify(refreshTokenService, times(1)).deleteByToken(storedToken.getId());
        }

        @Test
        @DisplayName("Should throw BusinessException when the current password does not match")
        void shouldThrowWhenCurrentPasswordIsWrong() {
            when(passwordEncoder.matches("current-plain", "encoded-current-password"))
                    .thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.changeMyPassword(currentUser, request));

            assertEquals("Senha atual incorreta", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).deleteByToken(anyString());
            verify(refreshTokenService, never()).findByToken(anyString());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should throw BusinessException when the supplied refresh token does not exist")
        void shouldThrowWhenRefreshTokenIsMissing() {
            when(passwordEncoder.matches("current-plain", "encoded-current-password"))
                    .thenReturn(true);
            when(refreshTokenService.findByToken(storedToken.getId()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.changeMyPassword(currentUser, request));

            assertEquals("Refresh token inválido para o usuário", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).deleteByToken(anyString());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should throw BusinessException when the supplied refresh token belongs to another user")
        void shouldThrowWhenRefreshTokenBelongsToOtherUser() {
            RefreshToken otherToken = new RefreshToken("attacker@example.com", 60_000L);
            ChangePasswordRequest stolen = new ChangePasswordRequest(
                    "current-plain",
                    "new-strong-password",
                    otherToken.getId());

            when(passwordEncoder.matches("current-plain", "encoded-current-password"))
                    .thenReturn(true);
            when(refreshTokenService.findByToken(otherToken.getId()))
                    .thenReturn(Optional.of(otherToken));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.changeMyPassword(currentUser, stolen));

            assertEquals("Refresh token inválido para o usuário", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).deleteByToken(anyString());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

}