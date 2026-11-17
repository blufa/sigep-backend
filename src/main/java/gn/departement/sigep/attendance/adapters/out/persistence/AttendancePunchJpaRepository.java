package gn.departement.sigep.attendance.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface AttendancePunchJpaRepository extends JpaRepository<AttendancePunchJpaEntity, Long> {

    @Query("select p.id from AttendancePunchJpaEntity p where p.id in :ids and p.photo is not null")
    List<Long> findIdsWithPhoto(@Param("ids") List<Long> ids);

    Optional<AttendancePunchJpaEntity> findFirstByEmployeeIdAndScannedAtBetweenOrderByScannedAtDesc(
            Long employeeId, Instant start, Instant end);

    List<AttendancePunchJpaEntity> findAllByScannedAtBetween(Instant start, Instant end);

    Page<AttendancePunchJpaEntity> findByEmployeeIdAndScannedAtBetween(
            Long employeeId, Instant start, Instant end, Pageable pageable);

    Page<AttendancePunchJpaEntity> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<AttendancePunchJpaEntity> findByScannedAtBetween(Instant start, Instant end, Pageable pageable);
}
