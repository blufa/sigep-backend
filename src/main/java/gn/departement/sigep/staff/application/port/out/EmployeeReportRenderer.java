package gn.departement.sigep.staff.application.port.out;

import gn.departement.sigep.platform.domain.OrganizationLogo;

import java.util.List;

/** Rendu PDF du rapport (listing tabulaire) des employés — en-tête ministère/service/logo commun au badge. */
public interface EmployeeReportRenderer {

    byte[] render(List<EmployeeReportRow> rows, String ministryName, String serviceName, OrganizationLogo logo);

    record EmployeeReportRow(
            String matricule,
            String lastName,
            String firstName,
            String hierarchie,
            String positionTitle,
            String phone,
            String genreLabel,
            boolean active
    ) {
    }
}
