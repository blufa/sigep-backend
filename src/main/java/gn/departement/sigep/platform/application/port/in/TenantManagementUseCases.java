package gn.departement.sigep.platform.application.port.in;

import gn.departement.sigep.platform.domain.Tenant;

import java.util.List;

/** Réservé au SUPER_ADMIN : création et administration des services (tenants) de la plateforme. */
public interface TenantManagementUseCases {

    Tenant createTenant(CreateTenantCommand command);

    List<Tenant> listTenants();

    Tenant setActive(Long tenantId, boolean active);

    record CreateTenantCommand(String name, String code, String ministryName, String serviceName) {
    }
}
