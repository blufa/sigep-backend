package gn.departement.sigep.meetings.domain;

import java.time.Instant;
import java.util.List;

public record Meeting(
        Long id,
        Long tenantId,
        String title,
        String description,
        Instant scheduledAt,
        String location,
        List<Long> attendeeEmployeeIds,
        /** Heure d'envoi du SMS de convocation — {@code null} pour un envoi immédiat à la création
         * (comportement historique). Si renseigné dans le futur, {@code MeetingSmsDispatchScheduler}
         * s'en charge au bon moment plutôt que {@code MeetingService#create}. */
        Instant notifyAt,
        boolean notified,
        Instant createdAt
) {
}
