package gn.departement.sigep.platform.adapters.in.web;

import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.platform.domain.OrganizationLogo;
import gn.departement.sigep.platform.domain.Tenant;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nom/logo du ministère affichés sur la page publique (sans authentification, donc sans tenant
 * courant) — voir {@link gn.departement.sigep.badges.adapters.in.web.PublicBadgeController} pour
 * le même principe appliqué à la vérification de badge.
 */
@RestController
@RequestMapping("/api/public/organization")
public class PublicOrganizationController {

    private final TenantSettingsUseCases useCases;

    public PublicOrganizationController(TenantSettingsUseCases useCases) {
        this.useCases = useCases;
    }

    public record PublicOrganizationResponse(String ministryName, String serviceName, boolean hasLogo) {
    }

    @GetMapping
    public PublicOrganizationResponse get() {
        Tenant tenant = useCases.getPublic();
        OrganizationLogo logo = useCases.getPublicLogo();
        return new PublicOrganizationResponse(tenant.ministryName(), tenant.serviceName(), logo != null);
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        OrganizationLogo logo = useCases.getPublicLogo();
        if (logo == null) {
            throw new NotFoundException("Aucun logo configuré");
        }
        MediaType mediaType = logo.contentType() != null
                ? MediaType.parseMediaType(logo.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(logo.content());
    }
}
