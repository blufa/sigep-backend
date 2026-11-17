package gn.departement.sigep.iam.domain;

/**
 * Rôles applicatifs : SUPER_ADMIN gère la plateforme (création des services/tenants),
 * ADMIN et AGENT sont scopés à un tenant.
 */
public enum Role {
    SUPER_ADMIN,
    ADMIN,
    AGENT
}
