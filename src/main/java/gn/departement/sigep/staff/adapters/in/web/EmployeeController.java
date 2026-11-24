package gn.departement.sigep.staff.adapters.in.web;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.staff.adapters.in.web.dto.EmployeeRequest;
import gn.departement.sigep.staff.adapters.in.web.dto.EmployeeResponse;
import gn.departement.sigep.staff.application.port.in.EmployeeUseCases;
import gn.departement.sigep.staff.domain.EmployeePhoto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeUseCases useCases;

    public EmployeeController(EmployeeUseCases useCases) {
        this.useCases = useCases;
    }

    @GetMapping
    public PageResponse<EmployeeResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
        PageResponse<gn.departement.sigep.staff.domain.Employee> page = useCases.list(q, pageable);
        return new PageResponse<>(
                page.items().stream().map(EmployeeResponse::from).toList(),
                page.page(), page.size(), page.totalItems(), page.totalPages());
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) {
        return EmployeeResponse.from(useCases.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeUseCases.CreateEmployeeCommand command = new EmployeeUseCases.CreateEmployeeCommand(
                request.matricule(), request.lastName(), request.firstName(),
                request.hierarchie(), request.genre(), request.positionId(), request.phone(), request.city());
        return EmployeeResponse.from(useCases.create(command));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        EmployeeUseCases.UpdateEmployeeCommand command = new EmployeeUseCases.UpdateEmployeeCommand(
                request.lastName(), request.firstName(), request.hierarchie(),
                request.genre(), request.positionId(), request.phone(), request.city(), request.active());
        return EmployeeResponse.from(useCases.update(id, command));
    }

    @PostMapping("/{id}/photo")
    @PreAuthorize("hasRole('ADMIN')")
    public void uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            useCases.uploadPhoto(id, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire la photo envoyée", e);
        }
    }

    @GetMapping("/report")
    public ResponseEntity<byte[]> report() {
        byte[] pdf = useCases.generateReportPdf();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"rapport-employes.pdf\"")
                .body(pdf);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        EmployeePhoto photo = useCases.getPhoto(id);
        if (photo == null) {
            throw new NotFoundException("Aucune photo pour cet employé");
        }
        MediaType mediaType = photo.contentType() != null
                ? MediaType.parseMediaType(photo.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(photo.content());
    }
}
