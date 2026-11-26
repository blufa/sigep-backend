package gn.departement.sigep.meetings.application.port.out;

import gn.departement.sigep.meetings.domain.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository {

    /** Enregistre la réunion ET ses invités en une seule opération (voir {@code Meeting#attendeeEmployeeIds}). */
    Meeting save(Meeting meeting);

    Optional<Meeting> findById(Long id);

    void deleteById(Long id);

    Page<Meeting> findAll(Pageable pageable);

    /** Convocations programmées dont l'heure d'envoi est passée et pas encore notifiées — utilisé
     * par {@code MeetingSmsDispatchScheduler}. */
    List<Meeting> findDueUnnotified(Instant now);
}
