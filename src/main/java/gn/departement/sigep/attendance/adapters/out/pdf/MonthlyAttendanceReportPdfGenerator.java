package gn.departement.sigep.attendance.adapters.out.pdf;

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
import gn.departement.sigep.attendance.application.port.out.MonthlyAttendanceReportRenderer;
import gn.departement.sigep.platform.domain.OrganizationLogo;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Rapport PDF mensuel de présence (tableau), format A4 paysage — bilan de fin de mois par employé :
 * jours ouvrés, présences (à l'heure + en retard), absences, départs anticipés. Mêmes codes visuels
 * que le rapport employés ({@code EmployeeReportPdfGenerator}).
 */
@Component
public class MonthlyAttendanceReportPdfGenerator implements MonthlyAttendanceReportRenderer {

    private static final Color ACCENT = new Color(0x0B, 0x3D, 0x2E);
    private static final Color HEADER_TEXT = Color.WHITE;
    private static final Color ROW_ALT = new Color(0xF4, 0xF6, 0xF7);
    private static final Color BORDER = new Color(0xDD, 0xE2, 0xE6);
    private static final Color INK = new Color(0x13, 0x20, 0x2B);
    private static final Color MUTED = new Color(0x5B, 0x66, 0x70);
    private static final Color WARNING = new Color(0xB5, 0x4A, 0x08);
    private static final Color DANGER = new Color(0xB4, 0x23, 0x18);

    private static final String DEFAULT_MINISTRY_NAME =
            "MINISTÈRE DE L'ÉDUCATION NATIONALE, DE L'ENSEIGNEMENT TECHNIQUE, "
                    + "DE LA FORMATION PROFESSIONNELLE ET DE L'ALPHABÉTISATION";
    private static final String DEFAULT_SERVICE_NAME =
            "SERVICE NATIONAL DES INFRASTRUCTURES, ÉQUIPEMENTS SCOLAIRES ET MAINTENANCE";

    @Override
    public byte[] render(
            YearMonth month, List<MonthlyReportRow> rows, String ministryName, String serviceName, OrganizationLogo logo) {
        try {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(header(month, ministryName, serviceName, logo));
            document.add(table(rows));
            document.add(footer());

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer le rapport mensuel de présence", e);
        }
    }

    private PdfPTable header(YearMonth month, String ministryName, String serviceName, OrganizationLogo logo) {
        PdfPTable header = new PdfPTable(logo != null && logo.content() != null ? 2 : 1);
        header.setWidthPercentage(100);
        if (logo != null && logo.content() != null) {
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

        String monthLabel = month.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + month.getYear();
        textCell.addElement(new Paragraph("Rapport mensuel de présence — " + monthLabel, titleFont));
        header.addCell(textCell);

        header.setSpacingAfter(14f);
        return header;
    }

    private PdfPTable table(List<MonthlyReportRow> rows) {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.3f, 2f, 2f, 1f, 1.1f, 1f, 1.1f, 1.3f});

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, HEADER_TEXT);
        for (String label : new String[]{
                "Matricule", "Employé", "Poste", "Jours ouvrés", "Présences", "Retards", "Absences", "Départs anticipés"}) {
            PdfPCell cell = new PdfPCell(new Phrase(label, headFont));
            cell.setBackgroundColor(ACCENT);
            cell.setBorderColor(ACCENT);
            cell.setPadding(6f);
            table.addCell(cell);
        }
        table.setHeaderRows(1);

        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, INK);
        Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, MUTED);
        Font presentFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, new Color(0x03, 0x7A, 0x47));
        Font lateFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, WARNING);
        Font absentFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, DANGER);

        int index = 0;
        for (MonthlyReportRow row : rows) {
            Color background = index % 2 == 1 ? ROW_ALT : Color.WHITE;
            addCell(table, row.matricule(), cellFont, background);
            addCell(table, row.fullName(), cellFont, background);
            addCell(table, row.positionTitle() != null ? row.positionTitle() : "—", mutedFont, background);
            addCell(table, String.valueOf(row.workingDays()), mutedFont, background, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(row.presentDays()), presentFont, background, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(row.lateDays()), lateFont, background, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(row.absentDays()), absentFont, background, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(row.earlyDepartureDays()), lateFont, background, Element.ALIGN_CENTER);
            index++;
        }

        if (rows.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucun employé actif.", mutedFont));
            empty.setColspan(8);
            empty.setBorderColor(BORDER);
            empty.setPadding(10f);
            table.addCell(empty);
        }

        return table;
    }

    private void addCell(PdfPTable table, String text, Font font, Color background) {
        addCell(table, text, font, background, Element.ALIGN_LEFT);
    }

    private void addCell(PdfPTable table, String text, Font font, Color background, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setBorderColor(BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private Paragraph footer() {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8f, MUTED);
        Paragraph paragraph = new Paragraph(
                "Jours ouvrés = lundi à vendredi du mois, jusqu'à la date du jour si le mois est en cours. "
                        + "Un départ anticipé est une sortie enregistrée avant l'heure de descente configurée.", font);
        paragraph.setSpacingBefore(10f);
        return paragraph;
    }
}
