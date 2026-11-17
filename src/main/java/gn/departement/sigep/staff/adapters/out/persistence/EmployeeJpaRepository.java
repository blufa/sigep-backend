package gn.departement.sigep.staff.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, Long> {

    boolean existsByMatricule(String matricule);

    Optional<EmployeeJpaEntity> findByMatricule(String matricule);

    Page<EmployeeJpaEntity> findAll(Pageable pageable);

    long countByActiveTrue();

    @Query("select e.id from EmployeeJpaEntity e where e.active = true")
    List<Long> findActiveIds();
}
