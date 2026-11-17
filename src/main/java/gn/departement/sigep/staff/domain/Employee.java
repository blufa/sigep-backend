package gn.departement.sigep.staff.domain;

import java.time.Instant;

public record Employee(
        Long id,
        Long tenantId,
        String matricule,
        String lastName,
        String firstName,
        String hierarchie,
        Genre genre,
        Long positionId,
        String phone,
        String city,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
