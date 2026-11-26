package gn.departement.sigep.meetings.application.port.in;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.meetings.domain.Meeting;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface MeetingUseCases {

    /** Crée la réunion et envoie immédiatement un SMS de convocation à chaque invité (voir
     * {@code gn.departement.sigep.communication}). */
    Meeting create(CreateMeetingCommand command);

    void cancel(Long id);

    PageResponse<MeetingView> list(Pageable pageable);

    record CreateMeetingCommand(
            String title, String description, Instant scheduledAt, String location, List<Long> attendeeEmployeeIds) {
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
            Instant createdAt
    ) {
    }

    record AttendeeView(Long employeeId, String matricule, String fullName) {
    }
}
