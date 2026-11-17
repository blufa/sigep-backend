package gn.departement.sigep.platform.domain;

import java.time.LocalTime;

public record Tenant(
        Long id,
        String name,
        String code,
        boolean active,
        String ministryName,
        String serviceName,
        LocalTime workStartTime,
        LocalTime workEndTime
) {
}
