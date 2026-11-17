package gn.departement.sigep.badges.application.port.in;

import gn.departement.sigep.badges.application.port.out.BadgeCardView;
import gn.departement.sigep.badges.application.port.out.PublicBadgeVerification;
import gn.departement.sigep.badges.domain.EmployeeBadge;
import gn.departement.sigep.staff.domain.EmployeePhoto;

import java.util.List;
import java.util.Optional;

public interface BadgeUseCases {

    /** Retourne le badge actif de l'employé, en émet un nouveau (jeton QR secret) si aucun n'existe encore. */
    EmployeeBadge getOrIssue(Long employeeId);

    EmployeeBadge findActiveByQrToken(String qrToken);

    /** Données de présentation de la carte (sans effet de bord), pour un rendu HTML/QR côté front. */
    BadgeCardView getCardView(Long employeeId);

    /** Génère le PDF de la carte badge d'un employé (émet le badge si besoin, marque l'impression). */
    byte[] generateCardPdf(Long employeeId);

    /** Génère une planche PDF regroupant plusieurs cartes badge. */
    byte[] generateSheetPdf(List<Long> employeeIds);

    /**
     * Vérification publique (sans authentification) d'un badge par son jeton QR — utilisée par la
     * page publique de contrôle. Ne renvoie que ce qui est déjà imprimé sur le badge physique.
     */
    PublicBadgeVerification verifyPublic(String qrToken);

    /** Photo de l'employé associé à un badge, pour la même page publique de contrôle. */
    Optional<EmployeePhoto> publicPhoto(String qrToken);

    /**
     * Vérification publique par matricule (saisie manuelle — le jeton QR n'est pas lisible par un
     * humain, seul le matricule est imprimé en clair sur le badge).
     */
    PublicBadgeVerification verifyPublicByMatricule(String matricule);

    Optional<EmployeePhoto> publicPhotoByMatricule(String matricule);
}
