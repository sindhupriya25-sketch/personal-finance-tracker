package com.financetracker.service;

import com.financetracker.BaseDBTest;
import com.financetracker.model.CategorySpend;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSV and OpenPDF report generation in ReportService.
 */
public class ReportServiceTest extends BaseDBTest {

    private final ReportService reportService = new ReportService();

    @Test
    public void testExportToCSV() throws IOException {
        File csvFile = File.createTempFile("test_transactions", ".csv");
        csvFile.deleteOnExit();

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(1, 1, 1, "Salary", TransactionType.INCOME, 5000.0, "Monthly Salary", LocalDate.now()));
        transactions.add(new Transaction(2, 1, 2, "Food", TransactionType.EXPENSE, 120.50, "Grocery supermarket", LocalDate.now()));

        reportService.exportToCSV(csvFile, transactions);

        assertTrue(csvFile.exists());
        assertTrue(csvFile.length() > 0);
    }

    @Test
    public void testExportToPDF() throws Exception {
        File pdfFile = File.createTempFile("test_report", ".pdf");
        pdfFile.deleteOnExit();

        User user = new User("pdf_user", "hash", "salt");
        user.setId(1);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(1, 1, 1, "Salary", TransactionType.INCOME, 4000.0, "Salary", LocalDate.now()));
        transactions.add(new Transaction(2, 1, 2, "Food", TransactionType.EXPENSE, 250.0, "Food", LocalDate.now()));
        transactions.add(new Transaction(3, 1, 3, "Savings", TransactionType.SAVINGS, 500.0, "Savings", LocalDate.now()));

        List<CategorySpend> categorySpends = new ArrayList<>();
        CategorySpend cs = new CategorySpend("Food", TransactionType.EXPENSE, 250.0, 1);
        cs.setPercentageOfTotal(100.0);
        categorySpends.add(cs);

        reportService.exportToPDF(pdfFile, user, LocalDate.now().minusMonths(1), LocalDate.now(), transactions, categorySpends);

        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 100);
    }
}
