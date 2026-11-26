package gn.departement.sigep.missions.application.port.in;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.missions.domain.Mission;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;

public interface MissionUseCases {

    Mission create(CreateMissionCommand command);

    void cancel(Long id);

    PageResponse<MissionView> list(Long employeeId, Pageable pageable);

    /** Consommé par le module attendance : un jour couvert par une mission ne doit pas être compté
     * absent (ni déclencher la notification SMS d'absence). */
    boolean isOnMission(Long employeeId, LocalDate date);

    record CreateMissionCommand(Long employeeId, LocalDate startDate, LocalDate endDate, String reason) {
    }

    /** Vue enrichie (matricule/nom) pour l'affichage — l'entité {@link Mission} ne porte que
     * l'identifiant employé, voir le même choix pour {@code AttendanceUseCases.DailyStatusEntry}. */
    record MissionView(
            Long id,
            Long employeeId,
            String matricule,
            String employeeFullName,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            Instant createdAt
    ) {
    }
}
