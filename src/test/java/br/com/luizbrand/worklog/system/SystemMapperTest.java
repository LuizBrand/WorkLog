package br.com.luizbrand.worklog.system;

import br.com.luizbrand.worklog.support.SystemTestBuilder;
import br.com.luizbrand.worklog.system.dto.SystemRequest;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMapperTest {

    private final SystemMapper systemMapper = Mappers.getMapper(SystemMapper.class);

    @Nested
    @DisplayName("Method: toSystem()")
    class ToSystemTests {

        @Test
        @DisplayName("Should map the request name and leave id, publicId, timestamps and clients null")
        void shouldMapRequestIgnoringIdentities() {
            SystemRequest request = new SystemRequest("Billing");

            Systems system = systemMapper.toSystem(request);

            assertThat(system).isNotNull();
            assertThat(system.getName()).isEqualTo("Billing");
            assertThat(system.getId()).isNull();
            assertThat(system.getPublicId()).isNull();
            assertThat(system.getCreatedAt()).isNull();
            assertThat(system.getUpdatedAt()).isNull();
            assertThat(system.getClients()).isNull();
        }
    }

    @Nested
    @DisplayName("Method: toSystemResponse()")
    class ToSystemResponseTests {

        @Test
        @DisplayName("Should expose only publicId and name")
        void shouldMapSystemToResponse() {
            Systems system = SystemTestBuilder.aSystem()
                    .withName("CRM")
                    .build();

            SystemResponse response = systemMapper.toSystemResponse(system);

            assertThat(response.publicId()).isEqualTo(system.getPublicId());
            assertThat(response.name()).isEqualTo("CRM");
        }

        @Test
        @DisplayName("Should return null when the system is null")
        void shouldReturnNullForNullSystem() {
            assertThat(systemMapper.toSystemResponse(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Method: updateSystem()")
    class UpdateSystemTests {

        @Test
        @DisplayName("Should overwrite the name and preserve id, publicId, timestamps and clients")
        void shouldMutateOnlyName() {
            UUID originalPublicId = UUID.randomUUID();
            LocalDateTime originalCreatedAt = LocalDateTime.of(2026, 4, 20, 10, 0);
            LocalDateTime originalUpdatedAt = LocalDateTime.of(2026, 4, 21, 10, 0);

            Systems target = Systems.builder()
                    .id(99L)
                    .publicId(originalPublicId)
                    .name("Old name")
                    .clients(List.of())
                    .isEnabled(true)
                    .createdAt(originalCreatedAt)
                    .updatedAt(originalUpdatedAt)
                    .build();

            systemMapper.updateSystem(new SystemRequest("New name"), target);

            assertThat(target.getName()).isEqualTo("New name");
            assertThat(target.getId()).isEqualTo(99L);
            assertThat(target.getPublicId()).isEqualTo(originalPublicId);
            assertThat(target.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(target.getUpdatedAt()).isEqualTo(originalUpdatedAt);
            assertThat(target.getClients()).isEmpty();
        }
    }
}
