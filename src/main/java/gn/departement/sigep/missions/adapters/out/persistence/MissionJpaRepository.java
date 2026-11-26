package gn.departement.sigep.missions.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

interface MissionJpaRepository extends JpaRepository<MissionJpaEntity, Long> {

    Page<MissionJpaEntity> findByEmployeeId(Long employeeId, Pageable pageable);

    boolean existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId, LocalDate startDate, LocalDate endDate);
}
