package gn.departement.sigep.attendance.adapters.in.web;

import gn.departement.sigep.attendance.adapters.in.web.dto.AttendanceEntryResponse;
import gn.departement.sigep.attendance.adapters.in.web.dto.AttendanceSummaryResponse;
import gn.departement.sigep.attendance.adapters.in.web.dto.ManualScanRequest;
import gn.departement.sigep.attendance.adapters.in.web.dto.ScanRequest;
import gn.departement.sigep.attendance.adapters.in.web.dto.ScanResponse;
import gn.departement.sigep.attendance.application.port.in.AttendanceUseCases;
import gn.departement.sigep.attendance.domain.AttendancePunchPhoto;
import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Base64;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceUseCases useCases;

    public AttendanceController(AttendanceUseCases useCases) {
        this.useCases = useCases;
    }

    @PostMapping("/scan")
    public ScanResponse scan(@Valid @RequestBody ScanRequest request) {
        byte[] photo = decodePhoto(request.photoBase64());
        return ScanResponse.from(useCases.scan(request.qrToken(), request.source(), photo, "image/jpeg"));
    }

    @PostMapping("/scan-manual")
    public ScanResponse scanManual(@Valid @RequestBody ManualScanRequest request) {
        return ScanResponse.from(useCases.scanManual(request.employeeId(), "MANUEL"));
    }

    @PutMapping("/{id}/flag")
    public void flag(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean flagged) {
        useCases.flagPunch(id, flagged);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> punchPhoto(@PathVariable Long id) {
        AttendancePunchPhoto photo = useCases.getPunchPhoto(id)
                .orElseThrow(() -> new NotFoundException("Aucune photo pour ce pointage"));
        MediaType mediaType = photo.contentType() != null
                ? MediaType.parseMediaType(photo.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(photo.content());
    }

    private byte[] decodePhoto(String photoBase64) {
        if (photoBase64 == null || photoBase64.isBlank()) {
            return null;
        }
        String raw = photoBase64.contains(",") ? photoBase64.substring(photoBase64.indexOf(',') + 1) : photoBase64;
        return Base64.getDecoder().decode(raw);
    }

    @GetMapping
    public PageResponse<AttendanceEntryResponse> history(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable) {
        PageResponse<AttendanceUseCases.AttendanceEntry> page = useCases.history(employeeId, from, to, pageable);
        return new PageResponse<>(
                page.items().stream().map(AttendanceEntryResponse::from).toList(),
                page.page(), page.size(), page.totalItems(), page.totalPages());
    }

    @GetMapping("/summary")
    public AttendanceSummaryResponse summary() {
        return AttendanceSummaryResponse.from(useCases.summary());
    }

    @GetMapping("/report/monthly")
    public ResponseEntity<byte[]> monthlyReport(@RequestParam int year, @RequestParam int month) {
        byte[] pdf = useCases.generateMonthlyReportPdf(YearMonth.of(year, month));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"rapport-mensuel-presence.pdf\"")
                .body(pdf);
    }
}
