package gn.departement.sigep.badges.adapters.in.web;

import gn.departement.sigep.badges.adapters.in.web.dto.BadgeSheetRequest;
import gn.departement.sigep.badges.application.port.in.BadgeUseCases;
import gn.departement.sigep.badges.application.port.out.BadgeCardView;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BadgeController {

    private final BadgeUseCases useCases;

    public BadgeController(BadgeUseCases useCases) {
        this.useCases = useCases;
    }

    @GetMapping("/api/employees/{employeeId}/badge/card")
    public BadgeCardView cardView(@PathVariable Long employeeId) {
        return useCases.getCardView(employeeId);
    }

    @GetMapping("/api/employees/{employeeId}/badge")
    public ResponseEntity<byte[]> downloadCard(@PathVariable Long employeeId) {
        byte[] pdf = useCases.generateCardPdf(employeeId);
        return pdfResponse(pdf, "badge-" + employeeId + ".pdf");
    }

    @PostMapping("/api/badges/sheet")
    public ResponseEntity<byte[]> downloadSheet(@Valid @RequestBody BadgeSheetRequest request) {
        byte[] pdf = useCases.generateSheetPdf(request.employeeIds());
        return pdfResponse(pdf, "planche-badges.pdf");
    }

    @PostMapping("/api/badges/sheet/cards")
    public List<BadgeCardView> sheetCardViews(@Valid @RequestBody BadgeSheetRequest request) {
        return request.employeeIds().stream().map(useCases::getCardView).toList();
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
