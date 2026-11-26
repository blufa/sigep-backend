package gn.departement.sigep.meetings.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface MeetingJpaRepository extends JpaRepository<MeetingJpaEntity, Long> {

    Page<MeetingJpaEntity> findAll(Pageable pageable);
}
