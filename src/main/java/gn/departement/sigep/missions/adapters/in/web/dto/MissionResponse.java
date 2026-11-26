package gn.departement.sigep.missions.adapters.in.web.dto;

import gn.departement.sigep.missions.application.port.in.MissionUseCases;
import gn.departement.sigep.missions.domain.Mission;

import java.time.Instant;
import java.time.LocalDate;

public record MissionResponse(
        Long id,
        Long employeeId,
        String matricule,
        String employeeFullName,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        Instant createdAt
) {

    public static MissionResponse from(MissionUseCases.MissionView view) {
        return new MissionResponse(
                view.id(), view.employeeId(), view.matricule(), view.employeeFullName(),
                view.startDate(), view.endDate(), view.reason(), view.createdAt());
    }

    /** Juste après la création : pas encore de vue enrichie, le front connaît déjà l'employé choisi. */
    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
                mission.id(), mission.employeeId(), null, null,
                mission.startDate(), mission.endDate(), mission.reason(), mission.createdAt());
    }
}
