package br.com.luizbrand.worklog.support;

import br.com.luizbrand.worklog.role.Role;
import br.com.luizbrand.worklog.role.enums.RoleName;

public final class RoleTestBuilder {

    private Long id = 1L;
    private RoleName name = RoleName.USER;

    public static RoleTestBuilder aRole() {
        return new RoleTestBuilder();
    }

    public static Role userRole() {
        return aRole().withName(RoleName.USER).build();
    }

    public static Role adminRole() {
        return aRole().withId(2L).withName(RoleName.ADMIN).build();
    }

    public RoleTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public RoleTestBuilder withName(RoleName name) {
        this.name = name;
        return this;
    }

    public Role build() {
        return Role.builder()
                .Id(id)
                .name(name)
                .build();
    }
}
