package com.financetracker.dao;

import com.financetracker.BaseDBTest;
import com.financetracker.model.Category;
import com.financetracker.model.CategorySpend;
import com.financetracker.model.MonthlySummary;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransactionDAO CRUD, aggregation, and filtering.
 */
public class TransactionDAOTest extends BaseDBTest {

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final UserDAO userDAO = new UserDAO();
    private int userId;
    private Category foodCategory;
    private Category salaryCategory;
    private Category savingsCategory;

    @BeforeEach
    public void initTestData() throws SQLException {
        User user = new User("tx_test_user", "hash", "salt");
        userId = userDAO.create(user);

        foodCategory = categoryDAO.getByName("Food").orElseThrow();
        salaryCategory = categoryDAO.getByName("Salary").orElseThrow();
        savingsCategory = categoryDAO.getByName("Savings").orElseThrow();
    }

    @Test
    public void testCreateReadUpdateDeleteTransaction() throws SQLException {
        LocalDate today = LocalDate.now();
        Transaction tx = new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 85.50, "Dinner with family", today);

        int txId = transactionDAO.create(tx);
        assertTrue(txId > 0);

        // Read
        Optional<Transaction> fetched = transactionDAO.getById(txId);
        assertTrue(fetched.isPresent());
        assertEquals(85.50, fetched.get().getAmount(), 0.001);
        assertEquals("Food", fetched.get().getCategoryName());
        assertEquals("Dinner with family", fetched.get().getDescription());

        // Update
        fetched.get().setAmount(95.00);
        fetched.get().setDescription("Dinner with family (updated)");
        boolean updated = transactionDAO.update(fetched.get());
        assertTrue(updated);

        Optional<Transaction> updatedFetched = transactionDAO.getById(txId);
        assertEquals(95.00, updatedFetched.get().getAmount(), 0.001);

        // Delete
        boolean deleted = transactionDAO.delete(txId, userId);
        assertTrue(deleted);
        assertTrue(transactionDAO.getById(txId).isEmpty());
    }

    @Test
    public void testFilteringAndSearch() throws SQLException {
        LocalDate today = LocalDate.now();
        transactionDAO.create(new Transaction(userId, salaryCategory.getId(), TransactionType.INCOME, 3000.0, "Monthly Salary", today.minusDays(5)));
        transactionDAO.create(new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 45.0, "Organic Supermarket", today.minusDays(3)));
        transactionDAO.create(new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 15.0, "Coffee & Bagel", today.minusDays(1)));
        transactionDAO.create(new Transaction(userId, savingsCategory.getId(), TransactionType.SAVINGS, 500.0, "Emergency Reserve", today));

        // Filter by Type
        List<Transaction> expenses = transactionDAO.getFiltered(userId, null, null, null, TransactionType.EXPENSE, null);
        assertEquals(2, expenses.size());

        // Filter by Category
        List<Transaction> foodTx = transactionDAO.getFiltered(userId, null, null, foodCategory.getId(), null, null);
        assertEquals(2, foodTx.size());

        // Search Keyword
        List<Transaction> searchResults = transactionDAO.getFiltered(userId, null, null, null, null, "Bagel");
        assertEquals(1, searchResults.size());
        assertEquals("Coffee & Bagel", searchResults.get(0).getDescription());
    }

    @Test
    public void testMonthlySummaryCalculation() throws SQLException {
        LocalDate today = LocalDate.now();
        transactionDAO.create(new Transaction(userId, salaryCategory.getId(), TransactionType.INCOME, 4000.0, "Salary", today));
        transactionDAO.create(new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 500.0, "Food", today));
        transactionDAO.create(new Transaction(userId, savingsCategory.getId(), TransactionType.SAVINGS, 1000.0, "Savings", today));

        MonthlySummary summary = transactionDAO.getMonthlySummary(userId, today.getYear(), today.getMonthValue());
        assertEquals(4000.0, summary.getTotalIncome(), 0.01);
        assertEquals(500.0, summary.getTotalExpense(), 0.01);
        assertEquals(1000.0, summary.getTotalSavings(), 0.01);
        assertEquals(2500.0, summary.getNetBalance(), 0.01);
        assertEquals(25.0, summary.getSavingsRate(), 0.01);
    }
}
