package br.com.luizbrand.worklog.support;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.system.Systems;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientTestBuilder {

    private Long id = 1L;
    private UUID publicId = UUID.randomUUID();
    private String name = "Test Client";
    private List<Systems> systems = new ArrayList<>();
    private Boolean enabled = true;
    private LocalDateTime createdAt = LocalDateTime.of(2026, 4, 19, 10, 0);
    private LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 19, 10, 0);

    public static ClientTestBuilder aClient() {
        return new ClientTestBuilder();
    }

    public ClientTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public ClientTestBuilder withPublicId(UUID publicId) {
        this.publicId = publicId;
        return this;
    }

    public ClientTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ClientTestBuilder withSystems(List<Systems> systems) {
        this.systems = systems;
        return this;
    }

    public ClientTestBuilder withSystem(Systems system) {
        this.systems = new ArrayList<>(List.of(system));
        return this;
    }

    public ClientTestBuilder disabled() {
        this.enabled = false;
        return this;
    }

    public ClientTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Client build() {
        return Client.builder()
                .id(id)
                .publicId(publicId)
                .name(name)
                .systems(systems)
                .isEnabled(enabled)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
