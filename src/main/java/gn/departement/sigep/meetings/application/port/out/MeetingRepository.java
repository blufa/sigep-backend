package gn.departement.sigep.meetings.application.port.out;

import gn.departement.sigep.meetings.domain.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MeetingRepository {

    /** Enregistre la réunion ET ses invités en une seule opération (voir {@code Meeting#attendeeEmployeeIds}). */
    Meeting save(Meeting meeting);

    Optional<Meeting> findById(Long id);

    void deleteById(Long id);

    Page<Meeting> findAll(Pageable pageable);
}
