package br.com.luizbrand.worklog.system;

import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Conflict.SystemAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.SystemNotFoundException;
import br.com.luizbrand.worklog.support.SystemTestBuilder;
import br.com.luizbrand.worklog.system.dto.SystemRequest;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

    @Mock
    private SystemRepository systemRepository;

    @Mock
    private SystemMapper systemMapper;

    @InjectMocks
    private SystemService systemService;

    private Systems system;
    private SystemResponse systemResponse;

    @BeforeEach
    void setUp() {
        system = SystemTestBuilder.aSystem().withName("Billing").build();
        systemResponse = new SystemResponse(system.getPublicId(), system.getName());
    }

    @Nested
    @DisplayName("Method: findAllSystems()")
    class FindAllSystemsTests {

        @Test
        @DisplayName("Should return the mapped list when systems exist")
        void shouldReturnMappedList() {
            Systems other = SystemTestBuilder.aSystem().withName("CRM").build();
            SystemResponse otherResponse = new SystemResponse(other.getPublicId(), other.getName());

            when(systemRepository.findAll()).thenReturn(List.of(system, other));
            when(systemMapper.toSystemResponse(system)).thenReturn(systemResponse);
            when(systemMapper.toSystemResponse(other)).thenReturn(otherResponse);

            List<SystemResponse> result = systemService.findAllSystems();

            assertThat(result).containsExactly(systemResponse, otherResponse);
        }

        @Test
        @DisplayName("Should return an empty list when there are no systems")
        void shouldReturnEmptyListWhenNoSystems() {
            when(systemRepository.findAll()).thenReturn(List.of());

            List<SystemResponse> result = systemService.findAllSystems();

            assertThat(result).isEmpty();
            verifyNoInteractions(systemMapper);
        }
    }

    @Nested
    @DisplayName("Method: findAllByPublicIds()")
    class FindAllByPublicIdsTests {

        @Test
        @DisplayName("Should return systems when all requested IDs are found")
        void shouldReturnAllFoundSystems() {
            Systems other = SystemTestBuilder.aSystem().withName("CRM").build();
            List<UUID> ids = List.of(system.getPublicId(), other.getPublicId());

            when(systemRepository.findAllByPublicIdIn(ids)).thenReturn(List.of(system, other));

            List<Systems> result = systemService.findAllByPublicIds(ids);

            assertThat(result).containsExactly(system, other);
        }

        @Test
        @DisplayName("Should return an empty list when the input is null")
        void shouldReturnEmptyListForNullInput() {
            List<Systems> result = systemService.findAllByPublicIds(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(systemRepository);
        }

        @Test
        @DisplayName("Should return an empty list when the input is empty")
        void shouldReturnEmptyListForEmptyInput() {
            List<Systems> result = systemService.findAllByPublicIds(List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(systemRepository);
        }

        @Test
        @DisplayName("Should throw SystemNotFoundException when some IDs do not match")
        void shouldThrowWhenSomeIdsMissing() {
            List<UUID> ids = List.of(system.getPublicId(), UUID.randomUUID());
            when(systemRepository.findAllByPublicIdIn(ids)).thenReturn(List.of(system));

            assertThrows(SystemNotFoundException.class,
                    () -> systemService.findAllByPublicIds(ids));
        }
    }

    @Nested
    @DisplayName("Method: getSystemByPublicId()")
    class GetSystemByPublicIdTests {

        @Test
        @DisplayName("Should return the response when the system exists")
        void shouldReturnResponseWhenFound() {
            when(systemRepository.findByPublicId(system.getPublicId())).thenReturn(Optional.of(system));
            when(systemMapper.toSystemResponse(system)).thenReturn(systemResponse);

            SystemResponse result = systemService.getSystemByPublicId(system.getPublicId());

            assertThat(result).isEqualTo(systemResponse);
        }

        @Test
        @DisplayName("Should throw SystemNotFoundException when the system does not exist")
        void shouldThrowWhenNotFound() {
            UUID missing = UUID.randomUUID();
            when(systemRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            SystemNotFoundException ex = assertThrows(SystemNotFoundException.class,
                    () -> systemService.getSystemByPublicId(missing));

            assertThat(ex.getMessage()).contains(missing.toString());
        }
    }

    @Nested
    @DisplayName("Method: createSystem()")
    class CreateSystemTests {

        @Test
        @DisplayName("Should persist and return the response when the name is unique")
        void shouldCreateSystemWhenNameUnique() {
            SystemRequest request = new SystemRequest("Billing");

            when(systemRepository.findByName(request.name())).thenReturn(Optional.empty());
            when(systemMapper.toSystem(request)).thenReturn(system);
            when(systemRepository.save(system)).thenReturn(system);
            when(systemMapper.toSystemResponse(system)).thenReturn(systemResponse);

            SystemResponse result = systemService.createSystem(request);

            assertThat(result).isEqualTo(systemResponse);
            verify(systemRepository, times(1)).save(system);
        }

        @Test
        @DisplayName("Should throw SystemAlreadyExistsException when the name already exists")
        void shouldThrowWhenNameExists() {
            SystemRequest request = new SystemRequest("Billing");
            when(systemRepository.findByName(request.name())).thenReturn(Optional.of(system));

            assertThrows(SystemAlreadyExistsException.class,
                    () -> systemService.createSystem(request));

            verify(systemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Method: updateSystem()")
    class UpdateSystemTests {

        @Test
        @DisplayName("Should delegate to the mapper and persist when the system exists")
        void shouldUpdateSystemWhenFound() {
            SystemRequest request = new SystemRequest("Billing v2");
            when(systemRepository.findByPublicId(system.getPublicId())).thenReturn(Optional.of(system));
            when(systemRepository.save(system)).thenReturn(system);
            when(systemMapper.toSystemResponse(system)).thenReturn(systemResponse);

            SystemResponse result = systemService.updateSystem(request, system.getPublicId());

            assertThat(result).isEqualTo(systemResponse);
            verify(systemMapper, times(1)).updateSystem(request, system);
            verify(systemRepository, times(1)).save(system);
        }

        @Test
        @DisplayName("Should throw SystemNotFoundException when the system does not exist")
        void shouldThrowWhenUpdatingMissingSystem() {
            UUID missing = UUID.randomUUID();
            SystemRequest request = new SystemRequest("Billing v2");
            when(systemRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            assertThrows(SystemNotFoundException.class,
                    () -> systemService.updateSystem(request, missing));

            verify(systemRepository, never()).save(any());
            verifyNoInteractions(systemMapper);
        }
    }

    @Nested
    @DisplayName("Method: findByPublicId()")
    class FindByPublicIdTests {

        @Test
        @DisplayName("Should return the entity when the system exists")
        void shouldReturnEntityWhenFound() {
            when(systemRepository.findByPublicId(system.getPublicId())).thenReturn(Optional.of(system));

            Systems result = systemService.findByPublicId(system.getPublicId());

            assertThat(result).isEqualTo(system);
        }

        @Test
        @DisplayName("Should throw SystemNotFoundException when the system does not exist")
        void shouldThrowWhenNotFound() {
            UUID missing = UUID.randomUUID();
            when(systemRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            assertThrows(SystemNotFoundException.class,
                    () -> systemService.findByPublicId(missing));
        }
    }

    @Nested
    @DisplayName("Method: findActiveSystem()")
    class FindActiveSystemTests {

        @Test
        @DisplayName("Should return the entity when it is enabled")
        void shouldReturnActiveSystem() {
            when(systemRepository.findByPublicId(system.getPublicId())).thenReturn(Optional.of(system));

            Systems result = systemService.findActiveSystem(system.getPublicId());

            assertThat(result).isEqualTo(system);
        }

        @Test
        @DisplayName("Should throw BusinessException when the system is disabled")
        void shouldThrowWhenDisabled() {
            Systems disabled = SystemTestBuilder.aSystem().disabled().build();
            when(systemRepository.findByPublicId(disabled.getPublicId())).thenReturn(Optional.of(disabled));

            assertThrows(BusinessException.class,
                    () -> systemService.findActiveSystem(disabled.getPublicId()));
        }

        @Test
        @DisplayName("Should propagate SystemNotFoundException when the system does not exist")
        void shouldPropagateNotFound() {
            UUID missing = UUID.randomUUID();
            when(systemRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            assertThrows(SystemNotFoundException.class,
                    () -> systemService.findActiveSystem(missing));
        }
    }
}
