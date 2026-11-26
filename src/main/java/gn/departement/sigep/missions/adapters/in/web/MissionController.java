package gn.departement.sigep.missions.adapters.in.web;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.missions.adapters.in.web.dto.MissionRequest;
import gn.departement.sigep.missions.adapters.in.web.dto.MissionResponse;
import gn.departement.sigep.missions.adapters.in.web.dto.UpdateMissionRequest;
import gn.departement.sigep.missions.application.port.in.MissionUseCases;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionUseCases useCases;

    public MissionController(MissionUseCases useCases) {
        this.useCases = useCases;
    }

    @GetMapping
    public PageResponse<MissionResponse> list(@RequestParam(required = false) Long employeeId, Pageable pageable) {
        PageResponse<MissionUseCases.MissionView> page = useCases.list(employeeId, pageable);
        return new PageResponse<>(
                page.items().stream().map(MissionResponse::from).toList(),
                page.page(), page.size(), page.totalItems(), page.totalPages());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public MissionResponse create(@Valid @RequestBody MissionRequest request) {
        return MissionResponse.from(useCases.create(
                new MissionUseCases.CreateMissionCommand(request.employeeId(), request.startDate(), request.endDate(), request.reason())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MissionResponse update(@PathVariable Long id, @Valid @RequestBody UpdateMissionRequest request) {
        return MissionResponse.from(useCases.update(id,
                new MissionUseCases.UpdateMissionCommand(request.startDate(), request.endDate(), request.reason())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void cancel(@PathVariable Long id) {
        useCases.cancel(id);
    }
}
