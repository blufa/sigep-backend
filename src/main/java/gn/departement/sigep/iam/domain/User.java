package gn.departement.sigep.iam.domain;

public record User(
        Long id,
        Long tenantId,
        String username,
        String passwordHash,
        String fullName,
        Role role,
        boolean active
) {
}
