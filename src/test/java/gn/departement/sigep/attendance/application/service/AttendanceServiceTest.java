package gn.departement.sigep.attendance.application.service;

import gn.departement.sigep.attendance.application.port.out.AttendanceRepository;
import gn.departement.sigep.attendance.domain.AttendancePunch;
import gn.departement.sigep.attendance.domain.Direction;
import gn.departement.sigep.badges.application.port.in.BadgeUseCases;
import gn.departement.sigep.badges.domain.EmployeeBadge;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.attendance.application.port.out.MonthlyAttendanceReportRenderer;
import gn.departement.sigep.missions.application.port.in.MissionUseCases;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.Genre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttendanceServiceTest {

    private static final Long EMPLOYEE_ID = 7L;
    private static final String QR_TOKEN = "secret-token";

    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final BadgeUseCases badgeUseCases = mock(BadgeUseCases.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final TenantSettingsUseCases tenantSettingsUseCases = mock(TenantSettingsUseCases.class);
    private final MonthlyAttendanceReportRenderer reportRenderer = mock(MonthlyAttendanceReportRenderer.class);
    private final MissionUseCases missionUseCases = mock(MissionUseCases.class);
    private final AttendanceService service = new AttendanceService(
            attendanceRepository, badgeUseCases, employeeRepository, positionRepository,
            tenantSettingsUseCases, reportRenderer, missionUseCases);

    @BeforeEach
    void setTenant() {
        TenantContext.set(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void firstScanOfTheDayIsEntree() {
        mockActiveEmployeeWithBadge();
        when(attendanceRepository.findLastByEmployeeIdAndScannedAtBetween(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(AttendancePunch.class))).thenAnswer(i -> i.getArgument(0));

        AttendanceService.ScanResult result = service.scan(QR_TOKEN, "QR", null, null);

        assertThat(result.direction()).isEqualTo(Direction.ENTREE);
        assertThat(result.employeeId()).isEqualTo(EMPLOYEE_ID);
    }

    @Test
    void secondScanOfTheDayIsSortie() {
        mockActiveEmployeeWithBadge();
        AttendancePunch previousEntree = new AttendancePunch(
                1L, 1L, EMPLOYEE_ID, 1L, Direction.ENTREE, Instant.now().minusSeconds(3600), "QR", false);
        when(attendanceRepository.findLastByEmployeeIdAndScannedAtBetween(any(), any(), any()))
                .thenReturn(Optional.of(previousEntree));
        when(attendanceRepository.save(any(AttendancePunch.class))).thenAnswer(i -> i.getArgument(0));

        AttendanceService.ScanResult result = service.scan(QR_TOKEN, "QR", null, null);

        assertThat(result.direction()).isEqualTo(Direction.SORTIE);
    }

    @Test
    void scanRejectsInactiveEmployee() {
        EmployeeBadge badge = new EmployeeBadge(1L, 1L, EMPLOYEE_ID, QR_TOKEN, true, Instant.now(), null);
        when(badgeUseCases.findActiveByQrToken(QR_TOKEN)).thenReturn(badge);

        Employee inactiveEmployee = new Employee(
                EMPLOYEE_ID, 1L, "ETAT-00123", "Camara", "Mamadou", null, Genre.HOMME, null, null, null,
                false, Instant.now(), Instant.now());
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(inactiveEmployee));

        assertThatThrownBy(() -> service.scan(QR_TOKEN, "QR", null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("activité");
    }

    private void mockActiveEmployeeWithBadge() {
        EmployeeBadge badge = new EmployeeBadge(1L, 1L, EMPLOYEE_ID, QR_TOKEN, true, Instant.now(), null);
        when(badgeUseCases.findActiveByQrToken(QR_TOKEN)).thenReturn(badge);

        Employee employee = new Employee(
                EMPLOYEE_ID, 1L, "ETAT-00123", "Camara", "Mamadou", "Agent de maîtrise", Genre.HOMME, null,
                "620000000", "Conakry", true, Instant.now(), Instant.now());
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
    }
}
