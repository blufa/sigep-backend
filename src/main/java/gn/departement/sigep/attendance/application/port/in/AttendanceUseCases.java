package gn.departement.sigep.attendance.application.port.in;

import gn.departement.sigep.attendance.domain.AttendancePunchPhoto;
import gn.departement.sigep.attendance.domain.Direction;
import gn.departement.sigep.common.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;

public interface AttendanceUseCases {

    /**
     * @param photo cliché pris par la caméra au moment du scan (preuve anti-fraude, voir
     *              {@link AttendancePunchPhoto}) — {@code null} si le pointage vient de la saisie
     *              manuelle du jeton (pas d'image caméra disponible dans ce cas).
     */
    ScanResult scan(String qrToken, String source, byte[] photo, String photoContentType);

    /** Pointage manuel par recherche d'employé (recherche nom/matricule côté front) — pas de QR à
     * scanner, utilisé quand la caméra est indisponible ou pour un pointage de secours. */
    ScanResult scanManual(Long employeeId, String source);

    PageResponse<AttendanceEntry> history(Long employeeId, Instant from, Instant to, Pageable pageable);

    AttendanceSummary summary();

    /** Rapport mensuel de présence (bilan de fin de mois) en PDF, un employé actif par ligne. */
    byte[] generateMonthlyReportPdf(YearMonth month);

    /** Photo prise au moment d'un pointage donné, pour la revue à distance par le directeur. */
    Optional<AttendancePunchPhoto> getPunchPhoto(Long punchId);

    record ScanResult(
            Long employeeId,
            String matricule,
            String fullName,
            Direction direction,
            Instant scannedAt
    ) {
    }

    record AttendanceEntry(
            Long id,
            Long employeeId,
            String matricule,
            String fullName,
            Direction direction,
            Instant scannedAt,
            String source,
            boolean hasPhoto
    ) {
    }

    record AttendanceSummary(
            long presentToday,
            long totalActiveEmployees,
            long onTimeToday,
            long lateToday,
            long absentToday,
            long earlyDepartureToday
    ) {
    }
}
