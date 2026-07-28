package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.dto.BranchUpdateRequest;
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

    @Operation(summary = "Atualizar filial",
            description = """
                    Atualiza parcialmente uma filial. Apenas os campos enviados são alterados (PATCH).

                    **`contatos`**: ausente não altera nada; presente **substitui a lista inteira**.

                    **`isMatriz: true`** promove esta filial a matriz e rebaixa a anterior na mesma
                    transação. **`isMatriz: false`** sobre a matriz atual é rejeitado com 422 — o
                    cliente ficaria sem matriz; promova outra filial no lugar.

                    **`enabled` não existe aqui**: inativar filial tem endpoint próprio
                    (`POST .../deactivate`).

                    Reenviar o **próprio** documento sem alterá-lo é aceito e não devolve 409.

                    Rejeitado com 422: cliente inativo; documento com checksum inválido para o tipo do
                    cliente; rebaixar a matriz; promover uma filial inativa; mais de um contato
                    marcado como `principal`.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filial atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = BranchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente ou filial não encontrada (inclui filial de outro cliente)",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento já cadastrado em outra filial",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "422", description = "Regra de negócio violada",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<BranchResponse> updateBranch(
            @Parameter(description = "ID público do cliente (UUID)", required = true) UUID publicId,
            @Parameter(description = "ID público da filial (UUID)", required = true) UUID branchId,
            BranchUpdateRequest branchUpdateRequest);

    @Operation(summary = "Inativar filial",
            description = """
                    Inativa a filial (`enabled = false`). **Não é exclusão**: o registro permanece e
                    continua aparecendo em `GET .../branches` sem filtro, ou com `status=INATIVO`.

                    **Consequência importante:** o `documento` da filial inativa **continua ocupando a
                    unicidade global**. Recadastrar aquele CNPJ em outro cliente devolve 409 enquanto
                    esta filial existir.

                    A **matriz não pode ser inativada** (422) — promova outra filial a matriz antes.

                    Requer perfil ADMIN.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Filial inativada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Cliente ou filial não encontrada (inclui filial de outro cliente)",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "422", description = "Tentativa de inativar a matriz, ou cliente inativo",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<Void> deactivateBranch(
            @Parameter(description = "ID público do cliente (UUID)", required = true) UUID publicId,
            @Parameter(description = "ID público da filial (UUID)", required = true) UUID branchId);

}
