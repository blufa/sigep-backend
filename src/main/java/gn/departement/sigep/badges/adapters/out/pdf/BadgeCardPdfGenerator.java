package gn.departement.sigep.badges.adapters.out.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.departement.sigep.badges.application.port.out.BadgeCardRenderer;
import gn.departement.sigep.badges.application.port.out.BadgeCardView;
import gn.departement.sigep.common.pdf.QrCodeGenerator;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Rend la carte de badge employé en PDF (OpenPDF), gabarit officiel CR80
 * portrait (54 x 85.6 mm) validé par le client (voir sigep-ui/docs/Badge
 * Ministère.*) : liseré tricolore, en-tête ministère/service, identité,
 * QR code secret de pointage, pied "BADGE AGENT".
 *
 * Les tailles (police, marges) sont dérivées du gabarit source (canevas
 * 540x856px = 54x85.6mm) via le facteur PX ci-dessous, pour reproduire
 * fidèlement les proportions sur une vraie carte CR80 — d'où des corps de
 * police très réduits (3-8pt), normaux sur un badge imprimé à cette taille.
 */
@Component
public class BadgeCardPdfGenerator implements BadgeCardRenderer {

    private static final float MM = 2.83465f;
    private static final float CARD_WIDTH = 54f * MM;
    private static final float CARD_HEIGHT = 85.6f * MM;
    /** points par pixel du gabarit source (canevas 540px de large = CARD_WIDTH). */
    private static final float PX = CARD_WIDTH / 540f;
    private static final int SHEET_COLUMNS = 3;

    private static final Color ACCENT = new Color(0x0B, 0x3D, 0x2E);
    private static final Color FLAG_RED = new Color(0xCE, 0x11, 0x26);
    private static final Color FLAG_YELLOW = new Color(0xFC, 0xD1, 0x16);
    private static final Color FLAG_GREEN = new Color(0x00, 0x94, 0x60);
    private static final Color MUTED = new Color(0x5B, 0x66, 0x70);
    private static final Color INK = new Color(0x13, 0x20, 0x2B);
    private static final Color PHOTO_BG = new Color(0xF1, 0xF3, 0xF5);
    private static final Color PANEL_BG = new Color(0xF4, 0xF6, 0xF7);

    private final QrCodeGenerator qrCodeGenerator;

    public BadgeCardPdfGenerator(QrCodeGenerator qrCodeGenerator) {
        this.qrCodeGenerator = qrCodeGenerator;
    }

    @Override
    public byte[] renderSingle(BadgeCardView card, CardPhoto photo, OrganizationLogo logo) {
        try {
            Document document = new Document(new Rectangle(CARD_WIDTH, CARD_HEIGHT), 0, 0, 0, 0);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            // Écriture directe à coordonnées absolues (comme renderSheet, voir son commentaire) plutôt
            // que document.add(table) : ce dernier ajoute une marge/interligne implicite (spacing par
            // défaut du pipeline de mise en page de Document) qui suffisait à dépasser CARD_HEIGHT une
            // fois le pied de carte calé pile sur le bord bas (voir le correctif d'espaceur dans
            // buildCard) — le pied de carte se retrouvait alors rejeté seul sur une 2e page, invisible
            // (page blanche) tant qu'on ne compte pas les pages du PDF généré.
            buildCard(card, photo, logo, CARD_WIDTH).writeSelectedRows(0, -1, 0, CARD_HEIGHT, writer.getDirectContent());
            writer.setPageEmpty(false);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer le PDF du badge", e);
        }
    }

    /** Marge de page et espacement entre cartes, planche (grille d'impression sur bristol A4). */
    private static final float SHEET_MARGIN = 20f;
    private static final float SHEET_GAP = 12f;

