package gn.departement.sigep.attendance.adapters.in.web.dto;

import gn.departement.sigep.attendance.application.port.in.AttendanceUseCases;
import gn.departement.sigep.attendance.domain.Direction;

import java.time.Instant;

public record AttendanceEntryResponse(
        Long id,
        Long employeeId,
        String matricule,
        String fullName,
        Direction direction,
        Instant scannedAt,
        String source,
        boolean hasPhoto
) {

    public static AttendanceEntryResponse from(AttendanceUseCases.AttendanceEntry entry) {
        return new AttendanceEntryResponse(
                entry.id(), entry.employeeId(), entry.matricule(), entry.fullName(),
                entry.direction(), entry.scannedAt(), entry.source(), entry.hasPhoto());
    }
}
