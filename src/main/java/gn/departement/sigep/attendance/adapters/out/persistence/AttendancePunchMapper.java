package gn.departement.sigep.attendance.adapters.out.persistence;

import gn.departement.sigep.attendance.domain.AttendancePunch;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface AttendancePunchMapper {

    AttendancePunch toDomain(AttendancePunchJpaEntity entity);

    AttendancePunchJpaEntity toEntity(AttendancePunch domain);
}