    /**
     * Grille de cartes sur A4 : chaque carte est écrite à sa position absolue
     * ({@link PdfPTable#writeSelectedRows}) sur le contenu direct de la page, plutôt
     * qu'imbriquée comme cellule d'une table englobante. Imbriquer {@code buildCard(...)}
     * (table à largeur verrouillée sur {@code CARD_WIDTH}) dans une {@link PdfPCell} d'une
     * table à {@code setWidthPercentage(100)} compressait silencieusement sa hauteur
     * calculée (mesuré : 64,5mm au lieu de 85,6mm pour un contenu identique à
     * {@link #renderSingle}, carte visiblement "aplatie" sur la planche) — un travers connu
     * d'OpenPDF/iText sur le calcul de hauteur des tables imbriquées. L'écriture directe à
     * coordonnées absolues réutilise exactement le même rendu que {@link #renderSingle},
     * déjà validé à la taille CR80 exacte.
     */
    @Override
    public byte[] renderSheet(List<CardWithPhoto> cards, OrganizationLogo logo) {
        try {
            Document document = new Document(PageSize.A4, SHEET_MARGIN, SHEET_MARGIN, SHEET_MARGIN, SHEET_MARGIN);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte canvas = writer.getDirectContent();

            float pageHeight = PageSize.A4.getHeight();
            int rowsPerPage = Math.max(1, (int) Math.floor((pageHeight - 2 * SHEET_MARGIN + SHEET_GAP) / (CARD_HEIGHT + SHEET_GAP)));
            int perPage = SHEET_COLUMNS * rowsPerPage;

            for (int i = 0; i < cards.size(); i++) {
                int positionOnPage = i % perPage;
                if (i > 0 && positionOnPage == 0) {
                    document.newPage();
                }
                int col = positionOnPage % SHEET_COLUMNS;
                int row = positionOnPage / SHEET_COLUMNS;
                float x = SHEET_MARGIN + col * (CARD_WIDTH + SHEET_GAP);
                float yTop = pageHeight - SHEET_MARGIN - row * (CARD_HEIGHT + SHEET_GAP);

                CardWithPhoto card = cards.get(i);
                PdfPTable cardTable = buildCard(card.card(), card.photo(), logo, CARD_WIDTH);
                cardTable.writeSelectedRows(0, -1, x, yTop, canvas);
                writer.setPageEmpty(false);
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer la planche de badges", e);
        }
    }

    /**
     * Les cellules du corps (en-tête, identité, panneau QR) ont une hauteur AUTO (dérivée du texte),
     * pas verrouillée comme la largeur — leur somme ne totalise naturellement qu'environ 60mm sur les
     * 85,6mm visés. Sans correctif, le pied "BADGE AGENT" s'arrête donc à 60mm au lieu de border le
     * bas réel de la carte : invisible sur la planche PDF seule (la page fait bien 85,6mm, mais 25mm
     * de blanc restent sous le pied, mesuré via rendu PNG), flagrant sur une carte PVC réelle ou une
     * planche aux cartes serrées (le bandeau de pied ne touche jamais le bord physique de la carte) —
     * c'est la "déformation" remontée. Un espaceur de hauteur calculée (reste exact avant le pied)
     * comble le vide, équivalent du `<div className="flex-1" />` utilisé côté front avant le pied de
     * carte (voir sigep-ui/components/cards/id-badge.tsx et lacaeducation-ui/id-badge-portrait.tsx).
     */
    private PdfPTable buildCard(BadgeCardView card, CardPhoto photo, OrganizationLogo logo, float width) {
        PdfPTable card_ = new PdfPTable(1);
        card_.setTotalWidth(width);
        card_.setLockedWidth(true);

        card_.addCell(flagStripeRow(width));
        card_.addCell(headerCell(card, logo));
        card_.addCell(identityCell(card, photo));
        card_.addCell(qrPanelCell(card));

        float footerHeight = 50f * PX;
        // Marge de sécurité (1pt) contre l'arrondi flottant des hauteurs de ligne calculées plus haut :
        // un espaceur pile à la limite peut faire déborder le pied de carte d'une fraction de point,
        // le rejetant entièrement sur une page PDF supplémentaire (voir renderSingle).
        float spacerHeight = CARD_HEIGHT - card_.getTotalHeight() - footerHeight - 1f;
        if (spacerHeight > 0f) {
            PdfPCell spacer = new PdfPCell();
            spacer.setBorder(Rectangle.NO_BORDER);
            spacer.setFixedHeight(spacerHeight);
            card_.addCell(spacer);
        }

        card_.addCell(footerCell(card));

        return card_;
    }

    private PdfPCell flagStripeRow(float width) {
        PdfPTable stripe = new PdfPTable(3);
        stripe.setTotalWidth(width);
        stripe.setLockedWidth(true);
        stripe.addCell(coloredCell(FLAG_RED));
        stripe.addCell(coloredCell(FLAG_YELLOW));
        stripe.addCell(coloredCell(FLAG_GREEN));

        PdfPCell wrapper = new PdfPCell(stripe);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0f);
        return wrapper;
    }

    private PdfPCell coloredCell(Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(color);
        cell.setFixedHeight(12f * PX);
        return cell;
    }

