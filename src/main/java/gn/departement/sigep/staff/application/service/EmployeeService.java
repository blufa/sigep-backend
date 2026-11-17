package gn.departement.sigep.staff.application.service;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.staff.application.port.in.EmployeeUseCases;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.application.port.out.EmployeeReportRenderer;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.EmployeePhoto;
import gn.departement.sigep.staff.domain.Genre;
import gn.departement.sigep.staff.domain.Position;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmployeeService implements EmployeeUseCases {

    private final EmployeeRepository repository;
    private final PositionRepository positionRepository;
    private final TenantSettingsUseCases tenantSettingsUseCases;
    private final EmployeeReportRenderer reportRenderer;

    public EmployeeService(
            EmployeeRepository repository,
            PositionRepository positionRepository,
            TenantSettingsUseCases tenantSettingsUseCases,
            EmployeeReportRenderer reportRenderer) {
        this.repository = repository;
        this.positionRepository = positionRepository;
        this.tenantSettingsUseCases = tenantSettingsUseCases;
        this.reportRenderer = reportRenderer;
    }

    @Override
    public Employee create(CreateEmployeeCommand command) {
        if (repository.existsByMatricule(command.matricule())) {
            throw new BusinessRuleException(
                    "MATRICULE_TAKEN", "Ce matricule d'Etat est déjà enregistré", HttpStatus.CONFLICT);
        }
        Instant now = Instant.now();
        Employee employee = new Employee(
                null,
                TenantContext.get(),
                command.matricule(),
                command.lastName(),
                command.firstName(),
                command.hierarchie(),
                command.genre(),
                command.positionId(),
                command.phone(),
                command.city(),
                true,
                now,
                now
        );
        return repository.save(employee);
    }

    @Override
    public Employee update(Long id, UpdateEmployeeCommand command) {
        Employee existing = get(id);
        Employee updated = new Employee(
                existing.id(),
                existing.tenantId(),
                existing.matricule(),
                command.lastName(),
                command.firstName(),
                command.hierarchie(),
                command.genre(),
                command.positionId(),
                command.phone(),
                command.city(),
                command.active(),
                existing.createdAt(),
                Instant.now()
        );
        return repository.save(updated);
    }

    @Override
    public Employee get(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Employé", id));
    }

    @Override
    public PageResponse<Employee> list(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable));
    }

    @Override
    public void uploadPhoto(Long id, byte[] content, String contentType) {
        get(id);
        repository.savePhoto(id, content, contentType);
    }

    @Override
    public EmployeePhoto getPhoto(Long id) {
        return repository.findPhoto(id).orElse(null);
    }

    @Override
    public byte[] generateReportPdf() {
        Map<Long, String> positionTitleById = new HashMap<>();
        for (Position position : positionRepository.findAll(Pageable.unpaged())) {
            positionTitleById.put(position.id(), position.title());
        }

        var rows = repository.findAll(Pageable.unpaged()).stream()
                .map(employee -> new EmployeeReportRenderer.EmployeeReportRow(
                        employee.matricule(),
                        employee.lastName(),
                        employee.firstName(),
                        employee.hierarchie(),
                        employee.positionId() != null ? positionTitleById.get(employee.positionId()) : null,
                        employee.phone(),
                        genreLabel(employee.genre()),
                        employee.active()))
                .toList();

        var organization = tenantSettingsUseCases.getCurrent();
        var logo = tenantSettingsUseCases.getLogo(TenantContext.get());
        return reportRenderer.render(rows, organization.ministryName(), organization.serviceName(), logo);
    }

    private String genreLabel(Genre genre) {
        if (genre == null) {
            return null;
        }
        return genre == Genre.HOMME ? "Homme" : "Femme";
    }
}
