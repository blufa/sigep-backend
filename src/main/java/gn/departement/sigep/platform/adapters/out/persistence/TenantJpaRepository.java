package gn.departement.sigep.platform.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, Long> {

    Optional<TenantJpaEntity> findByCode(String code);
}
