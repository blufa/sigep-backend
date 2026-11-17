package gn.departement.sigep.staff.application.port.out;

import gn.departement.sigep.staff.domain.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PositionRepository {

    Position save(Position position);

    Optional<Position> findById(Long id);

    boolean existsByCode(String code);

    Page<Position> findAll(Pageable pageable);
}
