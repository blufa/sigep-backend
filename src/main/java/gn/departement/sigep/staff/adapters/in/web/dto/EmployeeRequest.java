package gn.departement.sigep.staff.adapters.in.web.dto;

import gn.departement.sigep.staff.domain.Genre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmployeeRequest(
        @NotBlank(message = "Le matricule d'Etat est requis") String matricule,
        @NotBlank(message = "Le nom est requis") String lastName,
        @NotBlank(message = "Le prénom est requis") String firstName,
        String hierarchie,
        @NotNull(message = "Le genre est requis") Genre genre,
        Long positionId,
        String phone,
        String city,
        boolean active
) {
}
