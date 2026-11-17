package gn.departement.sigep.badges.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface EmployeeBadgeJpaRepository extends JpaRepository<EmployeeBadgeJpaEntity, Long> {

    Optional<EmployeeBadgeJpaEntity> findByEmployeeId(Long employeeId);

    Optional<EmployeeBadgeJpaEntity> findByQrToken(String qrToken);

    List<EmployeeBadgeJpaEntity> findAllByEmployeeIdIn(List<Long> employeeIds);
}
