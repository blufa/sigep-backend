package gn.departement.sigep.staff.adapters.out.persistence;

import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.EmployeePhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class EmployeePersistenceAdapter implements EmployeeRepository {

    private final EmployeeJpaRepository jpaRepository;
    private final EmployeeMapper mapper;

    EmployeePersistenceAdapter(EmployeeJpaRepository jpaRepository, EmployeeMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Employee save(Employee employee) {
        // Charge l'entité existante plutôt qu'un mapper.toEntity() "à neuf" : le domain Employee ne
        // porte pas la photo (binaire tenu à part, voir savePhoto/findPhoto) — repartir d'une entité
        // neuve écraserait silencieusement une photo déjà enregistrée à chaque modification de fiche.
        EmployeeJpaEntity entity = employee.id() != null
                ? jpaRepository.findById(employee.id()).orElseGet(EmployeeJpaEntity::new)
                : new EmployeeJpaEntity();
        entity.setTenantId(employee.tenantId());
        entity.setMatricule(employee.matricule());
        entity.setLastName(employee.lastName());
        entity.setFirstName(employee.firstName());
        entity.setHierarchie(employee.hierarchie());
        entity.setGenre(employee.genre());
        entity.setPositionId(employee.positionId());
        entity.setPhone(employee.phone());
        entity.setCity(employee.city());
        entity.setActive(employee.active());
        entity.setCreatedAt(employee.createdAt());
        entity.setUpdatedAt(employee.updatedAt());
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByMatricule(String matricule) {
        return jpaRepository.existsByMatricule(matricule);
    }

    @Override
    public Optional<Employee> findByMatricule(String matricule) {
        return jpaRepository.findByMatricule(matricule).map(mapper::toDomain);
    }

    @Override
    public Page<Employee> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public long countByActiveTrue() {
        return jpaRepository.countByActiveTrue();
    }

    @Override
    public List<Long> findActiveEmployeeIds() {
        return jpaRepository.findActiveIds();
    }

    @Override
    public void savePhoto(Long employeeId, byte[] content, String contentType) {
        EmployeeJpaEntity entity = jpaRepository.findById(employeeId)
                .orElseThrow(() -> NotFoundException.of("Employé", employeeId));
        entity.setPhoto(content);
        entity.setPhotoContentType(contentType);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<EmployeePhoto> findPhoto(Long employeeId) {
        return jpaRepository.findById(employeeId)
                .filter(entity -> entity.getPhoto() != null)
                .map(entity -> new EmployeePhoto(entity.getPhoto(), entity.getPhotoContentType()));
    }
}
