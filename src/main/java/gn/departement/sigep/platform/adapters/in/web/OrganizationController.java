package gn.departement.sigep.platform.adapters.in.web;

import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.platform.adapters.in.web.dto.OrganizationResponse;
import gn.departement.sigep.platform.adapters.in.web.dto.UpdateOrganizationRequest;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.platform.domain.OrganizationLogo;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final TenantSettingsUseCases useCases;

    public OrganizationController(TenantSettingsUseCases useCases) {
        this.useCases = useCases;
    }

    @GetMapping
    public OrganizationResponse get() {
        OrganizationLogo logo = useCases.getLogo(TenantContext.get());
        return OrganizationResponse.from(useCases.getCurrent(), logo != null);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public OrganizationResponse update(@Valid @RequestBody UpdateOrganizationRequest request) {
        var tenant = useCases.updateCurrent(
                new TenantSettingsUseCases.UpdateOrganizationCommand(
                        request.ministryName(), request.serviceName(), request.workStartTime(), request.workEndTime()));
        OrganizationLogo logo = useCases.getLogo(TenantContext.get());
        return OrganizationResponse.from(tenant, logo != null);
    }

    @PostMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public void uploadLogo(@RequestParam("file") MultipartFile file) {
        try {
            useCases.updateLogo(file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le logo envoyé", e);
        }
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        OrganizationLogo logo = useCases.getLogo(TenantContext.get());
        if (logo == null) {
            throw new NotFoundException("Aucun logo configuré pour cette organisation");
        }
        MediaType mediaType = logo.contentType() != null
                ? MediaType.parseMediaType(logo.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(logo.content());
    }
}
