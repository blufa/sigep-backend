package gn.departement.sigep.missions.adapters.out.persistence;

import gn.departement.sigep.missions.domain.Mission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface MissionMapper {

    Mission toDomain(MissionJpaEntity entity);

    MissionJpaEntity toEntity(Mission domain);
}
