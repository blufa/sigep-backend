package gn.departement.sigep.badges.application.port.out;

import java.util.List;

public interface BadgeCardRenderer {

    byte[] renderSingle(BadgeCardView card, CardPhoto photo, OrganizationLogo logo);

    byte[] renderSheet(List<CardWithPhoto> cards, OrganizationLogo logo);

    /** Logo de l'organisation (optionnel), identique pour toutes les cartes d'un même tenant. */
    record OrganizationLogo(byte[] content, String contentType) {

        public static final OrganizationLogo NONE = new OrganizationLogo(null, null);

        public boolean isPresent() {
            return content != null && content.length > 0;
        }
    }

    /** Photo d'identité de l'employé (optionnelle), propre à chaque carte. */
    record CardPhoto(byte[] content, String contentType) {

        public static final CardPhoto NONE = new CardPhoto(null, null);

        public boolean isPresent() {
            return content != null && content.length > 0;
        }
    }

    record CardWithPhoto(BadgeCardView card, CardPhoto photo) {
    }
}
