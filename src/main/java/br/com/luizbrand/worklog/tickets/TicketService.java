package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.client.ClientService;
import br.com.luizbrand.worklog.exception.NotFound.TicketNotFoundException;
import br.com.luizbrand.worklog.system.SystemService;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.tickets.dto.TicketRequest;
import br.com.luizbrand.worklog.tickets.dto.TicketResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketUpdateRequest;
import br.com.luizbrand.worklog.user.User;
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
    private final TicketLogManager ticketLogManager;

    public TicketService(TicketRepository ticketRepository, TicketMapper ticketMapper, ClientService clientService, SystemService systemService, UserService userService, TicketLogManager ticketLogManager) {

        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
        this.clientService = clientService;
        this.systemService = systemService;
        this.userService = userService;
        this.ticketLogManager = ticketLogManager;
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

        //TODO:verificar se o valor do status é valido

        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    public TicketResponse findTicketByPublicId(UUID publicId) {
        Ticket ticket = ticketRepository.findByPublicId(publicId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with publicId: " + publicId));
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateTicket(UUID ticketPublicId, TicketUpdateRequest ticketRequest) {

        Ticket existingTicket = ticketRepository.findByPublicId(ticketPublicId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with publicId: " + ticketPublicId));

        //TODO: Mudar para buscar no security context o user loggado
        User currentUser = null;

        if (ticketRequest.userId() != null) {
            currentUser = userService.findEntityByPublicId(ticketRequest.userId());
        } else {
            currentUser = existingTicket.getUser();
        }

        Ticket newTicket = prepareNewTicket(existingTicket, ticketRequest);

        //Chamar o log manager para registrar os logs
        ticketLogManager.generateLogs(existingTicket, newTicket, currentUser);
        //Atualiza o ticket realmente
        updateTicketEntity(existingTicket, ticketRequest);

        Ticket savedTicket = ticketRepository.save(existingTicket);

        return ticketMapper.toResponse(savedTicket);
    }

    private Ticket prepareNewTicket(Ticket existingTicket, TicketUpdateRequest ticketRequest) {
        return Ticket.builder()
                .title(ticketRequest.title() != null ? ticketRequest.title() : existingTicket.getTitle())
                .description(ticketRequest.description() != null ? ticketRequest.description() : existingTicket.getDescription())
                .solution(ticketRequest.solution() != null ? ticketRequest.solution() : existingTicket.getSolution())
                .status(ticketRequest.status() != null ? ticketRequest.status() : existingTicket.getStatus())
                .completedAt(ticketRequest.completedAt() != null ? ticketRequest.completedAt() : existingTicket.getCompletedAt())
                .client(existingTicket.getClient())
                .system(existingTicket.getSystem())
                .build();

    }

    private void  updateTicketEntity(Ticket ticket, TicketUpdateRequest request) {
        if (request.title() != null) ticket.setTitle(request.title());
        if (request.description() != null) ticket.setDescription(request.description());
        if (request.solution() != null) ticket.setSolution(request.solution());
        if (request.status() != null) ticket.setStatus(request.status());
        if (request.completedAt() != null) ticket.setCompletedAt(request.completedAt());
    }

}
