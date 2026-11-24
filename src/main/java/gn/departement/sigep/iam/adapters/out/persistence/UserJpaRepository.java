package gn.departement.sigep.iam.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<UserJpaEntity> findAll(Pageable pageable);

    @Query("""
            select u from UserJpaEntity u
            where lower(u.username) like lower(concat('%', :q, '%'))
               or lower(u.fullName) like lower(concat('%', :q, '%'))
            """)
    Page<UserJpaEntity> search(@Param("q") String query, Pageable pageable);
}
