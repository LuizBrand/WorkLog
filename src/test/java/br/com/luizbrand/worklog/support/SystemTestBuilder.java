package br.com.luizbrand.worklog.support;

import br.com.luizbrand.worklog.system.Systems;

import java.time.LocalDateTime;
import java.util.UUID;

public final class SystemTestBuilder {

    private Long id = 1L;
    private UUID publicId = UUID.randomUUID();
    private String name = "Test System";
    private Boolean enabled = true;
    private LocalDateTime createdAt = LocalDateTime.of(2026, 4, 19, 10, 0);
    private LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 19, 10, 0);

    public static SystemTestBuilder aSystem() {
        return new SystemTestBuilder();
    }

    public SystemTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public SystemTestBuilder withPublicId(UUID publicId) {
        this.publicId = publicId;
        return this;
    }

    public SystemTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public SystemTestBuilder disabled() {
        this.enabled = false;
        return this;
    }

    public SystemTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Systems build() {
        return Systems.builder()
                .id(id)
                .publicId(publicId)
                .name(name)
                .isEnabled(enabled)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
