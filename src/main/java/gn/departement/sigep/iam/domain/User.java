package gn.departement.sigep.iam.domain;

import java.util.List;

public record User(
        Long id,
        Long tenantId,
        String username,
        String passwordHash,
        String fullName,
        Role role,
        boolean active,
        List<String> allowedMenus
) {
}
