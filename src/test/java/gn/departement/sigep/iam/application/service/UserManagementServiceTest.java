package gn.departement.sigep.iam.application.service;

import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.iam.application.port.in.UserManagementUseCases.CreateUserCommand;
import gn.departement.sigep.iam.application.port.out.UserRepository;
import gn.departement.sigep.iam.domain.Role;
import gn.departement.sigep.iam.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserManagementServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserManagementService service = new UserManagementService(repository, passwordEncoder);

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void createRejectsSuperAdminRole() {
        TenantContext.set(1L);

        assertThatThrownBy(() -> service.create(new CreateUserCommand("x", "X", "password123", Role.SUPER_ADMIN, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createForTenantStampsGivenTenantRegardlessOfCurrentContext() {
        TenantContext.clear();
        when(repository.existsByUsername("directeur")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.createForTenant(7L, new CreateUserCommand("directeur", "Directeur", "password123", Role.ADMIN, null));

        assertThat(created.tenantId()).isEqualTo(7L);
        assertThat(created.role()).isEqualTo(Role.ADMIN);
        assertThat(created.active()).isTrue();
    }

    @Test
    void createForTenantRejectsSuperAdminRole() {
        assertThatThrownBy(() -> service.createForTenant(7L, new CreateUserCommand("x", "X", "password123", Role.SUPER_ADMIN, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateAllowedMenusRestrictsToGivenKeys() {
        User existing = new User(5L, 1L, "dg", "hash", "Directeur Général", Role.ADMIN, true, null, null);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = service.updateAllowedMenus(5L, List.of("dashboard"));

        assertThat(updated.allowedMenus()).containsExactly("dashboard");
    }

    @Test
    void updateAllowedMenusClearsRestrictionWhenNull() {
        User existing = new User(5L, 1L, "dg", "hash", "Directeur Général", Role.ADMIN, true, List.of("dashboard"), null);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = service.updateAllowedMenus(5L, null);

        assertThat(updated.allowedMenus()).isNull();
    }
}
