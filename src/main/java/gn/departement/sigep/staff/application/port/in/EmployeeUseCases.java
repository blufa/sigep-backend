package gn.departement.sigep.staff.application.port.in;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.EmployeePhoto;
import gn.departement.sigep.staff.domain.Genre;
import org.springframework.data.domain.Pageable;

public interface EmployeeUseCases {

    Employee create(CreateEmployeeCommand command);

    Employee update(Long id, UpdateEmployeeCommand command);

    Employee get(Long id);

    PageResponse<Employee> list(Pageable pageable);

    void uploadPhoto(Long id, byte[] content, String contentType);

    EmployeePhoto getPhoto(Long id);

    byte[] generateReportPdf();

    record CreateEmployeeCommand(
            String matricule,
            String lastName,
            String firstName,
            String hierarchie,
            Genre genre,
            Long positionId,
            String phone,
            String city
    ) {
    }

    record UpdateEmployeeCommand(
            String lastName,
            String firstName,
            String hierarchie,
            Genre genre,
            Long positionId,
            String phone,
            String city,
            boolean active
    ) {
    }
}
