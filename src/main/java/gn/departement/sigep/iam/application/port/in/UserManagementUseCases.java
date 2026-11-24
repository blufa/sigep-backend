package gn.departement.sigep.iam.application.port.in;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.iam.domain.Role;
import gn.departement.sigep.iam.domain.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserManagementUseCases {

    User create(CreateUserCommand command);

    /** Créé par un SUPER_ADMIN pour un service (tenant) donné, hors du TenantContext courant. */
    User createForTenant(Long tenantId, CreateUserCommand command);

    /** @param query recherche par identifiant/nom complet — {@code null} ou vide pour lister tous
     *               les utilisateurs sans filtre. */
    PageResponse<User> list(String query, Pageable pageable);

    /** Réinitialisation par un administrateur : pas besoin de l'ancien mot de passe. */
    void resetPassword(Long userId, String newPassword);

    /** Corrige l'identifiant d'un compte existant (ex. un numéro de téléphone mal normalisé à la
     * création — espaces, tirets… rendant la connexion impossible). Réservé à l'administrateur. */
    User updateUsername(Long userId, String newUsername);

    /** Changement par l'utilisateur lui-même : l'ancien mot de passe doit être vérifié. */
    void changeOwnPassword(Long userId, String currentPassword, String newPassword);

    /** Assigné par l'administrateur : les menus que cet utilisateur peut voir — null pour lever
     * toute restriction (il retrouve tous les menus permis par son rôle). */
    User updateAllowedMenus(Long userId, List<String> menuKeys);

    record CreateUserCommand(
            String username,
            String fullName,
            String password,
            Role role,
            /** Employé à rattacher (ex. depuis la fiche employé — voir "Créer un compte de
             * connexion") — {@code null} pour un compte sans employé associé. */
            Long employeeId
    ) {
    }
}
