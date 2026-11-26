package gn.departement.sigep.missions.application.service;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.missions.application.port.in.MissionUseCases;
import gn.departement.sigep.missions.application.port.out.MissionRepository;
import gn.departement.sigep.missions.domain.Mission;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class MissionService implements MissionUseCases {

    private final MissionRepository repository;
    private final EmployeeRepository employeeRepository;

    public MissionService(MissionRepository repository, EmployeeRepository employeeRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Mission create(CreateMissionCommand command) {
        if (command.endDate().isBefore(command.startDate())) {
            throw new BusinessRuleException(
                    "MISSION_INVALID_RANGE", "La date de fin ne peut pas précéder la date de début", HttpStatus.BAD_REQUEST);
        }
        employeeRepository.findById(command.employeeId())
                .orElseThrow(() -> NotFoundException.of("Employé", command.employeeId()));
        Mission mission = new Mission(
                null, TenantContext.get(), command.employeeId(), command.startDate(), command.endDate(),
                command.reason(), Instant.now());
        return repository.save(mission);
    }

    @Override
    public void cancel(Long id) {
        repository.findById(id).orElseThrow(() -> NotFoundException.of("Mission", id));
        repository.deleteById(id);
    }

    @Override
    public PageResponse<MissionView> list(Long employeeId, Pageable pageable) {
        Page<Mission> page = repository.findAll(employeeId, pageable);
        List<MissionView> views = page.getContent().stream().map(this::toView).toList();
        return PageResponse.from(page, views);
    }

    @Override
    public boolean isOnMission(Long employeeId, LocalDate date) {
        return repository.existsActiveForEmployeeOnDate(employeeId, date);
    }

    private MissionView toView(Mission mission) {
        Employee employee = employeeRepository.findById(mission.employeeId()).orElse(null);
        return new MissionView(
                mission.id(), mission.employeeId(),
                employee != null ? employee.matricule() : null,
                employee != null ? employee.firstName() + " " + employee.lastName() : "Employé supprimé",
                mission.startDate(), mission.endDate(), mission.reason(), mission.createdAt());
    }
}
