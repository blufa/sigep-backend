package gn.departement.sigep.platform.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalTime;

public record UpdateOrganizationRequest(
        @NotBlank(message = "Le nom du ministère est requis") String ministryName,
        @NotBlank(message = "Le nom du service est requis") String serviceName,
        LocalTime workStartTime,
        LocalTime workEndTime
) {
}
