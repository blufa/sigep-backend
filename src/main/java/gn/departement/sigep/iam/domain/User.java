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
        List<String> allowedMenus,
        /** Employé lié (compte créé depuis la fiche employé — voir "Créer un compte de connexion")
         * — {@code null} pour un compte créé directement (ex. le compte administrateur initial). */
        Long employeeId
) {
}
