package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.tickets.dto.TicketFiltersParams;
import br.com.luizbrand.worklog.tickets.dto.TicketLogResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketRequest;
import br.com.luizbrand.worklog.tickets.dto.TicketResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketSummary;
import br.com.luizbrand.worklog.tickets.dto.TicketUpdateRequest;
import br.com.luizbrand.worklog.user.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController implements TicketControllerDocs {

    private final TicketService ticketService;
    private final TicketLogManager ticketLogManager;

    public TicketController(TicketService ticketService, TicketLogManager ticketLogManager) {
        this.ticketService = ticketService;
        this.ticketLogManager = ticketLogManager;
    }

    @GetMapping
    public ResponseEntity<Page<TicketSummary>> findAllTickets(TicketFiltersParams filters, Pageable pageable) {
        return ResponseEntity.ok(ticketService.findAll(filters, pageable));
    }

    @PostMapping("/create")
    public ResponseEntity<TicketResponse> createTicket(@RequestBody @Valid TicketRequest ticketRequest) {
        TicketResponse ticketResponse = ticketService.createTicket(ticketRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketResponse);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<TicketResponse> getTicketByPublicId(@PathVariable UUID publicId) {
        TicketResponse ticketResponse = ticketService.findTicketByPublicId(publicId);
        return ResponseEntity.ok(ticketResponse);
    }

    @GetMapping("/{publicId}/logs")
    public ResponseEntity<Page<TicketLogResponse>> getTicketLogs(@PathVariable UUID publicId, Pageable pageable) {
        return ResponseEntity.ok(ticketLogManager.findLogsByTicket(publicId, pageable));
    }

    @PutMapping("/update/{ticketPublicId}")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable UUID ticketPublicId,
                                                       @RequestBody @Valid TicketUpdateRequest ticketRequest,
                                                       @AuthenticationPrincipal User currentUser) {
        TicketResponse ticketResponse = ticketService.updateTicket(ticketPublicId, ticketRequest, currentUser);
        return ResponseEntity.ok(ticketResponse);

    }

}
