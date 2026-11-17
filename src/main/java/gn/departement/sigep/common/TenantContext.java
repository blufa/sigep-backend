package gn.departement.sigep.common;

/**
 * Porte l'identifiant du tenant courant pour la durée d'une requête HTTP.
 * Rempli par le filtre de sécurité (claim JWT), lu par {@code TenantFilterAspect}
 * pour activer le filtre Hibernate {@code tenantFilter} avant chaque accès repository.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