    private static final String DEFAULT_MINISTRY_NAME =
            "MINISTÈRE DE L'ÉDUCATION NATIONALE, DE L'ENSEIGNEMENT TECHNIQUE, "
                    + "DE LA FORMATION PROFESSIONNELLE ET DE L'ALPHABÉTISATION";
    private static final String DEFAULT_SERVICE_NAME =
            "SERVICE NATIONAL DES INFRASTRUCTURES, ÉQUIPEMENTS SCOLAIRES ET MAINTENANCE";

    private PdfPCell headerCell(BadgeCardView card, OrganizationLogo logo) {
        Font countryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f * PX, INK);
        Font mottoFont = FontFactory.getFont(FontFactory.HELVETICA, 11f * PX, MUTED);
        Font ministryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f * PX, INK);
        Font serviceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f * PX, ACCENT);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(19f * PX);
        cell.setPaddingBottom(9f * PX);
        cell.setPaddingLeft(20f * PX);
        cell.setPaddingRight(20f * PX);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph country = new Paragraph("RÉPUBLIQUE DE GUINÉE", countryFont);
        country.setAlignment(Element.ALIGN_CENTER);

        Paragraph motto = new Paragraph("Travail – Justice – Solidarité", mottoFont);
        motto.setAlignment(Element.ALIGN_CENTER);
        motto.setSpacingAfter(5f * PX);

        if (logo.isPresent()) {
            PdfPTable identityRow = new PdfPTable(2);
            try {
                identityRow.setWidths(new float[]{1f, 2.6f});
                Image logoImage = Image.getInstance(logo.content());
                logoImage.scaleToFit(85f * PX, 85f * PX);
                PdfPCell logoCell = new PdfPCell(logoImage, false);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                identityRow.addCell(logoCell);

                PdfPCell textCell = new PdfPCell();
                textCell.setBorder(Rectangle.NO_BORDER);
                textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                textCell.addElement(country);
                textCell.addElement(motto);
                identityRow.addCell(textCell);
                cell.addElement(identityRow);
            } catch (Exception e) {
                cell.addElement(country);
                cell.addElement(motto);
            }
        } else {
            cell.addElement(country);
            cell.addElement(motto);
        }

        String ministryName = card.ministryName() != null ? card.ministryName().toUpperCase() : DEFAULT_MINISTRY_NAME;
        String serviceName = card.serviceName() != null ? card.serviceName().toUpperCase() : DEFAULT_SERVICE_NAME;

        Paragraph ministry = new Paragraph(ministryName, ministryFont);
        ministry.setAlignment(Element.ALIGN_CENTER);
        ministry.setSpacingAfter(5f * PX);
        cell.addElement(ministry);

        Paragraph service = new Paragraph(serviceName, serviceFont);
        service.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(service);

        return cell;
    }

    private PdfPCell identityCell(BadgeCardView card, CardPhoto photo) {
        PdfPTable identity = new PdfPTable(2);
        identity.setWidths(new float[]{1f, 1.5f});

        PdfPCell photoCell = new PdfPCell();
        photoCell.setBackgroundColor(PHOTO_BG);
        photoCell.setBorderColor(ACCENT);
        photoCell.setBorderWidth(1.5f);
        photoCell.setFixedHeight(270f * PX);
        photoCell.setPadding(0f);
        if (photo.isPresent()) {
            try {
                Image photoImage = Image.getInstance(cropToCover(photo.content(), 221, 270));
                photoImage.scaleAbsolute(221f * PX, 270f * PX);
                photoCell.addElement(photoImage);
            } catch (Exception ignored) {
                // garde la case vide (fond uni) si l'image ne peut pas être décodée
            }
        }
        identity.addCell(photoCell);

        Font firstNameFont = FontFactory.getFont(FontFactory.HELVETICA, 20f * PX, MUTED);
        Font lastNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24f * PX, INK);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f * PX, MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f * PX, INK);

        String[] nameParts = splitFullName(card.fullName());

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setPaddingLeft(10f * PX);
        infoCell.setPaddingTop(4f * PX);
        infoCell.addElement(new Phrase(nameParts[0], firstNameFont));
        Paragraph lastName = new Paragraph(nameParts[1].toUpperCase(), lastNameFont);
        // Sépare davantage "FONCTION" du nom — demande explicite : "faire descendre un peu
        // FONCTION Directeur Général" (même ajustement que id-badge.tsx côté front).
        lastName.setSpacingAfter(21f * PX);
        infoCell.addElement(lastName);

        infoCell.addElement(new Phrase("FONCTION", labelFont));
        Paragraph fonctionValue = new Paragraph(card.positionTitle() != null ? card.positionTitle() : "—", valueFont);
        infoCell.addElement(fonctionValue);
        identity.addCell(infoCell);

        PdfPCell wrapper = new PdfPCell(identity);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPaddingLeft(20f * PX);
        wrapper.setPaddingRight(20f * PX);
        wrapper.setPaddingTop(4f * PX);
        return wrapper;
    }

    private PdfPCell qrPanelCell(BadgeCardView card) {
        PdfPTable panel = new PdfPTable(2);
        panel.setWidths(new float[]{1f, 1.3f});

        Image qrImage = qrCodeGenerator.generatePdfImage(card.qrToken(), 200);
        qrImage.scaleToFit(187f * PX, 187f * PX);
        PdfPCell qrCell = new PdfPCell(qrImage, false);
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setBackgroundColor(Color.WHITE);
        qrCell.setPadding(6f * PX);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        panel.addCell(qrCell);

        Font instructionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f * PX, INK);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f * PX, MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f * PX, INK);
        Font matriculeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 13f * PX, INK);

        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setPaddingLeft(10f * PX);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph instruction = new Paragraph("Scanner à l'arrivée et au départ", instructionFont);
        instruction.setSpacingAfter(6f * PX);
        textCell.addElement(instruction);
        textCell.addElement(new Phrase("MATRICULE", labelFont));
        Paragraph matriculeValue = new Paragraph(card.matricule(), matriculeFont);
        matriculeValue.setSpacingAfter(6f * PX);
        textCell.addElement(matriculeValue);
        textCell.addElement(new Phrase("HIÉRARCHIE", labelFont));
        textCell.addElement(new Phrase(card.hierarchie() != null ? card.hierarchie() : "—", valueFont));
        panel.addCell(textCell);

        PdfPCell wrapper = new PdfPCell(panel);
        wrapper.setBackgroundColor(PANEL_BG);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPaddingLeft(20f * PX);
        wrapper.setPaddingRight(20f * PX);
        wrapper.setPaddingTop(22f * PX);
        wrapper.setPaddingBottom(22f * PX);
        return wrapper;
    }

    private PdfPCell footerCell(BadgeCardView card) {
        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f * PX, Color.WHITE);
        Font cityFont = FontFactory.getFont(FontFactory.HELVETICA, 11f * PX, Color.WHITE);

        PdfPCell title = new PdfPCell(new Phrase("BADGE AGENT", titleFont));
        title.setBorder(Rectangle.NO_BORDER);
        title.setBackgroundColor(ACCENT);
        title.setVerticalAlignment(Element.ALIGN_MIDDLE);
        title.setPaddingLeft(20f * PX);
        title.setFixedHeight(50f * PX);
        footer.addCell(title);

        String city = card.city() != null && !card.city().isBlank() ? card.city() : "";
        PdfPCell cityCell = new PdfPCell(new Phrase(city, cityFont));
        cityCell.setBorder(Rectangle.NO_BORDER);
        cityCell.setBackgroundColor(ACCENT);
        cityCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cityCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cityCell.setPaddingRight(20f * PX);
        cityCell.setFixedHeight(50f * PX);
        footer.addCell(cityCell);

        PdfPCell wrapper = new PdfPCell(footer);
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(0f);
        return wrapper;
    }

    /**
     * Recadre la photo au centre pour correspondre exactement au ratio largeur/hauteur visé (comme
     * CSS {@code object-fit: cover}), avant de la redimensionner — {@link Image#scaleAbsolute} seul
     * étire l'image pour remplir la case sans respecter son ratio d'origine, déformant tout portrait
     * dont l'aspect ratio diffère de la case cible.
     */
    private byte[] cropToCover(byte[] source, int targetWidth, int targetHeight) throws java.io.IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(source));
        if (original == null) {
            return source;
        }
        float targetRatio = (float) targetWidth / targetHeight;
        float sourceRatio = (float) original.getWidth() / original.getHeight();

        int cropWidth;
        int cropHeight;
        if (sourceRatio > targetRatio) {
            cropHeight = original.getHeight();
            cropWidth = Math.round(cropHeight * targetRatio);
        } else {
            cropWidth = original.getWidth();
            cropHeight = Math.round(cropWidth / targetRatio);
        }
        int x = (original.getWidth() - cropWidth) / 2;
        int y = (original.getHeight() - cropHeight) / 2;

        BufferedImage cropped = original.getSubimage(x, y, cropWidth, cropHeight);
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", out);
        return out.toByteArray();
    }

    private String[] splitFullName(String fullName) {
        String trimmed = fullName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace == -1) {
            return new String[]{"", trimmed};
        }
        return new String[]{trimmed.substring(0, lastSpace), trimmed.substring(lastSpace + 1)};
    }
}
