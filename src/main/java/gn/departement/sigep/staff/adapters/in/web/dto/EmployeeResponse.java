package gn.departement.sigep.staff.adapters.in.web.dto;

import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.Genre;

public record EmployeeResponse(
        Long id,
        String matricule,
        String lastName,
        String firstName,
        String hierarchie,
        Genre genre,
        Long positionId,
        String phone,
        String city,
        boolean active
) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.id(),
                employee.matricule(),
                employee.lastName(),
                employee.firstName(),
                employee.hierarchie(),
                employee.genre(),
                employee.positionId(),
                employee.phone(),
                employee.city(),
                employee.active()
        );
    }
}
