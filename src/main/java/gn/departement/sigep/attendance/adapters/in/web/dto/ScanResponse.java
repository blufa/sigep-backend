package gn.departement.sigep.attendance.adapters.in.web.dto;

import gn.departement.sigep.attendance.application.port.in.AttendanceUseCases;
import gn.departement.sigep.attendance.domain.Direction;

import java.time.Instant;

public record ScanResponse(
        Long employeeId,
        String matricule,
        String fullName,
        Direction direction,
        Instant scannedAt
) {

    public static ScanResponse from(AttendanceUseCases.ScanResult result) {
        return new ScanResponse(
                result.employeeId(), result.matricule(), result.fullName(), result.direction(), result.scannedAt());
    }
}
