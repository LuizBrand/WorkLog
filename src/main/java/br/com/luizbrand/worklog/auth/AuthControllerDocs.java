package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.auth.dto.AuthenticationResponse;
import br.com.luizbrand.worklog.auth.dto.LoginRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterResponse;
import br.com.luizbrand.worklog.auth.refreshtoken.RefreshTokenRequest;
import br.com.luizbrand.worklog.exceptionhandler.ApiExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação", description = "Endpoints de registro, login, refresh token e logout")
@SecurityRequirements
public interface AuthControllerDocs {

    @Operation(summary = "Registrar novo usuário",
            description = "Cria um novo usuário com a role padrão USER. O email deve ser único no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email já cadastrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class)))
    })
    ResponseEntity<RegisterResponse> register(RegisterRequest request);

    @Operation(summary = "Realizar login",
            description = "Autentica o usuário e retorna um access token JWT e um refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    ResponseEntity<AuthenticationResponse> login(LoginRequest login);

    @Operation(summary = "Renovar access token",
            description = "Gera um novo access token e um novo refresh token a partir de um refresh token válido. O refresh token anterior é invalidado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado com sucesso",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class)))
    })
    ResponseEntity<AuthenticationResponse> refreshToken(RefreshTokenRequest request);

    @Operation(summary = "Realizar logout",
            description = "Invalida o refresh token do usuário no Redis.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso")
    })
    ResponseEntity<Void> logout(RefreshTokenRequest request);

}
