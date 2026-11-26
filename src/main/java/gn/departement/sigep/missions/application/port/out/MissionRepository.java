package gn.departement.sigep.missions.application.port.out;

import gn.departement.sigep.missions.domain.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface MissionRepository {

    Mission save(Mission mission);

    Optional<Mission> findById(Long id);

    void deleteById(Long id);

    /** @param employeeId filtre optionnel (null = toutes les missions du tenant courant). */
    Page<Mission> findAll(Long employeeId, Pageable pageable);

    /** Vrai si l'employé a une mission couvrant cette date (bornes incluses). */
    boolean existsActiveForEmployeeOnDate(Long employeeId, LocalDate date);
}
