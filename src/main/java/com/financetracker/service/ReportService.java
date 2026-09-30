package com.financetracker.service;

import com.financetracker.dao.TransactionDAO;
import com.financetracker.model.CategorySpend;
import com.financetracker.model.MonthlySummary;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Service for compiling financial reports and exporting them to CSV and PDF documents.
 */
public class ReportService {
    private final TransactionDAO transactionDAO;
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private String formatAmount(double amount) {
        return "₹" + String.format(Locale.US, "%,.2f", amount);
    }

    public ReportService() {
        this(new TransactionDAO());
    }

    public ReportService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    /**
     * Exports a list of transactions to a CSV file.
     *
     * @param targetFile destination file
     * @param transactions transactions list
     * @throws IOException on file write error
     */
    public void exportToCSV(File targetFile, List<Transaction> transactions) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            // Header
            writer.write("ID,Date,Type,Category,Amount,Description");
            writer.newLine();

            for (Transaction tx : transactions) {
                String desc = tx.getDescription() != null ? tx.getDescription().replace("\"", "\"\"") : "";
                String line = String.format("%d,%s,%s,\"%s\",%.2f,\"%s\"",
                        tx.getId(),
                        tx.getFormattedDate(),
                        tx.getType().name(),
                        tx.getCategoryName() != null ? tx.getCategoryName().replace("\"", "\"\"") : "",
                        tx.getAmount(),
                        desc
                );
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * Exports a comprehensive Financial Report to a professionally formatted PDF.
     *
     * @param targetFile output PDF file
     * @param user current logged in user
     * @param fromDate start date filter (optional)
     * @param toDate end date filter (optional)
     * @param transactions filtered transactions
     * @param categorySpends category breakdown
     * @throws IOException on file write error
     * @throws DocumentException on PDF generation error
     */
    public void exportToPDF(File targetFile, User user, LocalDate fromDate, LocalDate toDate,
                            List<Transaction> transactions, List<CategorySpend> categorySpends)
            throws IOException, DocumentException {

        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter.getInstance(document, new FileOutputStream(targetFile));
        document.open();

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(30, 41, 59));
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 41, 59));
        Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(51, 65, 85));
        Font boldCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(30, 41, 59));
        Font greenFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(46, 125, 50));
        Font redFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(198, 40, 40));
        Font blueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(21, 101, 192));

        // Header Title
        Paragraph title = new Paragraph("Personal Finance Tracker", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        document.add(title);

        Paragraph reportType = new Paragraph("Financial Statement & Transaction Summary",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(71, 85, 105)));
        document.add(reportType);

        String periodText = String.format("Period: %s to %s  |  Generated for: %s  |  Date: %s",
                fromDate != null ? fromDate.toString() : "All Time",
                toDate != null ? toDate.toString() : "Present",
                user != null ? user.getUsername() : "User",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        Paragraph meta = new Paragraph(periodText, subTitleFont);
        meta.setSpacingAfter(15f);
        document.add(meta);

        // Calculations
        double totalIncome = 0;
        double totalExpense = 0;
        double totalSavings = 0;
        for (Transaction tx : transactions) {
            if (tx.getType() == TransactionType.INCOME) totalIncome += tx.getAmount();
            else if (tx.getType() == TransactionType.EXPENSE) totalExpense += tx.getAmount();
            else if (tx.getType() == TransactionType.SAVINGS) totalSavings += tx.getAmount();
        }
        double netBalance = totalIncome - totalExpense - totalSavings;
        double savingsRate = totalIncome > 0 ? (totalSavings / totalIncome) * 100.0 : 0.0;

        // Executive Summary Metrics Table
        Paragraph summaryHeading = new Paragraph("1. Executive Financial Summary", sectionHeaderFont);
        summaryHeading.setSpacingBefore(10f);
        summaryHeading.setSpacingAfter(8f);
        document.add(summaryHeading);

        PdfPTable summaryTable = new PdfPTable(5);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{20f, 20f, 20f, 20f, 20f});

        addSummaryHeaderCell(summaryTable, "Total Income", new Color(46, 125, 50), tableHeaderFont);
        addSummaryHeaderCell(summaryTable, "Total Expenses", new Color(198, 40, 40), tableHeaderFont);
        addSummaryHeaderCell(summaryTable, "Total Savings", new Color(21, 101, 192), tableHeaderFont);
        addSummaryHeaderCell(summaryTable, "Net Balance", new Color(71, 85, 105), tableHeaderFont);
        addSummaryHeaderCell(summaryTable, "Savings Rate", new Color(109, 40, 217), tableHeaderFont);

        addSummaryValueCell(summaryTable, formatAmount(totalIncome), greenFont);
        addSummaryValueCell(summaryTable, formatAmount(totalExpense), redFont);
        addSummaryValueCell(summaryTable, formatAmount(totalSavings), blueFont);
        addSummaryValueCell(summaryTable, formatAmount(netBalance),
                netBalance >= 0 ? greenFont : redFont);
        addSummaryValueCell(summaryTable, String.format("%.1f%%", savingsRate), boldCellFont);

        document.add(summaryTable);

        // Category Breakdown Section
        if (categorySpends != null && !categorySpends.isEmpty()) {
            Paragraph catHeading = new Paragraph("2. Category-Wise Spending & Income Breakdown", sectionHeaderFont);
            catHeading.setSpacingBefore(18f);
            catHeading.setSpacingAfter(8f);
            document.add(catHeading);

            PdfPTable catTable = new PdfPTable(4);
            catTable.setWidthPercentage(100);
            catTable.setWidths(new float[]{35f, 20f, 25f, 20f});

            addHeaderCell(catTable, "Category", tableHeaderFont);
            addHeaderCell(catTable, "Type", tableHeaderFont);
            addHeaderCell(catTable, "Total Amount", tableHeaderFont);
            addHeaderCell(catTable, "% of Category Total", tableHeaderFont);

            boolean alt = false;
            for (CategorySpend cs : categorySpends) {
                Color bg = alt ? new Color(248, 250, 252) : Color.WHITE;
                addTableCell(catTable, cs.getCategoryName(), cellFont, bg, Element.ALIGN_LEFT);
                addTableCell(catTable, cs.getType().getDisplayName(), cellFont, bg, Element.ALIGN_CENTER);
                addTableCell(catTable, formatAmount(cs.getTotalAmount()), boldCellFont, bg, Element.ALIGN_RIGHT);
                addTableCell(catTable, String.format("%.1f%%", cs.getPercentageOfTotal()), cellFont, bg, Element.ALIGN_RIGHT);
                alt = !alt;
            }
            document.add(catTable);
        }

        // Detailed Transactions Section
        Paragraph txHeading = new Paragraph("3. Detailed Transaction Log (" + transactions.size() + " records)", sectionHeaderFont);
        txHeading.setSpacingBefore(18f);
        txHeading.setSpacingAfter(8f);
        document.add(txHeading);

        PdfPTable txTable = new PdfPTable(5);
        txTable.setWidthPercentage(100);
        txTable.setWidths(new float[]{16f, 15f, 22f, 32f, 15f});

        addHeaderCell(txTable, "Date", tableHeaderFont);
        addHeaderCell(txTable, "Type", tableHeaderFont);
        addHeaderCell(txTable, "Category", tableHeaderFont);
        addHeaderCell(txTable, "Description", tableHeaderFont);
        addHeaderCell(txTable, "Amount", tableHeaderFont);

        boolean alt = false;
        for (Transaction tx : transactions) {
            Color bg = alt ? new Color(248, 250, 252) : Color.WHITE;
            addTableCell(txTable, tx.getFormattedDate(), cellFont, bg, Element.ALIGN_CENTER);

            Font typeFont = cellFont;
            if (tx.getType() == TransactionType.INCOME) typeFont = greenFont;
            else if (tx.getType() == TransactionType.EXPENSE) typeFont = redFont;
            else if (tx.getType() == TransactionType.SAVINGS) typeFont = blueFont;

            addTableCell(txTable, tx.getType().getDisplayName(), typeFont, bg, Element.ALIGN_CENTER);
            addTableCell(txTable, tx.getCategoryName() != null ? tx.getCategoryName() : "-", cellFont, bg, Element.ALIGN_LEFT);
            addTableCell(txTable, tx.getDescription() != null ? tx.getDescription() : "", cellFont, bg, Element.ALIGN_LEFT);
            addTableCell(txTable, formatAmount(tx.getAmount()), boldCellFont, bg, Element.ALIGN_RIGHT);
            alt = !alt;
        }

        document.add(txTable);

        // Footer note
        Paragraph footer = new Paragraph("\nReport generated automatically by Personal Finance Tracker.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(148, 163, 184)));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
    }

    private void addSummaryHeaderCell(PdfPTable table, String text, Color bg, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(6f);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addSummaryValueCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(8f);
        cell.setPaddingBottom(8f);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(51, 65, 85));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(6f);
        cell.setBorderColor(new Color(30, 41, 59));
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }
}
