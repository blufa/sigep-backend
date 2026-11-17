package gn.departement.sigep.platform.application.port.out;

import gn.departement.sigep.platform.domain.OrganizationLogo;
import gn.departement.sigep.platform.domain.Tenant;

import java.util.List;
import java.util.Optional;

public interface TenantRepository {

    Optional<Tenant> findById(Long id);

    Optional<Tenant> findByCode(String code);

    /** Utilisé par la page publique (sans authentification, donc sans tenant courant) pour afficher
     * le logo/nom de l'organisation — tant qu'un seul tenant existe en pratique, "le premier" suffit. */
    Optional<Tenant> findFirst();

    /** Tous les services (tenants) de la plateforme — réservé au SUPER_ADMIN. */
    List<Tenant> findAll();

    Tenant save(Tenant tenant);

    void saveLogo(Long tenantId, byte[] content, String contentType);

    Optional<OrganizationLogo> findLogo(Long tenantId);
}
