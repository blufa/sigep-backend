package gn.departement.sigep.badges.application.port.out;

/** Modèle de présentation d'une carte, assemblé par le service à partir de l'employé/poste/badge/organisation. */
public record BadgeCardView(
        String matricule,
        String fullName,
        String positionTitle,
        String hierarchie,
        String qrToken,
        String ministryName,
        String serviceName,
        String city,
        /** Sigle du tenant (ex. "SNIESM") — affiché seul au verso du badge. */
        String organizationCode
) {
}
