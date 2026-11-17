package gn.departement.sigep.attendance.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(
        @NotBlank(message = "Le jeton QR scanné est requis") String qrToken,
        String source,
        /** Cliché caméra au moment du scan, encodé en base64 (data URL ou brut) — preuve anti-fraude,
         * absent pour la saisie manuelle du jeton (pas d'image caméra disponible dans ce cas). */
        String photoBase64
) {
}
