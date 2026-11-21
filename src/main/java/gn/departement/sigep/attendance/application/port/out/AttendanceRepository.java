package gn.departement.sigep.attendance.application.port.out;

import gn.departement.sigep.attendance.domain.AttendancePunch;
import gn.departement.sigep.attendance.domain.AttendancePunchPhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository {

    AttendancePunch save(AttendancePunch punch);

    Optional<AttendancePunch> findLastByEmployeeIdAndScannedAtBetween(Long employeeId, Instant start, Instant end);

    List<AttendancePunch> findAllByScannedAtBetween(Instant start, Instant end);

    Page<AttendancePunch> search(Long employeeId, Instant from, Instant to, Pageable pageable);

    /** Cliché pris par la caméra au moment du scan (preuve anti-fraude) — voir {@link AttendancePunchPhoto}. */
    void savePunchPhoto(Long punchId, byte[] content, String contentType);

    Optional<AttendancePunchPhoto> findPunchPhoto(Long punchId);

    /** Pour la liste d'historique — savoir quels pointages ont une photo, sans charger les binaires. */
    List<Long> findPunchIdsWithPhoto(List<Long> punchIds);

    void setFlagged(Long punchId, boolean flagged);
}
