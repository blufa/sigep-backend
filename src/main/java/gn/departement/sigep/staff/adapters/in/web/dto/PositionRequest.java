package gn.departement.sigep.staff.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PositionRequest(
        @NotBlank(message = "Le titre du poste est requis") String title,
        @NotBlank(message = "Le code du poste est requis") String code,
        boolean active
) {
}
