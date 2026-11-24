package gn.departement.sigep.iam.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUsernameRequest(
        @NotBlank(message = "L'identifiant est requis") String username
) {
}
