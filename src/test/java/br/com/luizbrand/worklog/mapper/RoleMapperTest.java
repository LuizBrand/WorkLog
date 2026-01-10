package br.com.luizbrand.worklog.mapper;

import br.com.luizbrand.worklog.dto.response.RoleResponse;
import br.com.luizbrand.worklog.entity.Role;
import br.com.luizbrand.worklog.role.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RoleMapperTest {

    @Autowired
    private RoleMapper roleMapper;

    @Test
    @DisplayName("Should map Role to RoleResponse using mapStruct implementaion")
    void shouldMapRoletoRoleResponse() {
        //ARRANGE
        Role role = new Role();
        role.setName(RoleName.USER);

        // ACT
        RoleResponse roleResponse = roleMapper.toRoleResponse(role);

        //ASSERT
        assertNotNull(roleResponse);
        assertEquals(RoleName.USER, roleResponse.role());

    }
}