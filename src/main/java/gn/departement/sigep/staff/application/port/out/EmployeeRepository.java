package gn.departement.sigep.staff.application.port.out;

import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.EmployeePhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {

    Employee save(Employee employee);

    Optional<Employee> findById(Long id);

    boolean existsByMatricule(String matricule);

    /** Utilisé par la vérification publique de badge — recherche sans contexte tenant (voir
     * {@code PublicBadgeController}), le matricule sert lui-même d'identifiant de recherche. */
    Optional<Employee> findByMatricule(String matricule);

    Page<Employee> findAll(Pageable pageable);

    /** Recherche par matricule/nom/prénom/téléphone (insensible à la casse, correspondance
     * partielle) — utilisée par la barre de recherche de la liste des employés. */
    Page<Employee> search(String query, Pageable pageable);

    long countByActiveTrue();

    List<Long> findActiveEmployeeIds();

    void savePhoto(Long employeeId, byte[] content, String contentType);

    Optional<EmployeePhoto> findPhoto(Long employeeId);
}
