package br.com.luizbrand.worklog.support;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.tickets.Ticket;
import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import br.com.luizbrand.worklog.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

public final class TicketTestBuilder {

    private Long id = 1L;
    private UUID publicId = UUID.randomUUID();
    private String title = "Test Ticket";
    private String description = "Customer reported an issue.";
    private String solution = null;
    private LocalDateTime completedAt = null;
    private TicketStatus status = TicketStatus.PENDING;
    private Client client = ClientTestBuilder.aClient().build();
    private Systems system = SystemTestBuilder.aSystem().build();
    private User user = UserTestBuilder.aUser().build();
    private Boolean enabled = true;
    private LocalDateTime createdAt = LocalDateTime.of(2026, 4, 19, 10, 0);
    private LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 19, 10, 0);

    public static TicketTestBuilder aTicket() {
        return new TicketTestBuilder();
    }

    public TicketTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TicketTestBuilder withPublicId(UUID publicId) {
        this.publicId = publicId;
        return this;
    }

    public TicketTestBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public TicketTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public TicketTestBuilder withSolution(String solution) {
        this.solution = solution;
        return this;
    }

    public TicketTestBuilder withCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public TicketTestBuilder withStatus(TicketStatus status) {
        this.status = status;
        return this;
    }

    public TicketTestBuilder withClient(Client client) {
        this.client = client;
        return this;
    }

    public TicketTestBuilder withSystem(Systems system) {
        this.system = system;
        return this;
    }

    public TicketTestBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public TicketTestBuilder disabled() {
        this.enabled = false;
        return this;
    }

    public Ticket build() {
        return Ticket.builder()
                .id(id)
                .publicId(publicId)
                .title(title)
                .description(description)
                .solution(solution)
                .completedAt(completedAt)
                .status(status)
                .client(client)
                .system(system)
                .user(user)
                .isEnabled(enabled)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
