package gn.departement.sigep.staff.adapters.out.persistence;

import gn.departement.sigep.staff.domain.Employee;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface EmployeeMapper {

    Employee toDomain(EmployeeJpaEntity entity);
}
