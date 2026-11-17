package gn.departement.sigep.badges.adapters.in.web.dto;

import gn.departement.sigep.badges.application.port.out.PublicBadgeVerification;

public record PublicBadgeVerificationResponse(
        boolean valid,
        String matricule,
        String fullName,
        String positionTitle,
        String hierarchie,
        boolean hasPhoto
) {
    public static PublicBadgeVerificationResponse from(PublicBadgeVerification verification) {
        return new PublicBadgeVerificationResponse(
                verification.valid(),
                verification.matricule(),
                verification.fullName(),
                verification.positionTitle(),
                verification.hierarchie(),
                verification.hasPhoto());
    }
}
