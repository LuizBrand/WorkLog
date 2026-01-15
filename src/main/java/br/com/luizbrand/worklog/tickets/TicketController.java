package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.tickets.dto.TicketRequest;
import br.com.luizbrand.worklog.tickets.dto.TicketResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
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

    @PutMapping("/update/{ticketPublicId}")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable UUID ticketPublicId, @RequestBody @Valid TicketUpdateRequest ticketRequest) {
        TicketResponse ticketResponse = ticketService.updateTicket(ticketPublicId, ticketRequest);
        return ResponseEntity.ok(ticketResponse);

    }

}
