package gn.departement.sigep.staff.application.port.in;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.staff.domain.Position;
import org.springframework.data.domain.Pageable;

public interface PositionUseCases {

    Position create(CreatePositionCommand command);

    Position update(Long id, UpdatePositionCommand command);

    Position get(Long id);

    PageResponse<Position> list(Pageable pageable);

    record CreatePositionCommand(String title, String code) {
    }

    record UpdatePositionCommand(String title, String code, boolean active) {
    }
}
