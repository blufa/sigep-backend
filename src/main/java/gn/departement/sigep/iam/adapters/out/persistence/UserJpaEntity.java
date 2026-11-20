package gn.departement.sigep.iam.adapters.out.persistence;

import gn.departement.sigep.iam.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "users")
@Filter(name = "tenantFilter")
@Getter
@Setter
@NoArgsConstructor
class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null pour un SUPER_ADMIN : il n'est rattaché à aucun service, il les gère tous. */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(nullable = false)
    private Boolean active = true;

    /** Clés de menu séparées par des virgules (ex. "dashboard,attendance") ; null = aucune
     * restriction. Voir {@code UserRepositoryAdapter} pour la conversion depuis/vers List<String>. */
    @Column(name = "allowed_menus")
    private String allowedMenus;
}
