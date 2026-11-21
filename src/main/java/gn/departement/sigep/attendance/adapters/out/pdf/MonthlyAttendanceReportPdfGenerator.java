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
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import gn.departement.sigep.attendance.application.port.out.MonthlyAttendanceReportRenderer;
import gn.departement.sigep.platform.domain.OrganizationLogo;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rapport PDF mensuel de présence, format A4 paysage — reproduit la maquette validée par le client
 * ("rapport-mensuelle.pdf") : en-tête avec référence/date d'édition, cartes de synthèse, tableau par
 * agent avec barre de taux colorée, répartition par poste, blocs de signature. Les sections nécessitant
 * des données non saisies dans sigep (direction/département, observations libres) ont été adaptées
 * (poste à la place de direction) ou retirées (observations) — voir échange avec le client.
 */
@Component
public class MonthlyAttendanceReportPdfGenerator implements MonthlyAttendanceReportRenderer {

    private static final Color ACCENT = new Color(0x0B, 0x3D, 0x2E);
    private static final Color INK = new Color(0x13, 0x20, 0x2B);
    private static final Color MUTED = new Color(0x5B, 0x66, 0x70);
    private static final Color BORDER = new Color(0xE2, 0xE5, 0xE8);
    private static final Color CARD_BG = new Color(0xFA, 0xFA, 0xFA);
    private static final Color TRACK_BG = new Color(0xEC, 0xEE, 0xF0);
    private static final Color GOOD = new Color(0x03, 0x7A, 0x47);
    private static final Color WARNING = new Color(0xB5, 0x8A, 0x04);
    private static final Color DANGER = new Color(0xB4, 0x23, 0x18);

    private static final String DEFAULT_MINISTRY_NAME =
            "Ministère de l'Éducation Nationale, de l'Enseignement Technique, "
                    + "de la Formation Professionnelle et de l'Alphabétisation";
    private static final String DEFAULT_SERVICE_NAME =
            "Service National des Infrastructures, Équipements Scolaires et Maintenance";

    @Override
    public byte[] render(
            java.time.YearMonth month, List<MonthlyReportRow> rows, String tenantCode,
            String ministryName, String serviceName, OrganizationLogo logo) {
        try {
            Document document = new Document(PageSize.A4.rotate(), 18, 18, 16, 16);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(header(month, rows, tenantCode, ministryName, serviceName, logo));
            document.add(statCards(rows));
            document.add(sectionTitle("Présence par agent"));
            document.add(agentTable(rows));
            document.add(byPositionSection(rows));
            document.add(signatureBlock());
            document.add(footer());

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer le rapport mensuel de présence", e);
        }
    }

    // ───────────────────────── En-tête ─────────────────────────

