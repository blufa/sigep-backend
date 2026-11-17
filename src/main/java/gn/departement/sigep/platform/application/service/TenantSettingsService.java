package gn.departement.sigep.platform.application.service;

import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.platform.application.port.out.TenantRepository;
import gn.departement.sigep.platform.domain.OrganizationLogo;
import gn.departement.sigep.platform.domain.Tenant;
import org.springframework.stereotype.Service;

@Service
public class TenantSettingsService implements TenantSettingsUseCases {

    private final TenantRepository repository;

    public TenantSettingsService(TenantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Tenant getCurrent() {
        return getOrThrow(currentTenantId());
    }

    @Override
    public Tenant updateCurrent(UpdateOrganizationCommand command) {
        Tenant current = getCurrent();
        Tenant updated = new Tenant(
                current.id(), current.name(), current.code(), current.active(),
                command.ministryName(), command.serviceName(), command.workStartTime(), command.workEndTime());
        return repository.save(updated);
    }

    @Override
    public void updateLogo(byte[] content, String contentType) {
        repository.saveLogo(currentTenantId(), content, contentType);
    }

    @Override
    public OrganizationLogo getLogo(Long tenantId) {
        return repository.findLogo(tenantId).orElse(null);
    }

    @Override
    public Tenant getPublic() {
        return repository.findFirst().orElseThrow(() -> new NotFoundException("Aucune organisation configurée"));
    }

    @Override
    public OrganizationLogo getPublicLogo() {
        return repository.findFirst().flatMap(t -> repository.findLogo(t.id())).orElse(null);
    }

    private Tenant getOrThrow(Long tenantId) {
        return repository.findById(tenantId).orElseThrow(() -> NotFoundException.of("Tenant", tenantId));
    }

    private Long currentTenantId() {
        return TenantContext.get();
    }
}
