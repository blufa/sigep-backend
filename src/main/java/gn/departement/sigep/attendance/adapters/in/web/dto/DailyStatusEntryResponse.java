package gn.departement.sigep.attendance.adapters.in.web.dto;

import gn.departement.sigep.attendance.application.port.in.AttendanceUseCases;

import java.time.Instant;

public record DailyStatusEntryResponse(
        Long employeeId,
        String matricule,
        String fullName,
        String positionTitle,
        AttendanceUseCases.DailyStatus status,
        Instant firstEntree,
        Instant lastSortie
) {

    public static DailyStatusEntryResponse from(AttendanceUseCases.DailyStatusEntry entry) {
        return new DailyStatusEntryResponse(
                entry.employeeId(), entry.matricule(), entry.fullName(), entry.positionTitle(),
                entry.status(), entry.firstEntree(), entry.lastSortie());
    }
}
