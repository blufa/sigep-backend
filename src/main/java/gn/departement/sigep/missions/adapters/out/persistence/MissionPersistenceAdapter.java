package gn.departement.sigep.missions.adapters.out.persistence;

import gn.departement.sigep.missions.application.port.out.MissionRepository;
import gn.departement.sigep.missions.domain.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
class MissionPersistenceAdapter implements MissionRepository {

    private final MissionJpaRepository jpaRepository;
    private final MissionMapper mapper;

    MissionPersistenceAdapter(MissionJpaRepository jpaRepository, MissionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Mission save(Mission mission) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(mission)));
    }

    @Override
    public Optional<Mission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Page<Mission> findAll(Long employeeId, Pageable pageable) {
        Page<MissionJpaEntity> page = employeeId != null
                ? jpaRepository.findByEmployeeId(employeeId, pageable)
                : jpaRepository.findAll(pageable);
        return page.map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveForEmployeeOnDate(Long employeeId, LocalDate date) {
        return jpaRepository.existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(employeeId, date, date);
    }
}
