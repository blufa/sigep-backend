package gn.departement.sigep.platform.adapters.out.persistence;

import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.platform.application.port.out.TenantRepository;
import gn.departement.sigep.platform.domain.OrganizationLogo;
import gn.departement.sigep.platform.domain.Tenant;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class TenantPersistenceAdapter implements TenantRepository {

    private final TenantJpaRepository jpaRepository;

    TenantPersistenceAdapter(TenantJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Tenant> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public Optional<Tenant> findFirst() {
        return jpaRepository.findAll(PageRequest.of(0, 1)).stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<Tenant> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantJpaEntity entity = tenant.id() != null
                ? jpaRepository.findById(tenant.id()).orElseThrow(() -> NotFoundException.of("Tenant", tenant.id()))
                : new TenantJpaEntity();
        entity.setName(tenant.name());
        entity.setCode(tenant.code());
        entity.setActive(tenant.active());
        entity.setMinistryName(tenant.ministryName());
        entity.setServiceName(tenant.serviceName());
        entity.setWorkStartTime(tenant.workStartTime());
        entity.setWorkEndTime(tenant.workEndTime());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void saveLogo(Long tenantId, byte[] content, String contentType) {
        TenantJpaEntity entity = jpaRepository.findById(tenantId)
                .orElseThrow(() -> NotFoundException.of("Tenant", tenantId));
        entity.setLogo(content);
        entity.setLogoContentType(contentType);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<OrganizationLogo> findLogo(Long tenantId) {
        return jpaRepository.findById(tenantId)
                .filter(entity -> entity.getLogo() != null)
                .map(entity -> new OrganizationLogo(entity.getLogo(), entity.getLogoContentType()));
    }

    private Tenant toDomain(TenantJpaEntity entity) {
        return new Tenant(
                entity.getId(), entity.getName(), entity.getCode(), Boolean.TRUE.equals(entity.getActive()),
                entity.getMinistryName(), entity.getServiceName(),
                entity.getWorkStartTime(), entity.getWorkEndTime());
    }
}
