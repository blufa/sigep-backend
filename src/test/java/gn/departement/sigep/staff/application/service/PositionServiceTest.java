package gn.departement.sigep.staff.application.service;

import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.staff.application.port.in.PositionUseCases.CreatePositionCommand;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PositionServiceTest {

    private final PositionRepository repository = mock(PositionRepository.class);
    private final PositionService service = new PositionService(repository);

    @BeforeEach
    void setTenant() {
        TenantContext.set(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.existsByCode("AGT-MAINT")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreatePositionCommand("Agent de maintenance", "AGT-MAINT")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createStampsCurrentTenantAndDefaultsToActive() {
        when(repository.existsByCode("AGT-MAINT")).thenReturn(false);
        when(repository.save(any(Position.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Position created = service.create(new CreatePositionCommand("Agent de maintenance", "AGT-MAINT"));

        assertThat(created.tenantId()).isEqualTo(1L);
        assertThat(created.active()).isTrue();
    }
}
