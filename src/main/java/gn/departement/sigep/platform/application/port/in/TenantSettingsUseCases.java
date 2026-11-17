package gn.departement.sigep.platform.application.port.in;

import gn.departement.sigep.platform.domain.OrganizationLogo;
import gn.departement.sigep.platform.domain.Tenant;

import java.time.LocalTime;

public interface TenantSettingsUseCases {

    /** Organisation du tenant courant (résolu depuis le contexte multi-tenant de la requête). */
    Tenant getCurrent();

    Tenant updateCurrent(UpdateOrganizationCommand command);

    void updateLogo(byte[] content, String contentType);

    /** Logo d'un tenant donné (utilisé aussi bien pour l'affichage front que pour la génération PDF). */
    OrganizationLogo getLogo(Long tenantId);

    /** Organisation affichée sur la page publique (sans authentification, donc sans tenant courant). */
    Tenant getPublic();

    /** Logo affiché sur la page publique — même tenant que {@link #getPublic()}. */
    OrganizationLogo getPublicLogo();

    record UpdateOrganizationCommand(
            String ministryName, String serviceName, LocalTime workStartTime, LocalTime workEndTime) {
    }
}
