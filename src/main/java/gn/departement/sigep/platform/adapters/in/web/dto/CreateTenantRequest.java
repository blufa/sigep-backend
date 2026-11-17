package gn.departement.sigep.platform.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
        @NotBlank(message = "Le nom du service est requis") String name,
        @NotBlank(message = "Le code du service est requis") String code,
        String ministryName,
        String serviceName
) {
}
