package br.com.luizbrand.worklog.support;

import br.com.luizbrand.worklog.role.Role;
import br.com.luizbrand.worklog.user.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class UserTestBuilder {

    private Long id = 1L;
    private UUID publicId = UUID.randomUUID();
    private String name = "Test User";
    private String email = "user@worklog.test";
    private String password = "$2a$10$abcdefghijklmnopqrstuv";
    private Set<Role> roles = new HashSet<>(Set.of(RoleTestBuilder.userRole()));
    private Boolean enabled = true;
    private LocalDateTime createdAt = LocalDateTime.of(2026, 4, 19, 10, 0);
    private LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 19, 10, 0);

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder withPublicId(UUID publicId) {
        this.publicId = publicId;
        return this;
    }

    public UserTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserTestBuilder withRoles(Set<Role> roles) {
        this.roles = roles;
        return this;
    }

    public UserTestBuilder withRole(Role role) {
        this.roles = new HashSet<>(Set.of(role));
        return this;
    }

    public UserTestBuilder disabled() {
        this.enabled = false;
        return this;
    }

    public UserTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public User build() {
        return User.builder()
                .id(id)
                .publicId(publicId)
                .name(name)
                .email(email)
                .password(password)
                .roles(roles)
                .isEnabled(enabled)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
