package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.exceptionhandler.ApiExceptionResponse;
import br.com.luizbrand.worklog.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Usuários", description = "Consulta e gerenciamento de usuários do sistema")
public interface UserControllerDocs {

    @Operation(summary = "Listar todos os usuários",
            description = "Retorna todos os usuários cadastrados com suas roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<List<UserResponse>> findAllUsers();

    @Operation(summary = "Buscar usuário por ID",
            description = "Retorna um usuário específico pelo seu publicId (UUID).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<UserResponse> findUserByPublicId(
            @Parameter(description = "ID público do usuário (UUID)", required = true) UUID publicId);

    @Operation(summary = "Desativar usuário",
            description = "Desativa um usuário (soft delete). Requer role ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — requer role ADMIN")
    })
    ResponseEntity<Void> deactiveUserByPublicId(
            @Parameter(description = "ID público do usuário (UUID)", required = true) UUID publicId);

}
