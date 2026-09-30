package com.financetracker.service;

import com.financetracker.BaseDBTest;
import com.financetracker.dao.AlertDAO;
import com.financetracker.dao.BudgetDAO;
import com.financetracker.dao.CategoryDAO;
import com.financetracker.dao.TransactionDAO;
import com.financetracker.dao.UserDAO;
import com.financetracker.model.Budget;
import com.financetracker.model.BudgetAlert;
import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import com.financetracker.service.BudgetAlertService.BudgetCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BudgetAlertService threshold calculations and alert triggers.
 */
public class BudgetAlertServiceTest extends BaseDBTest {

    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AlertDAO alertDAO = new AlertDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final UserDAO userDAO = new UserDAO();

    private BudgetAlertService alertService;
    private int userId;
    private Category foodCategory;

    @BeforeEach
    public void init() throws SQLException {
        alertService = new BudgetAlertService(budgetDAO, transactionDAO, alertDAO);
        User user = new User("alert_user", "hash", "salt");
        userId = userDAO.create(user);
        foodCategory = categoryDAO.getByName("Food").orElseThrow();
    }

    @Test
    public void testNoBudgetReturnsNone() throws SQLException {
        LocalDate today = LocalDate.now();
        transactionDAO.create(new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 100.0, "Food", today));

        BudgetCheckResult result = alertService.checkBudget(userId, foodCategory.getId(), today);
        assertFalse(result.hasBudget());
        assertFalse(result.isAlert());
    }

    @Test
    public void testUnder80PercentReturnsNormal() throws SQLException {
        LocalDate today = LocalDate.now();
        budgetDAO.setBudget(new Budget(userId, foodCategory.getId(), 500.0));
        transactionDAO.create(new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 250.0, "Food", today)); // 50%

        BudgetCheckResult result = alertService.checkBudget(userId, foodCategory.getId(), today);
        assertTrue(result.hasBudget());
        assertFalse(result.isAlert());
        assertEquals(50.0, result.getPercentage(), 0.01);

        List<BudgetAlert> alerts = alertDAO.getByUser(userId, 10);
        assertTrue(alerts.isEmpty());
    }

    @Test
    public void testAtOrAbove80PercentTriggersWarning() throws SQLException {
        LocalDate today = LocalDate.now();
        budgetDAO.setBudget(new Budget(userId, foodCategory.getId(), 500.0));
        transactionDAO.create(new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 425.0, "Food", today)); // 85%

        BudgetCheckResult result = alertService.checkBudget(userId, foodCategory.getId(), today);
        assertTrue(result.hasBudget());
        assertTrue(result.isAlert());
        assertTrue(result.isWarning());
        assertFalse(result.isExceeded());
        assertEquals(85.0, result.getPercentage(), 0.01);

        List<BudgetAlert> alerts = alertDAO.getByUser(userId, 10);
        assertEquals(1, alerts.size());
        assertEquals(BudgetAlert.AlertLevel.WARNING, alerts.get(0).getAlertLevel());
    }

    @Test
    public void testAtOrAbove100PercentTriggersExceededAlert() throws SQLException {
        LocalDate today = LocalDate.now();
        budgetDAO.setBudget(new Budget(userId, foodCategory.getId(), 500.0));
        transactionDAO.create(new Transaction(userId, foodCategory.getId(), TransactionType.EXPENSE, 550.0, "Food", today)); // 110%

        BudgetCheckResult result = alertService.checkBudget(userId, foodCategory.getId(), today);
        assertTrue(result.hasBudget());
        assertTrue(result.isAlert());
        assertTrue(result.isExceeded());
        assertEquals(110.0, result.getPercentage(), 0.01);

        List<BudgetAlert> alerts = alertDAO.getByUser(userId, 10);
        assertEquals(1, alerts.size());
        assertEquals(BudgetAlert.AlertLevel.EXCEEDED, alerts.get(0).getAlertLevel());
    }
}
