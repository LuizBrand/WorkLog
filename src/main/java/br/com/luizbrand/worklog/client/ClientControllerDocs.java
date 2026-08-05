package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.client.dto.ClientUpdateRequest;
import br.com.luizbrand.worklog.exceptionhandler.ApiExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Clientes", description = "Gerenciamento de clientes e seus sistemas associados")
public interface ClientControllerDocs {

    @Operation(summary = "Listar clientes",
            description = "Retorna os clientes com suporte a filtros por nome, status (ATIVO/INATIVO/TODOS), tipo, documento e sistemas associados. "
                    + "A paginação é opcional: sem `page` e sem `size` a resposta é um array de `ClientResponse`; "
                    + "informando qualquer um dos dois, a resposta vira um `Page` (`content`, `totalElements`, `totalPages`, `number`, `size`). "
                    + "Padrões da versão paginada: 12 itens por página ordenados por `name` ascendente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista (ou página) de clientes retornada com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClientResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<Object> findAllClients(
            ClientFiltersParams filtersParams,
            Pageable pageable,
            @Parameter(description = "Índice da página (base 0). Presente = resposta paginada.") Integer page,
            @Parameter(description = "Itens por página (padrão 12). Presente = resposta paginada.") Integer size);

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
            ClientUpdateRequest clientUpdateRequest,
            @Parameter(description = "ID público do cliente (UUID)", required = true) UUID publicId);

    @Operation(summary = "Excluir cliente (soft delete)",
            description = "Marca o cliente como inativo (`isEnabled = false`). Restrito a ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão (ADMIN obrigatório)")
    })
    ResponseEntity<Void> softDeleteClient(
            @Parameter(description = "ID público do cliente (UUID)", required = true) UUID publicId);

}
