package gn.departement.sigep.attendance.adapters.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record ManualScanRequest(
        @NotNull(message = "L'employé est requis") Long employeeId
) {
}
