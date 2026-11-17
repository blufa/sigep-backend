package gn.departement.sigep.badges.adapters.out.persistence;

import gn.departement.sigep.badges.domain.EmployeeBadge;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface EmployeeBadgeMapper {

    EmployeeBadge toDomain(EmployeeBadgeJpaEntity entity);

    EmployeeBadgeJpaEntity toEntity(EmployeeBadge domain);
}
