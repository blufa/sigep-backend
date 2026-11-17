package gn.departement.sigep.badges.adapters.in.web;

import gn.departement.sigep.badges.adapters.in.web.dto.PublicBadgeVerificationResponse;
import gn.departement.sigep.badges.application.port.in.BadgeUseCases;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.staff.domain.EmployeePhoto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vérification publique d'un badge, sans authentification — n'importe qui (sécurité, visiteur)
 * scanne le QR ou saisit le jeton et obtient uniquement ce qui est déjà imprimé sur le badge
 * physique (nom, fonction, photo). Jamais d'accès à l'admin SIGEP depuis ce endpoint.
 */
@RestController
@RequestMapping("/api/public/badges")
public class PublicBadgeController {

    private final BadgeUseCases useCases;

    public PublicBadgeController(BadgeUseCases useCases) {
        this.useCases = useCases;
    }

    @GetMapping("/verify")
    public PublicBadgeVerificationResponse verify(@RequestParam String token) {
        return PublicBadgeVerificationResponse.from(useCases.verifyPublic(token));
    }

    @GetMapping("/verify/photo")
    public ResponseEntity<byte[]> photo(@RequestParam String token) {
        EmployeePhoto photo = useCases.publicPhoto(token).orElseThrow(() -> new NotFoundException("Aucune photo"));
        return photoResponse(photo);
    }

    @GetMapping("/verify-by-matricule")
    public PublicBadgeVerificationResponse verifyByMatricule(@RequestParam String matricule) {
        return PublicBadgeVerificationResponse.from(useCases.verifyPublicByMatricule(matricule));
    }

    @GetMapping("/verify-by-matricule/photo")
    public ResponseEntity<byte[]> photoByMatricule(@RequestParam String matricule) {
        EmployeePhoto photo = useCases.publicPhotoByMatricule(matricule)
                .orElseThrow(() -> new NotFoundException("Aucune photo"));
        return photoResponse(photo);
    }

    private ResponseEntity<byte[]> photoResponse(EmployeePhoto photo) {
        MediaType mediaType = photo.contentType() != null
                ? MediaType.parseMediaType(photo.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(photo.content());
    }
}
