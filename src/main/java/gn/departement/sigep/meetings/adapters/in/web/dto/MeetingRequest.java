package gn.departement.sigep.meetings.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record MeetingRequest(
        @NotBlank(message = "Le titre est requis") String title,
        String description,
        @NotNull(message = "La date/heure est requise") Instant scheduledAt,
        String location,
        @NotEmpty(message = "Au moins un invité est requis") List<Long> attendeeEmployeeIds
) {
}
