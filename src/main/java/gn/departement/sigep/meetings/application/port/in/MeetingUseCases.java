package gn.departement.sigep.meetings.application.port.in;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.meetings.domain.Meeting;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface MeetingUseCases {

    /** Crée la réunion. Envoie le SMS de convocation immédiatement si {@code notifyAt} est {@code null}
     * ou déjà passé ; sinon le diffère à cette heure (voir {@link #dispatchDueNotifications}). */
    Meeting create(CreateMeetingCommand command);

    void cancel(Long id);

    PageResponse<MeetingView> list(Pageable pageable);

    /** Envoie les convocations SMS de toutes les réunions programmées dont l'heure est passée —
     * appelé chaque minute par {@code MeetingSmsDispatchScheduler}, boucle déjà faite par tenant. */
    void dispatchDueNotifications(Instant now);

    record CreateMeetingCommand(
            String title, String description, Instant scheduledAt, String location, List<Long> attendeeEmployeeIds,
            /** {@code null} = envoi immédiat du SMS de convocation à la création. */
            Instant notifyAt) {
    }

    /** Vue enrichie (noms des invités) pour l'affichage — voir le même choix pour
     * {@code MissionUseCases.MissionView}. */
    record MeetingView(
            Long id,
            String title,
            String description,
            Instant scheduledAt,
            String location,
            List<AttendeeView> attendees,
            Instant notifyAt,
            boolean notified,
            Instant createdAt
    ) {
    }

    record AttendeeView(Long employeeId, String matricule, String fullName) {
    }
}
