package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.auth.dto.LoginRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterResponse;
import br.com.luizbrand.worklog.exceptionhandler.ApiExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Autenticação", description = "Endpoints de registro, login, refresh token e logout (tokens via cookies HttpOnly)")
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
            description = "Autentica o usuário e emite os tokens via cookies `worklog_access` (path `/`) e `worklog_refresh` (path `/worklog/auth`). Ambos são HttpOnly e SameSite=Strict.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Login realizado com sucesso; cookies emitidos via Set-Cookie"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    ResponseEntity<Void> login(LoginRequest login);

    @Operation(summary = "Renovar access token",
            description = "Gera um novo access token e um novo refresh token a partir do cookie `worklog_refresh`. O refresh token anterior é invalidado e os cookies são re-emitidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token renovado com sucesso; novos cookies emitidos"),
            @ApiResponse(responseCode = "401", description = "Refresh token cookie ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class)))
    })
    ResponseEntity<Void> refreshToken(String refreshToken);

    @Operation(summary = "Realizar logout",
            description = "Invalida o refresh token (se presente) no Redis e expira os cookies de auth.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso; cookies expirados via Set-Cookie")
    })
    ResponseEntity<Void> logout(String refreshToken);

}
