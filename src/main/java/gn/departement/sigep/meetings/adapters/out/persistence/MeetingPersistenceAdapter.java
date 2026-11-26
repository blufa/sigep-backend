package gn.departement.sigep.meetings.adapters.out.persistence;

import gn.departement.sigep.meetings.application.port.out.MeetingRepository;
import gn.departement.sigep.meetings.domain.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class MeetingPersistenceAdapter implements MeetingRepository {

    private final MeetingJpaRepository jpaRepository;
    private final MeetingMapper mapper;

    MeetingPersistenceAdapter(MeetingJpaRepository jpaRepository, MeetingMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Meeting save(Meeting meeting) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(meeting)));
    }

    @Override
    public Optional<Meeting> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Page<Meeting> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }
}
