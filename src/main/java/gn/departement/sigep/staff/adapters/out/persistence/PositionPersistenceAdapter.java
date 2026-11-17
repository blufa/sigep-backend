package gn.departement.sigep.staff.adapters.out.persistence;

import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class PositionPersistenceAdapter implements PositionRepository {

    private final PositionJpaRepository jpaRepository;
    private final PositionMapper mapper;

    PositionPersistenceAdapter(PositionJpaRepository jpaRepository, PositionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Position save(Position position) {
        PositionJpaEntity saved = jpaRepository.save(mapper.toEntity(position));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Position> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public Page<Position> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }
}
