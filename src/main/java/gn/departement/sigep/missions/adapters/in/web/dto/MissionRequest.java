package gn.departement.sigep.missions.adapters.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MissionRequest(
        @NotNull(message = "L'employé est requis") Long employeeId,
        @NotNull(message = "La date de début est requise") LocalDate startDate,
        @NotNull(message = "La date de fin est requise") LocalDate endDate,
        String reason
) {
}
