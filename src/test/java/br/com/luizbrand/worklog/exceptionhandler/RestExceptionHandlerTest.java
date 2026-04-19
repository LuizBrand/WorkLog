package br.com.luizbrand.worklog.exceptionhandler;

import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Business.RefreshTokenException;
import br.com.luizbrand.worklog.exception.Conflict.ResourceAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new RestExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("ResourceNotFoundException -> 404 with Not Found body")
    void shouldReturn404ForResourceNotFound() throws Exception {
        mockMvc.perform(get("/throw/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("resource missing"))
                .andExpect(jsonPath("$.path").value("/throw/not-found"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("ResourceAlreadyExistsException -> 409 with Conflict body")
    void shouldReturn409ForResourceAlreadyExists() throws Exception {
        mockMvc.perform(get("/throw/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("already exists"))
                .andExpect(jsonPath("$.path").value("/throw/conflict"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("BusinessException -> 422 with Business role exception body")
    void shouldReturn422ForBusinessException() throws Exception {
        mockMvc.perform(get("/throw/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Business role exception"))
                .andExpect(jsonPath("$.message").value("rule violated"))
                .andExpect(jsonPath("$.path").value("/throw/business"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("RefreshTokenException -> 401 with Unauthorized body")
    void shouldReturn401ForRefreshTokenException() throws Exception {
        mockMvc.perform(get("/throw/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("expired refresh"))
                .andExpect(jsonPath("$.path").value("/throw/refresh"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 with fieldErrors")
    void shouldReturn400WithFieldErrorsForValidation() throws Exception {
        String invalidPayload = "{\"name\":\"\"}";

        mockMvc.perform(post("/throw/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation Exception. Check the fields"))
                .andExpect(jsonPath("$.path").value("/throw/validate"))
                .andExpect(jsonPath("$.fieldErrors.name").value("must not be blank"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/throw/not-found")
        void throwNotFound() {
            throw new ResourceNotFoundException("resource missing");
        }

        @GetMapping("/throw/conflict")
        void throwConflict() {
            throw new ResourceAlreadyExistsException("already exists");
        }

        @GetMapping("/throw/business")
        void throwBusiness() {
            throw new BusinessException("rule violated");
        }

        @GetMapping("/throw/refresh")
        void throwRefresh() {
            throw new RefreshTokenException("expired refresh");
        }

        @PostMapping("/throw/validate")
        void validatePayload(@Valid @RequestBody ValidatedPayload payload) {
        }

        record ValidatedPayload(@NotBlank String name) {
        }
    }
}
