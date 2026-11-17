package gn.departement.sigep.staff.application.service;

import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.staff.application.port.in.EmployeeUseCases.CreateEmployeeCommand;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.application.port.out.EmployeeReportRenderer;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.Genre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeServiceTest {

    private final EmployeeRepository repository = mock(EmployeeRepository.class);
    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final TenantSettingsUseCases tenantSettingsUseCases = mock(TenantSettingsUseCases.class);
    private final EmployeeReportRenderer reportRenderer = mock(EmployeeReportRenderer.class);
    private final EmployeeService service =
            new EmployeeService(repository, positionRepository, tenantSettingsUseCases, reportRenderer);

    @BeforeEach
    void setTenant() {
        TenantContext.set(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void createRejectsDuplicateMatricule() {
        when(repository.existsByMatricule("ETAT-00123")).thenReturn(true);

        CreateEmployeeCommand command = new CreateEmployeeCommand(
                "ETAT-00123", "Camara", "Mamadou", "Agent de maîtrise", Genre.HOMME, null, "620000000", "Conakry");

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("matricule");
    }

    @Test
    void createStampsCurrentTenantAndActivatesEmployee() {
        when(repository.existsByMatricule("ETAT-00124")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEmployeeCommand command = new CreateEmployeeCommand(
                "ETAT-00124", "Diallo", "Fatoumata", null, Genre.FEMME, 42L, "621111111", "Kindia");

        Employee created = service.create(command);

        assertThat(created.tenantId()).isEqualTo(1L);
        assertThat(created.active()).isTrue();
        assertThat(created.matricule()).isEqualTo("ETAT-00124");
        verify(repository).save(any(Employee.class));
    }
}
