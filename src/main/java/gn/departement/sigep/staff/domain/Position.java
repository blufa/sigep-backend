package gn.departement.sigep.staff.domain;

public record Position(
        Long id,
        Long tenantId,
        String title,
        String code,
        boolean active
) {
}
