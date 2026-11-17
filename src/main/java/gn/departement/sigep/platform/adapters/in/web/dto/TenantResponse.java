package gn.departement.sigep.platform.adapters.in.web.dto;

import gn.departement.sigep.platform.domain.Tenant;

public record TenantResponse(
        Long id, String name, String code, boolean active, String ministryName, String serviceName) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.id(), tenant.name(), tenant.code(), tenant.active(),
                tenant.ministryName(), tenant.serviceName());
    }
}
