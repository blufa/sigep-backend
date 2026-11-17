package gn.departement.sigep.badges.domain;

import java.time.Instant;

public record EmployeeBadge(
        Long id,
        Long tenantId,
        Long employeeId,
        String qrToken,
        boolean active,
        Instant issuedAt,
        Instant printedAt
) {
}
