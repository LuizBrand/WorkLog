package br.com.luizbrand.worklog.role;

import br.com.luizbrand.worklog.role.dto.RoleResponse;
import br.com.luizbrand.worklog.role.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class RoleMapperTest {

    private final RoleMapper roleMapper = Mappers.getMapper(RoleMapper.class);

    @Nested
    @DisplayName("Method: toRoleResponse()")
    class ToRoleResponseTests {

        @Test
        @DisplayName("Should map the Role name to the RoleResponse role field")
        void shouldMapRoleNameToResponse() {
            Role role = new Role();
            role.setName(RoleName.USER);

            RoleResponse response = roleMapper.toRoleResponse(role);

            assertThat(response).isNotNull();
            assertThat(response.role()).isEqualTo(RoleName.USER);
        }

        @Test
        @DisplayName("Should return null when the role is null")
        void shouldReturnNullForNullRole() {
            assertThat(roleMapper.toRoleResponse(null)).isNull();
        }
    }
}
