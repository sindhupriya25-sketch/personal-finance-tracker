package com.financetracker.service;

import com.financetracker.dao.AlertDAO;
import com.financetracker.dao.BudgetDAO;
import com.financetracker.dao.TransactionDAO;
import com.financetracker.model.Budget;
import com.financetracker.model.BudgetAlert;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Service that monitors spending thresholds against defined budgets and generates alerts.
 */
public class BudgetAlertService {
    private final BudgetDAO budgetDAO;
    private final TransactionDAO transactionDAO;
    private final AlertDAO alertDAO;

    /**
     * Encapsulates the outcome of a budget limit check.
     */
    public static class BudgetCheckResult {
        private final boolean hasBudget;
        private final BudgetAlert.AlertLevel alertLevel;
        private final String categoryName;
        private final double limitAmount;
        private final double spentAmount;
        private final double percentage;
        private final String message;

        public BudgetCheckResult(boolean hasBudget, BudgetAlert.AlertLevel alertLevel,
                                 String categoryName, double limitAmount, double spentAmount,
                                 double percentage, String message) {
            this.hasBudget = hasBudget;
            this.alertLevel = alertLevel;
            this.categoryName = categoryName;
            this.limitAmount = limitAmount;
            this.spentAmount = spentAmount;
            this.percentage = percentage;
            this.message = message;
        }

        public static BudgetCheckResult none() {
            return new BudgetCheckResult(false, null, "", 0, 0, 0, "");
        }

        public static BudgetCheckResult normal(String categoryName, double limit, double spent, double percentage) {
            return new BudgetCheckResult(true, null, categoryName, limit, spent, percentage,
                    String.format("Spending is within budget (%.1f%% used).", percentage));
        }

        public static BudgetCheckResult warning(String categoryName, double limit, double spent, double percentage, String message) {
            return new BudgetCheckResult(true, BudgetAlert.AlertLevel.WARNING, categoryName, limit, spent, percentage, message);
        }

        public static BudgetCheckResult exceeded(String categoryName, double limit, double spent, double percentage, String message) {
            return new BudgetCheckResult(true, BudgetAlert.AlertLevel.EXCEEDED, categoryName, limit, spent, percentage, message);
        }

        public boolean hasBudget() {
            return hasBudget;
        }

        public boolean isAlert() {
            return alertLevel != null;
        }

        public boolean isWarning() {
            return alertLevel == BudgetAlert.AlertLevel.WARNING;
        }

        public boolean isExceeded() {
            return alertLevel == BudgetAlert.AlertLevel.EXCEEDED;
        }

        public BudgetAlert.AlertLevel getAlertLevel() {
            return alertLevel;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public double getLimitAmount() {
            return limitAmount;
        }

        public double getSpentAmount() {
            return spentAmount;
        }

        public double getPercentage() {
            return percentage;
        }

        public String getMessage() {
            return message;
        }
    }

    public BudgetAlertService() {
        this(new BudgetDAO(), new TransactionDAO(), new AlertDAO());
    }

    public BudgetAlertService(BudgetDAO budgetDAO, TransactionDAO transactionDAO, AlertDAO alertDAO) {
        this.budgetDAO = budgetDAO;
        this.transactionDAO = transactionDAO;
        this.alertDAO = alertDAO;
    }

    /**
     * Checks if a transaction causes the monthly budget for its category to enter warning (>=80%) or exceeded (>=100%) status.
     *
     * @param userId user ID
     * @param categoryId category ID
     * @param txDate date of the transaction
     * @return BudgetCheckResult containing alert status and details
     * @throws SQLException on database error
     */
    public BudgetCheckResult checkBudget(int userId, int categoryId, LocalDate txDate) throws SQLException {
        if (txDate == null) {
            txDate = LocalDate.now();
        }

        Optional<Budget> budgetOpt = budgetDAO.getBudgetByUserAndCategory(userId, categoryId);
        if (budgetOpt.isEmpty()) {
            return BudgetCheckResult.none();
        }

        Budget budget = budgetOpt.get();
        double limit = budget.getMonthlyLimit();
        if (limit <= 0) {
            return BudgetCheckResult.none();
        }

        int year = txDate.getYear();
        int month = txDate.getMonthValue();
        double spent = transactionDAO.getCategoryMonthlySpend(userId, categoryId, year, month);
        double percentage = (spent / limit) * 100.0;
        String categoryName = budget.getCategoryName() != null ? budget.getCategoryName() : "Category";
        String monthFormatted = YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        String monthKey = YearMonth.of(year, month).toString();

        if (percentage >= 100.0) {
            double overage = spent - limit;
            String msg = String.format("ALERT: You have EXCEEDED your %s budget by ₹%,.2f for %s!\nSpent: ₹%,.2f | Budget: ₹%,.2f (%.1f%%)",
                    categoryName, overage, monthFormatted, spent, limit, percentage);

            BudgetAlert alert = new BudgetAlert(userId, categoryId, categoryName, monthKey,
                    limit, spent, percentage, BudgetAlert.AlertLevel.EXCEEDED, msg);
            alertDAO.create(alert);

            return BudgetCheckResult.exceeded(categoryName, limit, spent, percentage, msg);
        } else if (percentage >= 80.0) {
            double remaining = limit - spent;
            String msg = String.format("WARNING: You have used %.1f%% of your %s budget for %s!\nSpent: ₹%,.2f | Budget: ₹%,.2f | Remaining: ₹%,.2f",
                    percentage, categoryName, monthFormatted, spent, limit, remaining);

            BudgetAlert alert = new BudgetAlert(userId, categoryId, categoryName, monthKey,
                    limit, spent, percentage, BudgetAlert.AlertLevel.WARNING, msg);
            alertDAO.create(alert);

            return BudgetCheckResult.warning(categoryName, limit, spent, percentage, msg);
        }

        return BudgetCheckResult.normal(categoryName, limit, spent, percentage);
    }
}
