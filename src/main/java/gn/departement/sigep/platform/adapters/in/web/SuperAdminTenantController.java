package gn.departement.sigep.platform.adapters.in.web;

import gn.departement.sigep.platform.adapters.in.web.dto.CreateTenantRequest;
import gn.departement.sigep.platform.adapters.in.web.dto.SetTenantActiveRequest;
import gn.departement.sigep.platform.adapters.in.web.dto.TenantResponse;
import gn.departement.sigep.platform.application.port.in.TenantManagementUseCases;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Réservé au SUPER_ADMIN : création et administration des services (tenants) rattachés au ministère. */
@RestController
@RequestMapping("/api/super-admin/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminTenantController {

    private final TenantManagementUseCases useCases;

    public SuperAdminTenantController(TenantManagementUseCases useCases) {
        this.useCases = useCases;
    }

    @GetMapping
    public List<TenantResponse> list() {
        return useCases.listTenants().stream().map(TenantResponse::from).toList();
    }

    @PostMapping
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        var tenant = useCases.createTenant(new TenantManagementUseCases.CreateTenantCommand(
                request.name(), request.code(), request.ministryName(), request.serviceName()));
        return TenantResponse.from(tenant);
    }

    @PutMapping("/{id}/active")
    public TenantResponse setActive(@PathVariable Long id, @RequestBody SetTenantActiveRequest request) {
        return TenantResponse.from(useCases.setActive(id, request.active()));
    }
}
