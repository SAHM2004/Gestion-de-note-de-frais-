package com.ids.expense.report;

import com.ids.expense.auth.security.UserDetailsImpl;
import com.ids.expense.note_de_frais.ExpenseService;
import com.ids.expense.note_de_frais.response.ExpenseLineResponse;
import com.ids.expense.note_de_frais.response.ExpenseReportResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ExpenseService expenseService;

    public ByteArrayInputStream generateExpensePdf(Long reportId, UserDetailsImpl currentUser) {
        ExpenseReportResponse report = expenseService.getReportById(reportId, currentUser);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font Styles
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, java.awt.Color.BLUE);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, java.awt.Color.WHITE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.BLACK);
            Font warningFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.RED);

            // Document Header
            Paragraph title = new Paragraph("BORDEREAU DE NOTE DE FRAIS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);

            // Info Table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15);

            addInfoRow(infoTable, "Référence Note N° :", "#" + report.getId(), labelFont, valueFont);
            addInfoRow(infoTable, "Intitulé :", report.getTitle(), labelFont, valueFont);
            addInfoRow(infoTable, "Employé :", report.getEmployeeName(), labelFont, valueFont);
            addInfoRow(infoTable, "Département :", report.getEmployeeDepartmentName() != null ? report.getEmployeeDepartmentName() : "N/A", labelFont, valueFont);
            addInfoRow(infoTable, "Statut actuel :", report.getStatus().name(), labelFont, valueFont);
            addInfoRow(infoTable, "Période du :", (report.getDateFrom() != null ? report.getDateFrom().toString() : "-") + " au " + (report.getDateTo() != null ? report.getDateTo().toString() : "-"), labelFont, valueFont);

            document.add(infoTable);

            // Table of Lines
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 3f, 3f, 2f, 2f});
            table.setSpacingBefore(10);

            // Table Header
            String[] headers = {"Date", "Catégorie", "Description", "Montant", "Plafond"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(java.awt.Color.DARK_GRAY);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            BigDecimal total = BigDecimal.ZERO;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            if (report.getLines() != null && !report.getLines().isEmpty()) {
                for (ExpenseLineResponse line : report.getLines()) {
                    String dateStr = line.getExpenseDate() != null ? line.getExpenseDate().format(formatter) : "";
                    table.addCell(new Phrase(dateStr, valueFont));
                    table.addCell(new Phrase(line.getCategoryName() != null ? line.getCategoryName() : "-", valueFont));
                    table.addCell(new Phrase(line.getDescription() != null ? line.getDescription() : "-", valueFont));

                    BigDecimal amount = line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO;
                    total = total.add(amount);
                    PdfPCell amountCell = new PdfPCell(new Phrase(amount + " " + report.getCurrency(), valueFont));
                    amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(amountCell);

                    String ceilingStr = line.getCategoryMaxAmount() != null ? line.getCategoryMaxAmount() + " €" : "Sans limite";
                    PdfPCell ceilingCell = new PdfPCell(new Phrase(ceilingStr, Boolean.TRUE.equals(line.getIsOverCeiling()) ? warningFont : valueFont));
                    ceilingCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(ceilingCell);
                }
            }

            document.add(table);

            // Total Summary
            Paragraph totalParagraph = new Paragraph("TOTAL À REMBOURSER : " + total + " " + (report.getCurrency() != null ? report.getCurrency() : "EUR"),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, java.awt.Color.BLACK));
            totalParagraph.setAlignment(Element.ALIGN_RIGHT);
            totalParagraph.setSpacingBefore(15);
            totalParagraph.setSpacingAfter(20);
            document.add(totalParagraph);

            // Signatures block
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setSpacingBefore(30);

            PdfPCell c1 = new PdfPCell(new Phrase("Signature Employé :", labelFont));
            c1.setBorder(Rectangle.NO_BORDER);
            c1.setMinimumHeight(60);

            PdfPCell c2 = new PdfPCell(new Phrase("Validation & Signature Direction/Comptabilité :", labelFont));
            c2.setBorder(Rectangle.NO_BORDER);
            c2.setMinimumHeight(60);

            sigTable.addCell(c1);
            sigTable.addCell(c2);
            document.add(sigTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF de la note de frais", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateExpensesCsv(List<ExpenseReportResponse> reports) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(out);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("ID Note", "Titre", "Employé", "Département", "Statut", "Devise", "Date Début", "Date Fin", "Montant Total"))) {

            for (ExpenseReportResponse report : reports) {
                BigDecimal total = BigDecimal.ZERO;
                if (report.getLines() != null) {
                    for (ExpenseLineResponse l : report.getLines()) {
                        if (l.getAmount() != null) total = total.add(l.getAmount());
                    }
                }

                csvPrinter.printRecord(
                        report.getId(),
                        report.getTitle(),
                        report.getEmployeeName(),
                        report.getEmployeeDepartmentName(),
                        report.getStatus(),
                        report.getCurrency(),
                        report.getDateFrom(),
                        report.getDateTo(),
                        total
                );
            }
            csvPrinter.flush();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération de l'export CSV", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, labelFont));
        cLabel.setBorder(Rectangle.NO_BORDER);
        cLabel.setPadding(4);

        PdfPCell cValue = new PdfPCell(new Phrase(value != null ? value : "-", valueFont));
        cValue.setBorder(Rectangle.NO_BORDER);
        cValue.setPadding(4);

        table.addCell(cLabel);
        table.addCell(cValue);
    }
}
