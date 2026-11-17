package gn.departement.sigep.staff.application.service;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.staff.application.port.in.PositionUseCases;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Position;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PositionService implements PositionUseCases {

    private final PositionRepository repository;

    public PositionService(PositionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Position create(CreatePositionCommand command) {
        if (repository.existsByCode(command.code())) {
            throw new BusinessRuleException("POSITION_CODE_TAKEN", "Ce code de poste existe déjà", HttpStatus.CONFLICT);
        }
        Position position = new Position(null, TenantContext.get(), command.title(), command.code(), true);
        return repository.save(position);
    }

    @Override
    public Position update(Long id, UpdatePositionCommand command) {
        Position existing = get(id);
        Position updated = new Position(existing.id(), existing.tenantId(), command.title(), command.code(), command.active());
        return repository.save(updated);
    }

    @Override
    public Position get(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Poste", id));
    }

    @Override
    public PageResponse<Position> list(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable));
    }
}
