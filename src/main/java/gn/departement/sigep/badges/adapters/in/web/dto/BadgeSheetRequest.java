package gn.departement.sigep.badges.adapters.in.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BadgeSheetRequest(@NotEmpty(message = "Sélectionnez au moins un employé") List<Long> employeeIds) {
}
