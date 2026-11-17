package gn.departement.sigep.badges.adapters.out.persistence;

import gn.departement.sigep.badges.application.port.out.BadgeRepository;
import gn.departement.sigep.badges.domain.EmployeeBadge;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class BadgePersistenceAdapter implements BadgeRepository {

    private final EmployeeBadgeJpaRepository jpaRepository;
    private final EmployeeBadgeMapper mapper;

    BadgePersistenceAdapter(EmployeeBadgeJpaRepository jpaRepository, EmployeeBadgeMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public EmployeeBadge save(EmployeeBadge badge) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(badge)));
    }

    @Override
    public Optional<EmployeeBadge> findByEmployeeId(Long employeeId) {
        return jpaRepository.findByEmployeeId(employeeId).map(mapper::toDomain);
    }

    @Override
    public Optional<EmployeeBadge> findByQrToken(String qrToken) {
        return jpaRepository.findByQrToken(qrToken).map(mapper::toDomain);
    }

    @Override
    public List<EmployeeBadge> findAllByEmployeeIdIn(List<Long> employeeIds) {
        return jpaRepository.findAllByEmployeeIdIn(employeeIds).stream().map(mapper::toDomain).toList();
    }
}
