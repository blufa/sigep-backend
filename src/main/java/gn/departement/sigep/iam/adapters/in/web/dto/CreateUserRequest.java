package gn.departement.sigep.iam.adapters.in.web.dto;

import gn.departement.sigep.iam.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "L'identifiant est requis") String username,
        @NotBlank(message = "Le nom complet est requis") String fullName,
        @NotBlank(message = "Le mot de passe est requis")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
        @NotNull(message = "Le rôle est requis") Role role,
        /** Employé à rattacher (ex. depuis la fiche employé — voir "Créer un compte de connexion")
         * — {@code null} pour un compte sans employé associé. */
        Long employeeId
) {
}
