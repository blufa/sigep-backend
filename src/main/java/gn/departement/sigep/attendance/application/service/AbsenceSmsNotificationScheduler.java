package gn.departement.sigep.attendance.application.service;

import gn.departement.sigep.attendance.application.port.in.AttendanceUseCases;
import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.communication.application.port.in.SmsUseCases;
import gn.departement.sigep.platform.application.port.out.TenantRepository;
import gn.departement.sigep.platform.domain.Tenant;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.domain.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Filet de sécurité quotidien (§demande client : "si l'employé n'a pas scanné jusqu'à 12h00, le
 * système le notifie par sms") : à midi, pour chaque tenant actif, notifie par SMS chaque employé
 * absent (aucun pointage, hors employés en mission — voir {@code AttendanceService.dailyStatus}).
 * Seul l'intéressé est notifié (pas le directeur/les comptes ADMIN).
 *
 * <p>S'exécute hors contexte de requête : boucle explicitement par tenant en restaurant
 * {@link TenantContext}, comme le filtre Hibernate {@code tenantFilter} dépend de ce contexte pour
 * chaque appel repository.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AbsenceSmsNotificationScheduler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantRepository tenantRepository;
    private final AttendanceUseCases attendanceUseCases;
    private final EmployeeRepository employeeRepository;
    private final SmsUseCases smsUseCases;

    @Scheduled(cron = "0 0 12 * * *")
    public void notifyTodaysAbsences() {
        LocalDate today = LocalDate.now();
        for (Tenant tenant : tenantRepository.findAll()) {
            if (!tenant.active()) {
                continue;
            }
            TenantContext.set(tenant.id());
            try {
                notifyAbsencesForTenant(today);
            } catch (Exception e) {
                log.error("Notification SMS d'absence en échec pour tenant {} : {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void notifyAbsencesForTenant(LocalDate today) {
        PageResponse<AttendanceUseCases.DailyStatusEntry> page = attendanceUseCases.dailyStatus(
                today, AttendanceUseCases.DailyStatus.ABSENT, PageRequest.of(0, Integer.MAX_VALUE));
        List<AttendanceUseCases.DailyStatusEntry> absentees = page.items();

        for (AttendanceUseCases.DailyStatusEntry absentee : absentees) {
            Employee employee = employeeRepository.findById(absentee.employeeId()).orElse(null);
            if (employee != null && employee.phone() != null && !employee.phone().isBlank()) {
                smsUseCases.sendSms(employee.phone(), "SIGAP : vous êtes marqué(e) absent(e) aujourd'hui "
                        + today.format(DATE_FMT) + ". Contactez votre service RH en cas d'erreur.");
            }
        }
    }
}
