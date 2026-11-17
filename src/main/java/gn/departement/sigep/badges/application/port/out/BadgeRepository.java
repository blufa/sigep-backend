package gn.departement.sigep.badges.application.port.out;

import gn.departement.sigep.badges.domain.EmployeeBadge;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository {

    EmployeeBadge save(EmployeeBadge badge);

    Optional<EmployeeBadge> findByEmployeeId(Long employeeId);

    Optional<EmployeeBadge> findByQrToken(String qrToken);

    List<EmployeeBadge> findAllByEmployeeIdIn(List<Long> employeeIds);
}
