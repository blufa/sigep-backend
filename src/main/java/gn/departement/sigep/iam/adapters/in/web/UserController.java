package gn.departement.sigep.iam.adapters.in.web;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.iam.adapters.in.web.dto.ChangePasswordRequest;
import gn.departement.sigep.iam.adapters.in.web.dto.CreateUserRequest;
import gn.departement.sigep.iam.adapters.in.web.dto.ResetPasswordRequest;
import gn.departement.sigep.iam.adapters.in.web.dto.UpdateUserMenusRequest;
import gn.departement.sigep.iam.adapters.in.web.dto.UserResponse;
import gn.departement.sigep.iam.application.port.in.UserManagementUseCases;
import gn.departement.sigep.iam.domain.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserManagementUseCases useCases;

    public UserController(UserManagementUseCases useCases) {
        this.useCases = useCases;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> list(Pageable pageable) {
        PageResponse<User> page = useCases.list(pageable);
        return new PageResponse<>(
                page.items().stream().map(UserResponse::from).toList(),
                page.page(), page.size(), page.totalItems(), page.totalPages());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        User user = useCases.create(new UserManagementUseCases.CreateUserCommand(
                request.username(), request.fullName(), request.password(), request.role()));
        return UserResponse.from(user);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        useCases.resetPassword(id, request.newPassword());
    }

    @PutMapping("/me/password")
    public void changeOwnPassword(JwtAuthenticationToken authentication, @Valid @RequestBody ChangePasswordRequest request) {
        Jwt jwt = authentication.getToken();
        Long userId = Long.valueOf(jwt.getSubject());
        useCases.changeOwnPassword(userId, request.currentPassword(), request.newPassword());
    }

    /** Assigne les menus visibles pour cet utilisateur — {@code menuKeys: null} lève toute
     * restriction. Réservé à l'administrateur du service (lui seul décide qui voit quoi). */
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateMenus(@PathVariable Long id, @RequestBody UpdateUserMenusRequest request) {
        return UserResponse.from(useCases.updateAllowedMenus(id, request.menuKeys()));
    }
}
