package gn.departement.sigep.meetings.domain;

import java.time.Instant;
import java.util.List;

public record Meeting(
        Long id,
        Long tenantId,
        String title,
        String description,
        Instant scheduledAt,
        String location,
        List<Long> attendeeEmployeeIds,
        Instant createdAt
) {
}
