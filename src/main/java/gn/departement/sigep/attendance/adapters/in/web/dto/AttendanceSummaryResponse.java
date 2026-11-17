package gn.departement.sigep.attendance.adapters.in.web.dto;

import gn.departement.sigep.attendance.application.port.in.AttendanceUseCases;

public record AttendanceSummaryResponse(
        long presentToday,
        long totalActiveEmployees,
        long onTimeToday,
        long lateToday,
        long absentToday,
        long earlyDepartureToday
) {

    public static AttendanceSummaryResponse from(AttendanceUseCases.AttendanceSummary summary) {
        return new AttendanceSummaryResponse(
                summary.presentToday(),
                summary.totalActiveEmployees(),
                summary.onTimeToday(),
                summary.lateToday(),
                summary.absentToday(),
                summary.earlyDepartureToday()
        );
    }
}
