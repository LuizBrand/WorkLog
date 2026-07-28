package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
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

@Tag(name = "Filiais", description = "Filiais (unidades) de um cliente. A matriz nasce junto com o cliente")
public interface ClientBranchControllerDocs {

    @Operation(summary = "Listar filiais do cliente",
            description = """
                    Retorna as filiais do cliente, incluindo a matriz. Sem o parâmetro `status`
                    devolve ativas e inativas, igual ao `GET /clients`.
                    Funciona também para clientes desativados — só a criação exige cliente ativo.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de filiais retornada com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BranchResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<List<BranchResponse>> findBranchesByClient(
            @Parameter(description = "ID público do cliente (UUID)", required = true) UUID publicId,
            @Parameter(description = "Filtro de status: ATIVO, INATIVO ou TODOS (padrão: TODOS)") StatusFiltro status);

    @Operation(summary = "Cadastrar filial",
            description = """
                    Cadastra uma nova filial no cliente. O `documento` é obrigatório e precisa ter
                    checksum válido para o tipo do **cliente persistido** (CPF para PF, CNPJ para PJ) —
                    o tipo não é lido do corpo.

                    **Atenção ao status de erro:** documento com checksum inválido aqui responde
                    **422**, enquanto o mesmo erro no `POST /clients` responde **400**. A diferença é
                    intencional: no cadastro do cliente a validação é do Bean Validation (que conhece
                    o `tipo` enviado no corpo) e aqui é regra de serviço (que só pode consultar o tipo
                    já persistido).

                    Regras rejeitadas com 422: cliente inativo; cliente pessoa física (que tem apenas
                    a matriz); `isMatriz: true` (promover uma filial a matriz é feito pelo `PATCH`);
                    documento ausente; documento com checksum inválido.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Filial criada com sucesso",
                    content = @Content(schema = @Schema(implementation = BranchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento já cadastrado em outra filial",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<BranchResponse> createBranch(
            @Parameter(description = "ID público do cliente (UUID)", required = true) UUID publicId,
            BranchRequest branchRequest);

}
