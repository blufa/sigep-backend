package gn.departement.sigep.platform.adapters.in.web.dto;

import gn.departement.sigep.platform.domain.Tenant;

import java.time.LocalTime;

public record OrganizationResponse(
        String ministryName, String serviceName, LocalTime workStartTime, LocalTime workEndTime, boolean hasLogo) {

    public static OrganizationResponse from(Tenant tenant, boolean hasLogo) {
        return new OrganizationResponse(
                tenant.ministryName(), tenant.serviceName(), tenant.workStartTime(), tenant.workEndTime(), hasLogo);
    }
}
