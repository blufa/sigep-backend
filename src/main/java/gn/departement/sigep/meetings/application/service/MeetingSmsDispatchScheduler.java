package gn.departement.sigep.meetings.application.service;

import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.meetings.application.port.in.MeetingUseCases;
import gn.departement.sigep.platform.application.port.out.TenantRepository;
import gn.departement.sigep.platform.domain.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Déclenche les convocations SMS de réunion programmées ({@code Meeting#notifyAt}) dont l'heure est
 * passée — même patron que {@code ScheduledBroadcastDispatchScheduler}/{@code
 * AbsenceSmsNotificationScheduler} : s'exécute hors contexte de requête, boucle par tenant en
 * restaurant {@link TenantContext}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingSmsDispatchScheduler {

    private final TenantRepository tenantRepository;
    private final MeetingUseCases meetingUseCases;

    @Scheduled(cron = "0 * * * * *") // chaque minute : précision attendue par qui programme "à cette heure"
    public void dispatchDueMeetingNotifications() {
        Instant now = Instant.now();
        for (Tenant tenant : tenantRepository.findAll()) {
            if (!tenant.active()) {
                continue;
            }
            TenantContext.set(tenant.id());
            try {
                meetingUseCases.dispatchDueNotifications(now);
            } catch (Exception e) {
                log.error("Dispatch des convocations de réunion en échec pour tenant {} : {}", tenant.id(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
