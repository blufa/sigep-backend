package gn.departement.sigep.staff.adapters.in.web;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.staff.adapters.in.web.dto.PositionRequest;
import gn.departement.sigep.staff.adapters.in.web.dto.PositionResponse;
import gn.departement.sigep.staff.application.port.in.PositionUseCases;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private final PositionUseCases useCases;

    public PositionController(PositionUseCases useCases) {
        this.useCases = useCases;
    }

    @GetMapping
    public PageResponse<PositionResponse> list(Pageable pageable) {
        PageResponse<gn.departement.sigep.staff.domain.Position> page = useCases.list(pageable);
        return new PageResponse<>(
                page.items().stream().map(PositionResponse::from).toList(),
                page.page(), page.size(), page.totalItems(), page.totalPages());
    }

    @GetMapping("/{id}")
    public PositionResponse get(@PathVariable Long id) {
        return PositionResponse.from(useCases.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PositionResponse create(@Valid @RequestBody PositionRequest request) {
        return PositionResponse.from(useCases.create(new PositionUseCases.CreatePositionCommand(request.title(), request.code())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PositionResponse update(@PathVariable Long id, @Valid @RequestBody PositionRequest request) {
        return PositionResponse.from(useCases.update(id,
                new PositionUseCases.UpdatePositionCommand(request.title(), request.code(), request.active())));
    }
}
