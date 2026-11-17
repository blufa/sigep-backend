package gn.departement.sigep.iam.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String newPassword
) {
}
