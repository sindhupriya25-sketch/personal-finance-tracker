package com.financetracker.dao;

import com.financetracker.BaseDBTest;
import com.financetracker.model.Budget;
import com.financetracker.model.Category;
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
 * Unit tests for BudgetDAO limit configurations and spending usage calculations.
 */
public class BudgetDAOTest extends BaseDBTest {

    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final UserDAO userDAO = new UserDAO();
    private int userId;
    private Category foodCategory;

    @BeforeEach
    public void initTestData() throws SQLException {
        User user = new User("budget_test_user", "hash", "salt");
        userId = userDAO.create(user);
        foodCategory = categoryDAO.getByName("Food").orElseThrow();
    }

    @Test
    public void testSetAndGetBudget() throws SQLException {
        Budget budget = new Budget(userId, foodCategory.getId(), 600.0);
        int budgetId = budgetDAO.setBudget(budget);
        assertTrue(budgetId > 0);

        Optional<Budget> fetched = budgetDAO.getBudgetByUserAndCategory(userId, foodCategory.getId());
        assertTrue(fetched.isPresent());
        assertEquals(600.0, fetched.get().getMonthlyLimit(), 0.001);
        assertEquals("Food", fetched.get().getCategoryName());

        // Test Upsert / Update
        budget.setMonthlyLimit(750.0);
        budgetDAO.setBudget(budget);

        Optional<Budget> updated = budgetDAO.getBudgetByUserAndCategory(userId, foodCategory.getId());
        assertTrue(updated.isPresent());
        assertEquals(750.0, updated.get().getMonthlyLimit(), 0.001);
    }

    @Test
    public void testGetBudgetUsagesWithTransactions() throws SQLException {
        Budget budget = new Budget(userId, foodCategory.getId(), 500.0);
        budgetDAO.setBudget(budget);

        LocalDate today = LocalDate.now();
        transactionDAO.create(new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 200.0, "Groceries", today));
        transactionDAO.create(new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 150.0, "Dinner", today));

        List<Budget> usages = budgetDAO.getBudgetUsages(userId, today.getYear(), today.getMonthValue());
        assertFalse(usages.isEmpty());

        Budget foodUsage = usages.get(0);
        assertEquals(500.0, foodUsage.getMonthlyLimit(), 0.01);
        assertEquals(350.0, foodUsage.getCurrentSpent(), 0.01);
        assertEquals(150.0, foodUsage.getRemaining(), 0.01);
        assertEquals(70.0, foodUsage.getPercentageUsed(), 0.01);
        assertFalse(foodUsage.isWarning());
        assertFalse(foodUsage.isExceeded());
    }
}
