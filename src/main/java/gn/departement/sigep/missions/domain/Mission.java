package gn.departement.sigep.missions.domain;

import java.time.Instant;
import java.time.LocalDate;

public record Mission(
        Long id,
        Long tenantId,
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        Instant createdAt
) {
}
