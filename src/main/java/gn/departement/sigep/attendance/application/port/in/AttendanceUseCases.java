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

    /** Même rapport que {@link #generateMonthlyReportPdf(YearMonth)}, limité à un seul employé —
     * accessible depuis la liste des pointages filtrée sur cet employé. */
    byte[] generateEmployeeMonthlyReportPdf(Long employeeId, YearMonth month);

    /** Photo prise au moment d'un pointage donné, pour la revue à distance par le directeur. */
    Optional<AttendancePunchPhoto> getPunchPhoto(Long punchId);

    /** Signale (ou retire le signalement d') un pointage comme suspect — voir {@code flagged} sur
     * {@link ScanResult}/{@link AttendanceEntry} : le vigile déclenche ça depuis l'écran de scan
     * quand la photo caméra ne correspond visiblement pas à la photo officielle de l'employé. */
    void flagPunch(Long punchId, boolean flagged);

    record ScanResult(
            Long id,
            Long employeeId,
            String matricule,
            String fullName,
            Direction direction,
            Instant scannedAt,
            boolean flagged
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
            boolean hasPhoto,
            boolean flagged
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
