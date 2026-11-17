package gn.departement.sigep.staff.adapters.out.pdf;

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
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.departement.sigep.platform.domain.OrganizationLogo;
import gn.departement.sigep.staff.application.port.out.EmployeeReportRenderer;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;

/**
 * Rapport PDF des employés (tableau), format A4 paysage — mêmes codes visuels que le badge
 * (en-tête ministère/service/logo, vert institutionnel) mais en mise en page tabulaire classique,
 * pensé pour être imprimé/archivé plutôt que découpé.
 */
@Component
public class EmployeeReportPdfGenerator implements EmployeeReportRenderer {

    private static final Color ACCENT = new Color(0x0B, 0x3D, 0x2E);
    private static final Color HEADER_TEXT = Color.WHITE;
    private static final Color ROW_ALT = new Color(0xF4, 0xF6, 0xF7);
    private static final Color BORDER = new Color(0xDD, 0xE2, 0xE6);
    private static final Color INK = new Color(0x13, 0x20, 0x2B);
    private static final Color MUTED = new Color(0x5B, 0x66, 0x70);

    private static final String DEFAULT_MINISTRY_NAME =
            "MINISTÈRE DE L'ÉDUCATION NATIONALE, DE L'ENSEIGNEMENT TECHNIQUE, "
                    + "DE LA FORMATION PROFESSIONNELLE ET DE L'ALPHABÉTISATION";
    private static final String DEFAULT_SERVICE_NAME =
            "SERVICE NATIONAL DES INFRASTRUCTURES, ÉQUIPEMENTS SCOLAIRES ET MAINTENANCE";

    @Override
    public byte[] render(List<EmployeeReportRow> rows, String ministryName, String serviceName, OrganizationLogo logo) {
        try {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(header(ministryName, serviceName, logo));
            document.add(table(rows));
            document.add(footer(rows.size()));

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer le rapport PDF des employés", e);
        }
    }

    private PdfPTable header(String ministryName, String serviceName, OrganizationLogo logo) {
        boolean hasLogo = logo != null && logo.content() != null;
        PdfPTable header = new PdfPTable(hasLogo ? 2 : 1);
        header.setWidthPercentage(100);
        if (hasLogo) {
            header.setWidths(new float[]{1f, 7f});
            try {
                Image logoImage = Image.getInstance(logo.content());
                logoImage.scaleToFit(42f, 42f);
                PdfPCell logoCell = new PdfPCell(logoImage, false);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                header.addCell(logoCell);
            } catch (Exception ignored) {
                PdfPCell empty = new PdfPCell();
                empty.setBorder(Rectangle.NO_BORDER);
                header.addCell(empty);
            }
        }

        Font ministryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, INK);
        Font serviceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, ACCENT);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, ACCENT);

        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph ministry = new Paragraph(
                (ministryName != null && !ministryName.isBlank() ? ministryName : DEFAULT_MINISTRY_NAME).toUpperCase(), ministryFont);
        ministry.setSpacingAfter(2f);
        textCell.addElement(ministry);

        Paragraph service = new Paragraph(
                (serviceName != null && !serviceName.isBlank() ? serviceName : DEFAULT_SERVICE_NAME).toUpperCase(), serviceFont);
        service.setSpacingAfter(8f);
        textCell.addElement(service);

        textCell.addElement(new Paragraph("Rapport du personnel", titleFont));
        header.addCell(textCell);

        header.setSpacingAfter(14f);
        return header;
    }

    private PdfPTable table(List<EmployeeReportRow> rows) {
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.6f, 1.3f, 1.6f, 1.6f, 1f, 2f, 1.3f, 1.2f, 0.9f});

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, HEADER_TEXT);
        for (String label : new String[]{
                "N°", "Matricule", "Nom", "Prénom", "Genre", "Poste", "Hiérarchie", "Téléphone", "Statut"}) {
            PdfPCell cell = new PdfPCell(new Phrase(label, headFont));
            cell.setBackgroundColor(ACCENT);
            cell.setBorderColor(ACCENT);
            cell.setPadding(6f);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
        table.setHeaderRows(1);

        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, INK);
        Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, MUTED);
        Font activeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, new Color(0x03, 0x7A, 0x47));
        Font inactiveFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, new Color(0xB4, 0x23, 0x18));

        int index = 1;
        for (EmployeeReportRow row : rows) {
            Color background = index % 2 == 0 ? ROW_ALT : Color.WHITE;
            addCell(table, String.valueOf(index), cellFont, background);
            addCell(table, row.matricule(), cellFont, background);
            addCell(table, row.lastName() != null ? row.lastName().toUpperCase() : "—", cellFont, background);
            addCell(table, row.firstName() != null ? row.firstName() : "—", cellFont, background);
            addCell(table, row.genreLabel() != null ? row.genreLabel() : "—", mutedFont, background);
            addCell(table, row.positionTitle() != null ? row.positionTitle() : "—", cellFont, background);
            addCell(table, row.hierarchie() != null ? row.hierarchie() : "—", mutedFont, background);
            addCell(table, row.phone() != null && !row.phone().isBlank() ? row.phone() : "—", mutedFont, background);
            PdfPCell statusCell = new PdfPCell(new Phrase(row.active() ? "Actif" : "Inactif", row.active() ? activeFont : inactiveFont));
            statusCell.setBackgroundColor(background);
            statusCell.setBorderColor(BORDER);
            statusCell.setPadding(5f);
            table.addCell(statusCell);
            index++;
        }

        if (rows.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucun employé enregistré.", mutedFont));
            empty.setColspan(9);
            empty.setBorderColor(BORDER);
            empty.setPadding(10f);
            table.addCell(empty);
        }

        return table;
    }

    private void addCell(PdfPTable table, String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setBorderColor(BORDER);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private Paragraph footer(int total) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8f, MUTED);
        String generatedAt = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")
                .withZone(ZoneId.of("Africa/Conakry"))
                .format(java.time.Instant.now());
        Paragraph paragraph = new Paragraph(
                "Généré le " + generatedAt + " — " + total + " employé" + (total > 1 ? "s" : ""), font);
        paragraph.setSpacingBefore(10f);
        return paragraph;
    }
}
