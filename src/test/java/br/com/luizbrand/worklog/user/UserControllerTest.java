package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.auth.AuthFilter;
import br.com.luizbrand.worklog.auth.CustomUserDetailsService;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.role.dto.RoleResponse;
import br.com.luizbrand.worklog.exception.NotFound.UserNotFoundException;
import br.com.luizbrand.worklog.role.enums.RoleName;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.user.dto.ChangePasswordRequest;
import br.com.luizbrand.worklog.user.dto.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ActiveProfiles("test")
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthFilter authFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UserResponse userResponse;
    private UUID nonExistenId;
    private String notFoundExpectedMessage;

    @BeforeEach
    void setUp() {

        LocalDateTime createdDate = LocalDateTime.of(2025, 9, 27, 10, 30, 00);
        nonExistenId = UUID.randomUUID();
        String publicId = "0abcfc81-9411-40a6-8cbc-d3f690da4ef0";
        RoleResponse roleResponse = new RoleResponse(RoleName.USER);

        userResponse = new UserResponse(
                publicId,
                "user@gmail.com",
                "username",
                Set.of(roleResponse),
                createdDate.toString());


        notFoundExpectedMessage =  "User with id: " + nonExistenId + " not found";
    }

    @Nested
    @DisplayName("Endpoint: GET /users/")
    class findAllUsers {

        @Test
        @DisplayName("Should return 200 OK status and a list with all users when exists")
        void shouldReturnOkAndUserListWhenUsersExists() throws Exception {

            when(userService.findAll()).thenReturn(List.of(userResponse));

            mockMvc.perform(get("/users/"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].publicId").value(userResponse.publicId()));
        }

        @Test
        @DisplayName("Should return 200 OK status and an empty list when no users exists")
        void shouldReturnOkAndEmptyListWhenNoUsersExists() throws Exception {

            when(userService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/users/"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().json("[]"));
        }

    }

    @Nested
    @DisplayName("Endpoint: GET /users/{publicId}")
    class findUserByPublicId {

        @Test
        @DisplayName("Should return 200 OK status and an User when ID exists")
        void shouldReturnAnUserWhenFoundByPublicId() throws Exception {

            when(userService.findByPublicId(UUID.fromString(userResponse.publicId())))
                    .thenReturn(userResponse);

            mockMvc.perform(get("/users/{publicId}", userResponse.publicId()).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.publicId").value((userResponse.publicId())))
                    .andExpect(jsonPath("$.email").value(userResponse.email()))
                    .andExpect(jsonPath("$.name").value(userResponse.name()))
                    .andExpect(jsonPath("$.roles[0].role").value(RoleName.USER.toString()));
        }

        @Test
        @DisplayName("Should return 404 Not Found when User does not exists")
        void shouldReturnNotFoundWhenUserDoesNotExists() throws Exception {

            when(userService.findByPublicId(nonExistenId))
                    .thenThrow(new UserNotFoundException(notFoundExpectedMessage));

            mockMvc.perform(get("/users/{publicId}", nonExistenId))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(notFoundExpectedMessage));
        }

    }

    @Nested
    @DisplayName("Endpoint: GET /users/me")
    class getMe {

        private User authenticatedUser;

        @BeforeEach
        void authenticate() {
            authenticatedUser = UserTestBuilder.aUser()
                    .withPublicId(UUID.fromString(userResponse.publicId()))
                    .withName(userResponse.name())
                    .withEmail(userResponse.email())
                    .build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser, null, authenticatedUser.getAuthorities()));
        }

        @AfterEach
        void clearAuthentication() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 200 OK with the authenticated principal's data")
        void shouldReturnOkWithAuthenticatedUserData() throws Exception {

            when(userService.getMe(any(User.class))).thenReturn(userResponse);

            mockMvc.perform(get("/users/me").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.publicId").value(userResponse.publicId()))
                    .andExpect(jsonPath("$.email").value(userResponse.email()))
                    .andExpect(jsonPath("$.name").value(userResponse.name()))
                    .andExpect(jsonPath("$.roles[0].role").value(RoleName.USER.toString()));
        }

        @Test
        @DisplayName("Should pass the authenticated principal through to the service")
        void shouldPassAuthenticatedPrincipalThroughToService() throws Exception {

            when(userService.getMe(eq(authenticatedUser))).thenReturn(userResponse);

            mockMvc.perform(get("/users/me").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(userService, times(1)).getMe(eq(authenticatedUser));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /users/{publicId}/deactivate")
    class deactiveUserByPublicId {

        @Test
        @DisplayName("Should return 204 No Content when user is deactivated sucessfully")
        void shouldReturnNoContentWhenUserIsDeactivatedSucessfully() throws Exception {

            UUID publicId = UUID.randomUUID();

            doNothing().when(userService).deactiveUser(publicId);

            mockMvc.perform(post("/users/{publicId}/deactivate", publicId))
                    .andExpect(status().isNoContent());

            verify(userService, times(1)).deactiveUser(publicId);

        }

        @Test
        @DisplayName("Should return status 404 Not Found when deactivating a non-existent user")
        void shouldReturnNotFoundWhenDeactivatingNonExistentUser() throws Exception {

            doThrow(new UserNotFoundException(notFoundExpectedMessage))
                    .when(userService).deactiveUser(nonExistenId);

            mockMvc.perform(post("/users/{publicId}/deactivate", nonExistenId))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(notFoundExpectedMessage));

        }
    }

    @Nested
    @DisplayName("Endpoint: POST /users/me/change-password")
    class ChangeMyPassword {

        private User authenticatedUser;
        private ChangePasswordRequest validRequest;

        @BeforeEach
        void authenticate() {
            authenticatedUser = UserTestBuilder.aUser()
                    .withPublicId(UUID.fromString(userResponse.publicId()))
                    .withName(userResponse.name())
                    .withEmail(userResponse.email())
                    .build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser, null, authenticatedUser.getAuthorities()));

            validRequest = new ChangePasswordRequest(
                    "current-plain",
                    "NewStrong1Password",
                    UUID.randomUUID().toString());
        }

        @AfterEach
        void clearAuthentication() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 204 No Content when the password is changed successfully")
        void shouldReturnNoContentOnSuccess() throws Exception {

            doNothing().when(userService).changeMyPassword(any(User.class), any(ChangePasswordRequest.class));

            mockMvc.perform(post("/users/me/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNoContent());

            verify(userService, times(1))
                    .changeMyPassword(any(User.class), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("Should return 422 Unprocessable Entity when the service throws BusinessException")
        void shouldReturnUnprocessableEntityWhenServiceThrowsBusiness() throws Exception {

            doThrow(new BusinessException("Senha atual incorreta"))
                    .when(userService).changeMyPassword(any(User.class), any(ChangePasswordRequest.class));

            mockMvc.perform(post("/users/me/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Senha atual incorreta"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when the body is missing required fields")
        void shouldReturnBadRequestWhenBodyIsInvalid() throws Exception {

            ChangePasswordRequest invalid = new ChangePasswordRequest(" ", "short", " ");

            mockMvc.perform(post("/users/me/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(userService, never())
                    .changeMyPassword(any(User.class), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("Should pass the authenticated principal through to the service")
        void shouldPassAuthenticatedPrincipalThroughToService() throws Exception {

            doNothing().when(userService)
                    .changeMyPassword(eq(authenticatedUser), any(ChangePasswordRequest.class));

            mockMvc.perform(post("/users/me/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNoContent());

            verify(userService, times(1))
                    .changeMyPassword(eq(authenticatedUser), any(ChangePasswordRequest.class));
        }
    }

}