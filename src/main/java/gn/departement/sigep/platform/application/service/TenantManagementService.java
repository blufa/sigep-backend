package gn.departement.sigep.platform.application.service;

import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.platform.application.port.in.TenantManagementUseCases;
import gn.departement.sigep.platform.application.port.out.TenantRepository;
import gn.departement.sigep.platform.domain.Tenant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantManagementService implements TenantManagementUseCases {

    private final TenantRepository repository;

    public TenantManagementService(TenantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Tenant createTenant(CreateTenantCommand command) {
        if (repository.findByCode(command.code()).isPresent()) {
            throw new BusinessRuleException(
                    "TENANT_CODE_TAKEN", "Ce code de service est déjà utilisé", HttpStatus.CONFLICT);
        }
        Tenant tenant = new Tenant(
                null, command.name(), command.code(), true, command.ministryName(), command.serviceName(),
                null, null);
        return repository.save(tenant);
    }

    @Override
    public List<Tenant> listTenants() {
        return repository.findAll();
    }

    @Override
    public Tenant setActive(Long tenantId, boolean active) {
        Tenant tenant = repository.findById(tenantId).orElseThrow(() -> NotFoundException.of("Tenant", tenantId));
        Tenant updated = new Tenant(
                tenant.id(), tenant.name(), tenant.code(), active, tenant.ministryName(), tenant.serviceName(),
                tenant.workStartTime(), tenant.workEndTime());
        return repository.save(updated);
    }
}
