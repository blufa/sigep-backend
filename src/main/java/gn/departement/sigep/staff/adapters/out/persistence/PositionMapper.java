package gn.departement.sigep.staff.adapters.out.persistence;

import gn.departement.sigep.staff.domain.Position;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface PositionMapper {

    Position toDomain(PositionJpaEntity entity);

    PositionJpaEntity toEntity(Position domain);
}
