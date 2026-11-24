package gn.departement.sigep.iam.application.service;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.iam.application.port.in.UserManagementUseCases;
import gn.departement.sigep.iam.application.port.out.UserRepository;
import gn.departement.sigep.iam.domain.Role;
import gn.departement.sigep.iam.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserManagementService implements UserManagementUseCases {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User create(CreateUserCommand command) {
        return createInTenant(TenantContext.get(), command);
    }

    @Override
    public User createForTenant(Long tenantId, CreateUserCommand command) {
        return createInTenant(tenantId, command);
    }

    private User createInTenant(Long tenantId, CreateUserCommand command) {
        if (command.role() == Role.SUPER_ADMIN) {
            throw new BusinessRuleException(
                    "INVALID_ROLE", "Le rôle super-administrateur ne peut pas être attribué à un compte de service",
                    HttpStatus.BAD_REQUEST);
        }
        String username = normalizeUsername(command.username());
        if (repository.existsByUsername(username)) {
            throw new BusinessRuleException(
                    "USERNAME_TAKEN", "Cet identifiant est déjà utilisé", HttpStatus.CONFLICT);
        }
        User user = new User(
                null,
                tenantId,
                username,
                passwordEncoder.encode(command.password()),
                command.fullName(),
                command.role(),
                true,
                null,
                command.employeeId()
        );
        return repository.save(user);
    }

    /** Un identifiant ne doit jamais contenir d'espace — sans ça, un compte créé depuis un numéro
     * de téléphone affiché formaté (ex. "622 78 43 22") devient impossible à saisir correctement
     * à la connexion, l'utilisateur tapant naturellement son numéro sans espaces (signalé par
     * l'utilisateur : connexion systématiquement refusée après création du compte). */
    private String normalizeUsername(String rawUsername) {
        return rawUsername.trim().replaceAll("\\s+", "");
    }

    @Override
    public PageResponse<User> list(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return PageResponse.from(repository.findAll(pageable));
        }
        return PageResponse.from(repository.search(query.trim(), pageable));
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        User user = getOrThrow(userId);
        save(user, newPassword);
    }

    @Override
    public User updateUsername(Long userId, String newUsername) {
        User user = getOrThrow(userId);
        String normalized = normalizeUsername(newUsername);
        if (!normalized.equals(user.username()) && repository.existsByUsername(normalized)) {
            throw new BusinessRuleException(
                    "USERNAME_TAKEN", "Cet identifiant est déjà utilisé", HttpStatus.CONFLICT);
        }
        User updated = new User(
                user.id(), user.tenantId(), normalized, user.passwordHash(), user.fullName(),
                user.role(), user.active(), user.allowedMenus(), user.employeeId());
        return repository.save(updated);
    }

    @Override
    public void changeOwnPassword(Long userId, String currentPassword, String newPassword) {
        User user = getOrThrow(userId);
        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new BusinessRuleException(
                    "INVALID_CURRENT_PASSWORD", "Mot de passe actuel incorrect", HttpStatus.FORBIDDEN);
        }
        save(user, newPassword);
    }

    @Override
    public User updateAllowedMenus(Long userId, List<String> menuKeys) {
        User user = getOrThrow(userId);
        User updated = new User(
                user.id(), user.tenantId(), user.username(), user.passwordHash(), user.fullName(),
                user.role(), user.active(), menuKeys, user.employeeId());
        return repository.save(updated);
    }

    private User getOrThrow(Long userId) {
        return repository.findById(userId).orElseThrow(() -> NotFoundException.of("Utilisateur", userId));
    }

    private void save(User user, String newPassword) {
        repository.save(new User(
                user.id(), user.tenantId(), user.username(),
                passwordEncoder.encode(newPassword), user.fullName(), user.role(), user.active(),
                user.allowedMenus(), user.employeeId()));
    }
}
