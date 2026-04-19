package br.com.luizbrand.worklog.system;

import br.com.luizbrand.worklog.exceptionhandler.ApiExceptionResponse;
import br.com.luizbrand.worklog.system.dto.SystemRequest;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
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

@Tag(name = "Sistemas", description = "Gerenciamento dos sistemas/produtos atendidos pelo suporte")
public interface SystemControllerDocs {

    @Operation(summary = "Listar todos os sistemas",
            description = "Retorna todos os sistemas cadastrados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de sistemas retornada com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SystemResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<List<SystemResponse>> findAllSystems();

    @Operation(summary = "Buscar sistema por ID",
            description = "Retorna um sistema específico pelo seu publicId (UUID).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sistema encontrado",
                    content = @Content(schema = @Schema(implementation = SystemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sistema não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<SystemResponse> findSystemByPublicId(
            @Parameter(description = "ID público do sistema (UUID)", required = true) UUID publicId);

    @Operation(summary = "Criar sistema",
            description = "Cria um novo sistema. O nome deve ser único.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sistema criado com sucesso",
                    content = @Content(schema = @Schema(implementation = SystemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "409", description = "Sistema com este nome já existe",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<SystemResponse> saveSystem(SystemRequest systemRequest);

    @Operation(summary = "Atualizar sistema",
            description = "Atualiza parcialmente um sistema. Apenas os campos enviados serão alterados (PATCH).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sistema atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = SystemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sistema não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<SystemResponse> updateSystem(
            SystemRequest systemRequest,
            @Parameter(description = "ID público do sistema (UUID)", required = true) UUID publicId);

}
