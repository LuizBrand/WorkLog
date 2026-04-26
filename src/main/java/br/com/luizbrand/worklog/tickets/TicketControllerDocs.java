package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.exceptionhandler.ApiExceptionResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketFiltersParams;
import br.com.luizbrand.worklog.tickets.dto.TicketLogResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketRequest;
import br.com.luizbrand.worklog.tickets.dto.TicketResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketSummary;
import br.com.luizbrand.worklog.tickets.dto.TicketUpdateRequest;
import br.com.luizbrand.worklog.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Tickets", description = "Gerenciamento de tickets de suporte — criação, consulta e atualização com log de alterações")
public interface TicketControllerDocs {

    @Operation(summary = "Listar tickets",
            description = "Retorna uma página de tickets (resumo) com filtros opcionais por título, status, cliente, sistema, usuário, faixa de datas de criação e visibilidade. "
                    + "O parâmetro `visibility` (ATIVO/INATIVO/TODOS) só é honrado para usuários ADMIN; usuários comuns sempre veem apenas tickets ativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de tickets",
                    content = @Content(schema = @Schema(implementation = TicketSummary.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<Page<TicketSummary>> findAllTickets(TicketFiltersParams filters,
                                                       Pageable pageable,
                                                       @Parameter(hidden = true) User currentUser);

    @Operation(summary = "Soft-delete de ticket",
            description = "Marca o ticket como inativo (`isEnabled = false`). Operação reservada para usuários ADMIN. O histórico de auditoria é preservado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ticket desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ticket não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado — requer role ADMIN")
    })
    ResponseEntity<Void> deleteTicket(
            @Parameter(description = "ID público do ticket (UUID)", required = true) UUID publicId);

    @Operation(summary = "Criar ticket",
            description = "Cria um novo ticket de suporte. O cliente e o sistema devem estar ativos. O usuário responsável é opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket criado com sucesso",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "422", description = "Cliente ou sistema inativo",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente, sistema ou usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<TicketResponse> createTicket(TicketRequest ticketRequest);

    @Operation(summary = "Buscar ticket por ID",
            description = "Retorna um ticket específico pelo seu publicId (UUID), incluindo dados do cliente, sistema e usuário responsável.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket encontrado",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<TicketResponse> getTicketByPublicId(
            @Parameter(description = "ID público do ticket (UUID)", required = true) UUID publicId);

    @Operation(summary = "Listar logs de auditoria do ticket",
            description = "Retorna uma página do histórico de alterações (TicketLog) do ticket informado, ordenada por data de alteração decrescente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de logs do ticket",
                    content = @Content(schema = @Schema(implementation = TicketLogResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<Page<TicketLogResponse>> getTicketLogs(
            @Parameter(description = "ID público do ticket (UUID)", required = true) UUID publicId,
            Pageable pageable);

    @Operation(summary = "Atualizar ticket",
            description = "Atualiza um ticket existente. Apenas os campos enviados serão alterados. "
                    + "Todas as mudanças são registradas automaticamente no log de auditoria (TicketLog).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ticket não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    ResponseEntity<TicketResponse> updateTicket(
            @Parameter(description = "ID público do ticket (UUID)", required = true) UUID ticketPublicId,
            TicketUpdateRequest ticketRequest,
            @Parameter(hidden = true) User currentUser);

}
