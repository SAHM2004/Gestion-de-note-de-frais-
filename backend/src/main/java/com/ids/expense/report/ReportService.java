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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
            addInfoRow(infoTable, "Statut actuel :", getStatusLabelInFrench(report), labelFont, valueFont);
            addInfoRow(infoTable, "Période du :", (report.getDateFrom() != null ? report.getDateFrom().toString() : "-") + " au " + (report.getDateTo() != null ? report.getDateTo().toString() : "-"), labelFont, valueFont);

            document.add(infoTable);

            // Table of Lines
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 3f, 3f, 2f});
            table.setSpacingBefore(10);

            // Table Header
            String[] headers = {"Date", "Catégorie", "Description", "Montant"};
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
                    PdfPCell amountCell = new PdfPCell(new Phrase(formatAmount(amount) + " " + report.getCurrency(), valueFont));
                    amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(amountCell);
                }
            }

            document.add(table);

            // Total Summary
            Paragraph totalParagraph = new Paragraph("TOTAL À REMBOURSER : " + formatAmount(total) + " " + (report.getCurrency() != null ? report.getCurrency() : "EUR"),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, java.awt.Color.BLACK));
            totalParagraph.setAlignment(Element.ALIGN_RIGHT);
            totalParagraph.setSpacingBefore(15);
            totalParagraph.setSpacingAfter(10);
            document.add(totalParagraph);

            String amountInWords = convertNumberToWords(total.longValue()) + " " + getCurrencyInWords(report.getCurrency(), total);
            amountInWords = amountInWords.trim().substring(0, 1).toUpperCase() + amountInWords.trim().substring(1);
            Paragraph wordsParagraph = new Paragraph("Arrêtée la présente note de frais à la somme de : " + amountInWords,
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, java.awt.Color.BLACK));
            wordsParagraph.setAlignment(Element.ALIGN_LEFT);
            wordsParagraph.setSpacingBefore(5);
            wordsParagraph.setSpacingAfter(20);
            document.add(wordsParagraph);

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try {
            // Write UTF-8 BOM for Microsoft Excel / Calc compatibility
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(';')
                          .withHeader("ID Note", "Titre Note", "Employé", "Département", "Statut", "Date Dépense", "Catégorie", "Description Dépense", "Montant Dépense", "Devise"))) {

                for (ExpenseReportResponse report : reports) {
                    if (report.getLines() != null && !report.getLines().isEmpty()) {
                        for (ExpenseLineResponse line : report.getLines()) {
                            csvPrinter.printRecord(
                                    report.getId(),
                                    report.getTitle(),
                                    report.getEmployeeName(),
                                    report.getEmployeeDepartmentName() != null ? report.getEmployeeDepartmentName() : "N/A",
                                    getStatusLabelInFrench(report),
                                    line.getExpenseDate() != null ? line.getExpenseDate().format(formatter) : "",
                                    line.getCategoryName() != null ? line.getCategoryName() : "N/A",
                                    line.getDescription() != null ? line.getDescription() : "",
                                    line.getAmount() != null ? line.getAmount() : "0.00",
                                    report.getCurrency() != null ? report.getCurrency() : "EUR"
                             );
                        }
                    } else {
                        // Header record if no line present
                        csvPrinter.printRecord(
                                report.getId(),
                                report.getTitle(),
                                report.getEmployeeName(),
                                report.getEmployeeDepartmentName() != null ? report.getEmployeeDepartmentName() : "N/A",
                                getStatusLabelInFrench(report),
                                report.getDateFrom() != null ? report.getDateFrom().format(formatter) : "",
                                "N/A",
                                report.getDescription() != null ? report.getDescription() : "",
                                "0.00",
                                report.getCurrency() != null ? report.getCurrency() : "EUR"
                        );
                    }
                }
                csvPrinter.flush();
            }
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

    private String getStatusLabelInFrench(ExpenseReportResponse report) {
        if (report.getStatus() == null) return "-";
        switch (report.getStatus()) {
            case DRAFT:
                return "Brouillon";
            case IN_PROGRESS:
                String step = report.getCurrentStepName() != null ? " (" + report.getCurrentStepName() + ")" : "";
                return "En cours de validation" + step;
            case APPROVED:
                return "Approuvé";
            case PAID:
                return "Remboursé";
            case REJECTED:
                String rejectedStep = report.getRejectedAtStepName() != null ? " (à l'étape : " + report.getRejectedAtStepName() + ")" : "";
                return "Rejeté" + rejectedStep;
            default:
                return report.getStatus().name();
        }
    }

    private String getCurrencyInWords(String currency, BigDecimal amount) {
        if (currency == null) return "";
        boolean plural = amount.compareTo(BigDecimal.ONE) > 0;
        switch (currency.toUpperCase()) {
            case "FCFA":
            case "XOF":
            case "XAF":
                return "Francs CFA";
            case "EUR":
            case "EURO":
                return plural ? "Euros" : "Euro";
            case "USD":
            case "DOLLAR":
                return plural ? "Dollars" : "Dollar";
            default:
                return currency;
        }
    }

    private static final String[] UNITS = {
        "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf"
    };
    
    private static final String[] TENS = {
        "", "dix", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante-dix", "quatre-vingt", "quatre-vingt-dix"
    };

    private String convertNumberToWords(long number) {
        if (number == 0) {
            return "zéro";
        }
        return convertHelper(number).trim().replaceAll("\\s+", " ");
    }

    private String convertHelper(long number) {
        if (number < 0) {
            return "moins " + convertHelper(-number);
        }
        
        if (number < 10) {
            return UNITS[(int) number];
        }
        
        if (number < 20) {
            switch ((int) number) {
                case 10: return "dix";
                case 11: return "onze";
                case 12: return "douze";
                case 13: return "treize";
                case 14: return "quatorze";
                case 15: return "quinze";
                case 16: return "seize";
                default: return "dix-" + UNITS[(int) (number % 10)];
            }
        }
        
        if (number < 100) {
            int ten = (int) (number / 10);
            int unit = (int) (number % 10);
            
            if (ten == 7) {
                if (unit == 1) return "soixante et onze";
                return "soixante-" + convertHelper(10 + unit);
            }
            if (ten == 9) {
                return "quatre-vingt-" + convertHelper(10 + unit);
            }
            if (unit == 0) {
                if (ten == 8) return "quatre-vingts";
                return TENS[ten];
            }
            if (unit == 1) {
                return TENS[ten] + " et un";
            }
            return TENS[ten] + "-" + UNITS[unit];
        }
        
        if (number < 1000) {
            int hundred = (int) (number / 100);
            int remainder = (int) (number % 100);
            String hundredStr = hundred == 1 ? "cent" : UNITS[hundred] + " cent";
            if (hundred > 1 && remainder == 0) {
                hundredStr += "s";
            }
            if (remainder == 0) {
                return hundredStr;
            }
            return hundredStr + " " + convertHelper(remainder);
        }
        
        if (number < 1000000) {
            long thousand = number / 1000;
            long remainder = number % 1000;
            String thousandStr = thousand == 1 ? "mille" : convertHelper(thousand) + " mille";
            if (remainder == 0) {
                return thousandStr;
            }
            return thousandStr + " " + convertHelper(remainder);
        }
        
        if (number < 1000000000L) {
            long million = number / 1000000;
            long remainder = number % 1000000;
            String millionStr = million == 1 ? "un million" : convertHelper(million) + " millions";
            if (remainder == 0) {
                return millionStr;
            }
            return millionStr + " " + convertHelper(remainder);
        }
        
        long milliard = number / 1000000000L;
        long remainder = number % 1000000000L;
        String milliardStr = milliard == 1 ? "un milliard" : convertHelper(milliard) + " milliards";
        if (remainder == 0) {
            return milliardStr;
        }
        return milliardStr + " " + convertHelper(remainder);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        java.text.DecimalFormat df;
        if (amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            df = new java.text.DecimalFormat("#,##0", symbols);
        } else {
            symbols.setDecimalSeparator(',');
            df = new java.text.DecimalFormat("#,##0.00", symbols);
        }
        return df.format(amount);
    }
}
