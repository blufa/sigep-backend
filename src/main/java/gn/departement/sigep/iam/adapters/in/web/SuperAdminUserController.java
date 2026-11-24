package gn.departement.sigep.iam.adapters.in.web;

import gn.departement.sigep.iam.adapters.in.web.dto.CreateUserRequest;
import gn.departement.sigep.iam.adapters.in.web.dto.UserResponse;
import gn.departement.sigep.iam.application.port.in.UserManagementUseCases;
import gn.departement.sigep.iam.domain.User;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Réservé au SUPER_ADMIN : créer le compte administrateur d'un service (tenant) nouvellement créé. */
@RestController
@RequestMapping("/api/super-admin/tenants/{tenantId}/users")
public class SuperAdminUserController {

    private final UserManagementUseCases useCases;

    public SuperAdminUserController(UserManagementUseCases useCases) {
        this.useCases = useCases;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public UserResponse create(@PathVariable Long tenantId, @Valid @RequestBody CreateUserRequest request) {
        User user = useCases.createForTenant(tenantId, new UserManagementUseCases.CreateUserCommand(
                request.username(), request.fullName(), request.password(), request.role(), request.employeeId()));
        return UserResponse.from(user);
    }
}
