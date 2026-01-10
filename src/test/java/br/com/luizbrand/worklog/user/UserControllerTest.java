package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.role.RoleResponse;
import br.com.luizbrand.worklog.exception.NotFound.UserNotFoundException;
import br.com.luizbrand.worklog.role.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
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

    @MockitoBean
    private UserService userService;

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

}