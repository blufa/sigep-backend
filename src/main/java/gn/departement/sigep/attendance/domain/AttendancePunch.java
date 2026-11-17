package gn.departement.sigep.attendance.domain;

import java.time.Instant;

public record AttendancePunch(
        Long id,
        Long tenantId,
        Long employeeId,
        Long badgeId,
        Direction direction,
        Instant scannedAt,
        String source
) {
}
