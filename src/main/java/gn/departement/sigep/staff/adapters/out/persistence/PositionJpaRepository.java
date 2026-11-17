package gn.departement.sigep.staff.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PositionJpaRepository extends JpaRepository<PositionJpaEntity, Long> {

    boolean existsByCode(String code);

    Page<PositionJpaEntity> findAll(Pageable pageable);
}
