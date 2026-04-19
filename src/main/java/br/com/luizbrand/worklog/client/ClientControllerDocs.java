package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.exceptionhandler.ApiExceptionResponse;
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

@Tag(name = "Clientes", description = "Gerenciamento de clientes e seus sistemas associados")
public interface ClientControllerDocs {

    @Operation(summary = "Listar clientes",
            description = "Retorna todos os clientes com suporte a filtros por nome, status (ATIVO/INATIVO/TODOS) e sistemas associados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClientResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<List<ClientResponse>> findAllClients(ClientFiltersParams filtersParams);

    @Operation(summary = "Buscar cliente por ID",
            description = "Retorna um cliente específico pelo seu publicId (UUID).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<ClientResponse> findClientByPublicId(
            @Parameter(description = "ID público do cliente (UUID)", required = true) UUID publicId);

    @Operation(summary = "Criar cliente",
            description = "Cria um novo cliente associado a um ou mais sistemas. O nome deve ser único.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso",
                    content = @Content(schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cliente com este nome já existe",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<ClientResponse> saveClient(ClientRequest clientRequest);

    @Operation(summary = "Atualizar cliente",
            description = "Atualiza parcialmente um cliente. Apenas os campos enviados serão alterados (PATCH).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = ClientResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<ClientResponse> updateClient(
            ClientRequest clientRequest,
            @Parameter(description = "ID público do cliente (UUID)", required = true) UUID publicId);

}
