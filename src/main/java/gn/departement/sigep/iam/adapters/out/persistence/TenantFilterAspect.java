package gn.departement.sigep.iam.adapters.out.persistence;

import gn.departement.sigep.common.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Active le filtre Hibernate {@code tenantFilter} sur la session JPA courante
 * avant chaque appel de repository Spring Data, pour isoler les données par tenant.
 *
 * Nécessite {@code spring.jpa.open-in-view=true} : ce {@code @Before} s'exécute en dehors de toute
 * transaction (avant que le repository Spring Data n'ouvre la sienne). Avec open-in-view=false,
 * {@code entityManager.unwrap(Session.class)} renvoie alors une session temporaire aussitôt
 * refermée par le proxy d'EntityManager partagé de Spring, et le filtre activé ici n'a jamais
 * d'effet sur la session réellement utilisée par la requête qui suit — isolation multi-tenant
 * silencieusement inopérante (voir TenantIsolationFlowTest). Avec open-in-view=true, une session
 * unique est liée au thread pour toute la durée de la requête HTTP et ce filtre s'y applique bien.
 *
 * Attention : {@code EntityManager.find()} par clé primaire ignore ce filtre -
 * les lectures par ID doivent revérifier manuellement le {@code tenantId}.
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableTenantFilter() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            return;
        }
        entityManager.unwrap(Session.class)
                .enableFilter("tenantFilter")
                .setParameter("tenantId", tenantId);
    }
}
