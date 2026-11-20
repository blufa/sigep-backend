package gn.departement.sigep.attendance.application.port.out;

import gn.departement.sigep.platform.domain.OrganizationLogo;

import java.time.YearMonth;
import java.util.List;

/** Rendu PDF du rapport mensuel de présence — un bilan par employé (jours présents/retards/absences/départs
 * anticipés), pour le point de fin de mois du directeur. */
public interface MonthlyAttendanceReportRenderer {

    byte[] render(
            YearMonth month,
            List<MonthlyReportRow> rows,
            String tenantCode,
            String ministryName,
            String serviceName,
            OrganizationLogo logo);

    record MonthlyReportRow(
            String matricule,
            String fullName,
            String hierarchie,
            String positionTitle,
            int workingDays,
            int presentDays,
            int lateDays,
            int absentDays,
            int earlyDepartureDays,
            double totalHours
    ) {
        /** Taux de présence de l'agent, en pourcentage (0-100). */
        public double attendanceRate() {
            return workingDays == 0 ? 0 : (presentDays * 100.0) / workingDays;
        }
    }
}
