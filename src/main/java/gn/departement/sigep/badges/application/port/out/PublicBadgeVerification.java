package gn.departement.sigep.badges.application.port.out;

/**
 * Résultat d'une vérification publique de badge (sans authentification) — ne porte que les
 * informations déjà imprimées sur le badge physique (jamais le téléphone, jamais le tenant).
 */
public record PublicBadgeVerification(
        boolean valid,
        String matricule,
        String fullName,
        String positionTitle,
        String hierarchie,
        boolean hasPhoto
) {
    public static PublicBadgeVerification invalid() {
        return new PublicBadgeVerification(false, null, null, null, null, false);
    }
}
