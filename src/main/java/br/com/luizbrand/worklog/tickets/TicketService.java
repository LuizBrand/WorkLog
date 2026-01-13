package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.client.ClientService;
import br.com.luizbrand.worklog.exception.NotFound.TicketNotFoundException;
import br.com.luizbrand.worklog.system.SystemService;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.tickets.dto.TicketRequest;
import br.com.luizbrand.worklog.tickets.dto.TicketResponse;
import br.com.luizbrand.worklog.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final ClientService clientService;
    private final SystemService systemService;
    private final UserService userService;

    public TicketService(TicketRepository ticketRepository, TicketMapper ticketMapper, ClientService clientService, SystemService systemService, UserService userService) {

        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
        this.clientService = clientService;
        this.systemService = systemService;
        this.userService = userService;
    }

    @Transactional
    public TicketResponse createTicket(TicketRequest ticketRequest) {

        Ticket ticket = ticketMapper.toEntity(ticketRequest);

        Client client = clientService.findActiveClient(ticketRequest.clientId());
        Systems system = systemService.findActiveSystem(ticketRequest.systemId());
        ticket.setClient(client);
        ticket.setSystem(system);

        if (ticketRequest.userId() != null) {
            ticket.setUser(userService.findActiveUser(ticketRequest.userId()));
        }

        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    public TicketResponse findTicketByPublicId(UUID publicId) {
        Ticket ticket = ticketRepository.findByPublicId(publicId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with publicId: " + publicId));
        return ticketMapper.toResponse(ticket);
    }

}
