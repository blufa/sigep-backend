package gn.departement.sigep.platform.application.service;

import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.platform.application.port.in.TenantManagementUseCases.CreateTenantCommand;
import gn.departement.sigep.platform.application.port.out.TenantRepository;
import gn.departement.sigep.platform.domain.Tenant;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantManagementServiceTest {

    private final TenantRepository repository = mock(TenantRepository.class);
    private final TenantManagementService service = new TenantManagementService(repository);

    @Test
    void createTenantRejectsDuplicateCode() {
        when(repository.findByCode("SNIESM")).thenReturn(Optional.of(
                new Tenant(1L, "SNIESM", "SNIESM", true, null, null, null, null)));

        assertThatThrownBy(() -> service.createTenant(new CreateTenantCommand("Autre", "SNIESM", null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createTenantSavesActiveTenantWithNullId() {
        when(repository.findByCode("DNFP")).thenReturn(Optional.empty());
        when(repository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant created = service.createTenant(new CreateTenantCommand(
                "Direction Nationale Formation Professionnelle", "DNFP", "MENET-FP", "DNFP"));

        assertThat(created.id()).isNull();
        assertThat(created.code()).isEqualTo("DNFP");
        assertThat(created.active()).isTrue();
    }

    @Test
    void setActiveTogglesExistingTenant() {
        Tenant existing = new Tenant(2L, "DNFP", "DNFP", true, "MENET-FP", "DNFP", null, null);
        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant updated = service.setActive(2L, false);

        assertThat(updated.active()).isFalse();
        assertThat(updated.code()).isEqualTo("DNFP");
    }
}
