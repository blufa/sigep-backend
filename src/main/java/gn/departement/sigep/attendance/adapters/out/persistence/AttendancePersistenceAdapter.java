package gn.departement.sigep.attendance.adapters.out.persistence;

import gn.departement.sigep.attendance.application.port.out.AttendanceRepository;
import gn.departement.sigep.attendance.domain.AttendancePunch;
import gn.departement.sigep.attendance.domain.AttendancePunchPhoto;
import gn.departement.sigep.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class AttendancePersistenceAdapter implements AttendanceRepository {

    private final AttendancePunchJpaRepository jpaRepository;
    private final AttendancePunchMapper mapper;

    AttendancePersistenceAdapter(AttendancePunchJpaRepository jpaRepository, AttendancePunchMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AttendancePunch save(AttendancePunch punch) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(punch)));
    }

    @Override
    public Optional<AttendancePunch> findLastByEmployeeIdAndScannedAtBetween(Long employeeId, Instant start, Instant end) {
        return jpaRepository.findFirstByEmployeeIdAndScannedAtBetweenOrderByScannedAtDesc(employeeId, start, end)
                .map(mapper::toDomain);
    }

    @Override
    public List<AttendancePunch> findAllByScannedAtBetween(Instant start, Instant end) {
        return jpaRepository.findAllByScannedAtBetween(start, end).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<AttendancePunch> search(Long employeeId, Instant from, Instant to, Pageable pageable) {
        Page<AttendancePunchJpaEntity> page;
        if (employeeId != null && from != null && to != null) {
            page = jpaRepository.findByEmployeeIdAndScannedAtBetween(employeeId, from, to, pageable);
        } else if (employeeId != null) {
            page = jpaRepository.findByEmployeeId(employeeId, pageable);
        } else if (from != null && to != null) {
            page = jpaRepository.findByScannedAtBetween(from, to, pageable);
        } else {
            page = jpaRepository.findAll(pageable);
        }
        return page.map(mapper::toDomain);
    }

    @Override
    public void savePunchPhoto(Long punchId, byte[] content, String contentType) {
        AttendancePunchJpaEntity entity = jpaRepository.findById(punchId)
                .orElseThrow(() -> NotFoundException.of("Pointage", punchId));
        entity.setPhoto(content);
        entity.setPhotoContentType(contentType);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<AttendancePunchPhoto> findPunchPhoto(Long punchId) {
        return jpaRepository.findById(punchId)
                .filter(entity -> entity.getPhoto() != null)
                .map(entity -> new AttendancePunchPhoto(entity.getPhoto(), entity.getPhotoContentType()));
    }

    @Override
    public List<Long> findPunchIdsWithPhoto(List<Long> punchIds) {
        if (punchIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findIdsWithPhoto(punchIds);
    }
}