    private PdfPTable header(
            java.time.YearMonth month, List<MonthlyReportRow> rows, String tenantCode,
            String ministryName, String serviceName, OrganizationLogo logo) {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(9f);

        PdfPTable top = new PdfPTable(logo != null && logo.content() != null ? 3 : 2);
        top.setWidthPercentage(100);
        if (logo != null && logo.content() != null) {
            top.setWidths(new float[]{0.6f, 5f, 2.2f});
            try {
                Image logoImage = Image.getInstance(logo.content());
                logoImage.scaleToFit(34f, 34f);
                PdfPCell logoCell = new PdfPCell(logoImage, false);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setVerticalAlignment(Element.ALIGN_TOP);
                top.addCell(logoCell);
            } catch (Exception ignored) {
                PdfPCell empty = new PdfPCell();
                empty.setBorder(Rectangle.NO_BORDER);
                top.addCell(empty);
            }
        } else {
            top.setWidths(new float[]{5f, 2.2f});
        }

        Font republicFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, MUTED);
        Font ministryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, INK);
        Font serviceFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, MUTED);

        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        Paragraph republic = new Paragraph("RÉPUBLIQUE DE GUINÉE · Travail – Justice – Solidarité", republicFont);
        republic.setSpacingAfter(3f);
        textCell.addElement(republic);
        Paragraph ministry = new Paragraph(ministryName != null && !ministryName.isBlank() ? ministryName : DEFAULT_MINISTRY_NAME, ministryFont);
        ministry.setSpacingAfter(2f);
        textCell.addElement(ministry);
        textCell.addElement(new Paragraph(serviceName != null && !serviceName.isBlank() ? serviceName : DEFAULT_SERVICE_NAME, serviceFont));
        top.addCell(textCell);

        Font refLabelFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, MUTED);
        Font refValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, INK);
        String ref = (tenantCode != null && !tenantCode.isBlank() ? tenantCode : "SIGAP") + "/RP/"
                + month.getYear() + "-" + String.format("%02d", month.getMonthValue());
        String editedOn = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        PdfPCell refCell = new PdfPCell();
        refCell.setBorder(Rectangle.NO_BORDER);
        refCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph refLine = new Paragraph("Réf. ", refLabelFont);
        refLine.add(new Phrase(ref, refValueFont));
        refLine.setAlignment(Element.ALIGN_RIGHT);
        refLine.setSpacingAfter(2f);
        refCell.addElement(refLine);
        Paragraph editedLine = new Paragraph("Édité le ", refLabelFont);
        editedLine.add(new Phrase(editedOn, refValueFont));
        editedLine.setAlignment(Element.ALIGN_RIGHT);
        refCell.addElement(editedLine);
        top.addCell(refCell);

        outer.addCell(borderless(top));
        outer.addCell(borderless(coloredRule(2f)));

        PdfPTable titleRow = new PdfPTable(2);
        titleRow.setWidthPercentage(100);
        titleRow.setWidths(new float[]{4f, 3f});
        titleRow.setSpacingBefore(6f);

        Font eyebrowFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, ACCENT);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, INK);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        Paragraph eyebrow = new Paragraph("RAPPORT MENSUEL DE PRÉSENCE", eyebrowFont);
        eyebrow.setSpacingAfter(2f);
        titleCell.addElement(eyebrow);
        String monthLabel = capitalize(month.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)) + " " + month.getYear();
        titleCell.addElement(new Paragraph(monthLabel, titleFont));
        titleRow.addCell(titleCell);

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        int workingDays = rows.isEmpty() ? 0 : rows.get(0).workingDays();
        PdfPCell statsCell = new PdfPCell(miniStatsRow(
                "Période", monthStart.format(DateTimeFormatter.ofPattern("dd/MM")) + " – " + monthEnd.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                "Jours ouvrés", String.valueOf(workingDays),
                "Effectif", rows.size() + " agent" + (rows.size() > 1 ? "s" : "")));
        statsCell.setBorder(Rectangle.NO_BORDER);
        statsCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        titleRow.addCell(statsCell);

        outer.addCell(borderless(titleRow));
        return outer;
    }

    private PdfPTable miniStatsRow(String... labelsAndValues) {
        int count = labelsAndValues.length / 2;
        PdfPTable table = new PdfPTable(count);
        table.setWidthPercentage(100);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, INK);
        for (int i = 0; i < count; i++) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph label = new Paragraph(labelsAndValues[i * 2].toUpperCase(), labelFont);
            label.setAlignment(Element.ALIGN_RIGHT);
            label.setSpacingAfter(1f);
            cell.addElement(label);
            Paragraph value = new Paragraph(labelsAndValues[i * 2 + 1], valueFont);
            value.setAlignment(Element.ALIGN_RIGHT);
            cell.addElement(value);
            table.addCell(cell);
        }
        return table;
    }

    private PdfPCell coloredRule(float heightPt) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(ACCENT);
        cell.setFixedHeight(heightPt);
        cell.setPadding(0f);
        return cell;
    }

    // ───────────────────────── Cartes de synthèse ─────────────────────────

    private PdfPTable statCards(List<MonthlyReportRow> rows) {
        int workingDays = rows.isEmpty() ? 0 : rows.get(0).workingDays();
        double avgRate = rows.isEmpty() ? 0
                : rows.stream().mapToDouble(MonthlyReportRow::attendanceRate).average().orElse(0);
        int totalAbsences = rows.stream().mapToInt(MonthlyReportRow::absentDays).sum();
        int totalLate = rows.stream().mapToInt(MonthlyReportRow::lateDays).sum();
        long fullAttendance = rows.stream().filter(r -> r.absentDays() == 0).count();

        PdfPTable cards = new PdfPTable(4);
        cards.setWidthPercentage(100);
        cards.setSpacingAfter(10f);
        cards.addCell(statCard("TAUX DE PRÉSENCE", String.format(Locale.FRENCH, "%.1f %%", avgRate), rateColor(avgRate), "moyenne du service"));
        cards.addCell(statCard("JOURS D'ABSENCE", String.valueOf(totalAbsences), INK, "sur " + (workingDays * rows.size()) + " jours-agents"));
        cards.addCell(statCard("RETARDS", String.valueOf(totalLate), INK, "arrivées tardives"));
        cards.addCell(statCard("PRÉSENCE COMPLÈTE", fullAttendance + " / " + rows.size(), INK, "agents sans absence"));
        return cards;
    }

    private PdfPCell statCard(String label, String value, Color valueColor, String subtitle) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 19f, valueColor);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, MUTED);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(CARD_BG);
        cell.setBorderColor(BORDER);
        cell.setPadding(7f);
        Paragraph labelP = new Paragraph(label, labelFont);
        labelP.setSpacingAfter(4f);
        cell.addElement(labelP);
        Paragraph valueP = new Paragraph(value, valueFont);
        valueP.setSpacingAfter(2f);
        cell.addElement(valueP);
        cell.addElement(new Paragraph(subtitle, subtitleFont));
        return cell;
    }

    // ───────────────────────── Tableau par agent ─────────────────────────

    private Paragraph sectionTitle(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, INK);
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingAfter(8f);
        return paragraph;
    }

    private PdfPTable agentTable(List<MonthlyReportRow> rows) {
        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 2.6f, 1.4f, 1.8f, 0.8f, 0.8f, 0.8f, 1f, 0.9f, 1.8f});
        table.setSpacingAfter(10f);

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE);
        for (String label : new String[]{
                "N°", "Agent", "Matricule", "Poste", "Prés.", "Abs.", "Retards", "Dép. anti.", "Heures", "Taux"}) {
            PdfPCell cell = new PdfPCell(new Phrase(label, headFont));
            cell.setBackgroundColor(ACCENT);
            cell.setBorderColor(ACCENT);
            cell.setPadding(6f);
            table.addCell(cell);
        }
        table.setHeaderRows(1);

        Font numFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, MUTED);
        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, INK);
        Font hierarchieFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, MUTED);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, INK);
        Font mutedCellFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, MUTED);

        int rank = 1;
        int totalPresent = 0, totalAbsent = 0, totalLate = 0, totalEarly = 0, totalWorking = 0;
        double totalHours = 0;
        for (MonthlyReportRow row : rows) {
            addCell(table, String.valueOf(rank++), numFont, Element.ALIGN_CENTER);

            PdfPCell agentCell = new PdfPCell();
            agentCell.setBorderColor(BORDER);
            agentCell.setPadding(5f);
            agentCell.addElement(new Phrase(row.fullName(), nameFont));
            if (row.hierarchie() != null && !row.hierarchie().isBlank()) {
                agentCell.addElement(new Phrase(row.hierarchie(), hierarchieFont));
            }
            table.addCell(agentCell);

            addCell(table, row.matricule(), cellFont, Element.ALIGN_LEFT);
            addCell(table, row.positionTitle() != null ? row.positionTitle() : "—", mutedCellFont, Element.ALIGN_LEFT);
            addCell(table, String.valueOf(row.presentDays()), cellFont, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(row.absentDays()), row.absentDays() > 0 ? boldColored(DANGER) : mutedCellFont, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(row.lateDays()), row.lateDays() > 0 ? boldColored(WARNING) : mutedCellFont, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(row.earlyDepartureDays()), row.earlyDepartureDays() > 0 ? boldColored(WARNING) : mutedCellFont, Element.ALIGN_CENTER);
            addCell(table, String.format(Locale.FRENCH, "%.1f", row.totalHours()), cellFont, Element.ALIGN_CENTER);
            table.addCell(rateCell(row.attendanceRate()));

            totalPresent += row.presentDays();
            totalAbsent += row.absentDays();
            totalLate += row.lateDays();
            totalEarly += row.earlyDepartureDays();
            totalHours += row.totalHours();
            totalWorking += row.workingDays();
        }

        if (rows.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucun employé actif.", mutedCellFont));
            empty.setColspan(10);
            empty.setBorderColor(BORDER);
            empty.setPadding(10f);
            table.addCell(empty);
        } else {
            Font totalLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, INK);
            PdfPCell totalLabel = new PdfPCell(new Phrase("Total / moyenne", totalLabelFont));
            totalLabel.setColspan(4);
            totalLabel.setBackgroundColor(CARD_BG);
            totalLabel.setBorderColor(BORDER);
            totalLabel.setPadding(6f);
            table.addCell(totalLabel);
            addCell(table, String.valueOf(totalPresent), totalLabelFont, Element.ALIGN_CENTER, CARD_BG);
            addCell(table, String.valueOf(totalAbsent), totalLabelFont, Element.ALIGN_CENTER, CARD_BG);
            addCell(table, String.valueOf(totalLate), totalLabelFont, Element.ALIGN_CENTER, CARD_BG);
            addCell(table, String.valueOf(totalEarly), totalLabelFont, Element.ALIGN_CENTER, CARD_BG);
            addCell(table, String.format(Locale.FRENCH, "%.1f", totalHours), totalLabelFont, Element.ALIGN_CENTER, CARD_BG);
            double overallRate = totalWorking == 0 ? 0 : (totalPresent * 100.0) / totalWorking;
            Font rateFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, rateColor(overallRate));
            PdfPCell rateTotalCell = new PdfPCell(new Phrase(String.format(Locale.FRENCH, "%.1f %%", overallRate), rateFont));
            rateTotalCell.setBackgroundColor(CARD_BG);
            rateTotalCell.setBorderColor(BORDER);
            rateTotalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            rateTotalCell.setPadding(6f);
            table.addCell(rateTotalCell);
        }

        return table;
    }

    private Font boldColored(Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, color);
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment) {
        addCell(table, text, font, alignment, null);
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(alignment);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        table.addCell(cell);
    }

    /** Cellule "Taux" : pourcentage coloré + barre de progression dessinée via {@link PdfPCellEvent}
     * (coordonnées exactes de la cellule après mise en page) plutôt qu'une table imbriquée à largeurs
     * proportionnelles — cette dernière approche avait silencieusement déformé la hauteur des cartes
     * badge plus tôt (voir BadgeCardPdfGenerator) ; l'événement de cellule évite complètement ce risque. */
    private PdfPCell rateCell(double rate) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, rateColor(rate));
        PdfPCell cell = new PdfPCell(new Phrase(String.format(Locale.FRENCH, "%.0f %%", rate), font));
        cell.setBorderColor(BORDER);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(7f);
        cell.setPaddingLeft(5f);
        cell.setPaddingRight(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setCellEvent(new RateBarCellEvent(rate));
        return cell;
    }

    private static Color rateColor(double rate) {
        if (rate >= 95) {
            return GOOD;
        }
        if (rate >= 85) {
            return WARNING;
        }
        return DANGER;
    }

    /** Dessine une barre de progression fine en bas de la cellule "Taux" (track gris + remplissage
     * coloré proportionnel au taux). */
    private static final class RateBarCellEvent implements PdfPCellEvent {
        private final double rate;

        private RateBarCellEvent(double rate) {
            this.rate = rate;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[com.lowagie.text.pdf.PdfPTable.BACKGROUNDCANVAS];
            float inset = 5f;
            float barHeight = 3f;
            float x = position.getLeft() + inset;
            float width = position.getWidth() - 2 * inset;
            float y = position.getBottom() + 4f;

            canvas.setColorFill(TRACK_BG);
            canvas.roundRectangle(x, y, width, barHeight, 1.5f);
            canvas.fill();

            float fillWidth = (float) Math.max(2f, width * Math.min(100, rate) / 100.0);
            canvas.setColorFill(rateColor(rate));
            canvas.roundRectangle(x, y, fillWidth, barHeight, 1.5f);
            canvas.fill();
        }
    }

    // ───────────────────────── Répartition par poste ─────────────────────────

    private PdfPTable byPositionSection(List<MonthlyReportRow> rows) {
        PdfPTable section = new PdfPTable(1);
        section.setWidthPercentage(100);
        section.setSpacingAfter(8f);
        section.addCell(borderless(sectionTitleCell("Présence par poste")));

        Map<String, List<MonthlyReportRow>> byPosition = new LinkedHashMap<>();
        for (MonthlyReportRow row : rows) {
            String position = row.positionTitle() != null && !row.positionTitle().isBlank() ? row.positionTitle() : "Sans poste";
            byPosition.computeIfAbsent(position, k -> new java.util.ArrayList<>()).add(row);
        }

        List<Map.Entry<String, List<MonthlyReportRow>>> entries = new java.util.ArrayList<>(byPosition.entrySet());
        entries.sort(Comparator.comparingDouble(
                (Map.Entry<String, List<MonthlyReportRow>> e) ->
                        e.getValue().stream().mapToDouble(MonthlyReportRow::attendanceRate).average().orElse(0))
                .reversed());

        PdfPTable rowsTable = new PdfPTable(1);
        rowsTable.setWidthPercentage(100);
        for (Map.Entry<String, List<MonthlyReportRow>> entry : entries) {
            double avg = entry.getValue().stream().mapToDouble(MonthlyReportRow::attendanceRate).average().orElse(0);
            rowsTable.addCell(positionRow(entry.getKey(), entry.getValue().size(), avg));
        }
        section.addCell(borderless(rowsTable));
        return section;
    }

    private PdfPCell sectionTitleCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, INK);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(8f);
        return cell;
    }

    private PdfPCell positionRow(String position, int count, double rate) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, INK);
        Font countFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, MUTED);
        Font rateFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, rateColor(rate));

        PdfPTable line = new PdfPTable(2);
        line.setWidthPercentage(100);
        line.setWidths(new float[]{4f, 1f});
        PdfPCell labelCell = new PdfPCell();
        labelCell.setBorder(Rectangle.NO_BORDER);
        Paragraph label = new Paragraph(position, labelFont);
        label.add(new Phrase(" · " + count + " agent" + (count > 1 ? "s" : ""), countFont));
        labelCell.addElement(label);
        line.addCell(labelCell);
        PdfPCell rateTextCell = new PdfPCell(new Phrase(String.format(Locale.FRENCH, "%.1f %%", rate), rateFont));
        rateTextCell.setBorder(Rectangle.NO_BORDER);
        rateTextCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        line.addCell(rateTextCell);

        PdfPCell wrapper = new PdfPCell(line);
        wrapper.setBorder(Rectangle.NO_BORDER);
        // La barre colorée se dessine à une distance fixe (4pt) du bas de la cellule (voir
        // RateBarCellEvent) : une marge basse plus petite ferait chevaucher la barre et le texte.
        wrapper.setPaddingTop(3f);
        wrapper.setPaddingBottom(7f);
        wrapper.setCellEvent(new RateBarCellEvent(rate));
        return wrapper;
    }

    // ───────────────────────── Signatures & pied de page ─────────────────────────

    private PdfPTable signatureBlock() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setSpacingBefore(12f);
        table.setSpacingAfter(16f);
        table.addCell(signatureCell("Établi par", "Le Chef du personnel"));
        table.addCell(signatureCell("Visa", "Le Directeur Général Adjoint"));
        return table;
    }

    private PdfPCell signatureCell(String label, String role) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, MUTED);
        Font roleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, INK);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        Paragraph labelP = new Paragraph(label, labelFont);
        labelP.setSpacingAfter(2f);
        cell.addElement(labelP);
        Paragraph roleP = new Paragraph(role, roleFont);
        roleP.setSpacingAfter(18f);
        cell.addElement(roleP);

        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(90);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.TOP);
        lineCell.setBorderColor(BORDER);
        lineCell.setFixedHeight(1f);
        line.addCell(lineCell);
        cell.addElement(line);
        return cell;
    }

    private Paragraph footer() {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, MUTED);
        Paragraph paragraph = new Paragraph(
                "Données issues du pointage par badge QR · arrivée et départ. Jours ouvrés = lundi à vendredi du "
                        + "mois, jusqu'à la date du jour si le mois est en cours. Un départ anticipé est une sortie "
                        + "enregistrée avant l'heure de descente configurée.", font);
        paragraph.setSpacingBefore(4f);
        return paragraph;
    }

    private PdfPCell borderless(PdfPTable table) {
        PdfPCell cell = new PdfPCell(table);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        return cell;
    }

    private PdfPCell borderless(PdfPCell inner) {
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.addCell(inner);
        return borderless(wrapper);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(Locale.FRENCH) + text.substring(1);
    }
}
