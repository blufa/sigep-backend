package gn.departement.sigep.badges.application.service;

import gn.departement.sigep.badges.application.port.out.BadgeCardRenderer;
import gn.departement.sigep.badges.application.port.out.BadgeRepository;
import gn.departement.sigep.badges.domain.EmployeeBadge;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BadgeServiceTest {

    private static final Long EMPLOYEE_ID = 7L;

    private final BadgeRepository badgeRepository = mock(BadgeRepository.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final BadgeCardRenderer renderer = mock(BadgeCardRenderer.class);
    private final TenantSettingsUseCases tenantSettingsUseCases = mock(TenantSettingsUseCases.class);
    private final BadgeService service =
            new BadgeService(badgeRepository, employeeRepository, positionRepository, renderer, tenantSettingsUseCases);

    @BeforeEach
    void setTenant() {
        TenantContext.set(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void getOrIssueReturnsExistingBadgeInsteadOfIssuingANewOne() {
        EmployeeBadge existing = new EmployeeBadge(1L, 1L, EMPLOYEE_ID, "existing-token", true, Instant.now(), null);
        when(badgeRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(existing));

        EmployeeBadge result = service.getOrIssue(EMPLOYEE_ID);

        assertThat(result.qrToken()).isEqualTo("existing-token");
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void getOrIssueCreatesANewBadgeWhenNoneExists() {
        when(badgeRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());
        when(badgeRepository.save(any(EmployeeBadge.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeBadge result = service.getOrIssue(EMPLOYEE_ID);

        assertThat(result.tenantId()).isEqualTo(1L);
        assertThat(result.employeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(result.active()).isTrue();
        assertThat(result.qrToken()).isNotBlank();
    }

    @Test
    void findActiveByQrTokenRejectsUnknownToken() {
        when(badgeRepository.findByQrToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findActiveByQrToken("missing"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findActiveByQrTokenRejectsRevokedBadge() {
        EmployeeBadge revoked = new EmployeeBadge(1L, 1L, EMPLOYEE_ID, "revoked-token", false, Instant.now(), null);
        when(badgeRepository.findByQrToken("revoked-token")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.findActiveByQrToken("revoked-token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("révoqué");
    }
}
