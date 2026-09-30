package com.financetracker.service;

import com.financetracker.dao.BudgetDAO;
import com.financetracker.dao.CategoryDAO;
import com.financetracker.dao.TransactionDAO;
import com.financetracker.model.Budget;
import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to generate 3 months of realistic financial transactions and budgets for demo purposes.
 */
public class SampleDataService {
    private final CategoryDAO categoryDAO;
    private final TransactionDAO transactionDAO;
    private final BudgetDAO budgetDAO;
    private final BudgetAlertService alertService;

    public SampleDataService() {
        this(new CategoryDAO(), new TransactionDAO(), new BudgetDAO(), new BudgetAlertService());
    }

    public SampleDataService(CategoryDAO categoryDAO, TransactionDAO transactionDAO,
                             BudgetDAO budgetDAO, BudgetAlertService alertService) {
        this.categoryDAO = categoryDAO;
        this.transactionDAO = transactionDAO;
        this.budgetDAO = budgetDAO;
        this.alertService = alertService;
    }

    /**
     * Seeds realistic budget limits and 3 months of income, expense, and savings transactions for the user.
     *
     * @param userId target user ID
     * @param clearExisting if true, clears user's prior transactions and budgets first
     * @throws SQLException on database error
     */
    public void generateSampleData(int userId, boolean clearExisting) throws SQLException {
        if (clearExisting) {
            transactionDAO.deleteAllForUser(userId);
            budgetDAO.deleteAllForUser(userId);
        }

        // Map category names to IDs
        List<Category> allCategories = categoryDAO.getAll();
        Map<String, Category> catMap = new HashMap<>();
        for (Category c : allCategories) {
            catMap.put(c.getName().toLowerCase(), c);
        }

        // 1. Setup Budgets (in INR)
        setBudgetIfPresent(userId, catMap.get("food"), 15000.0);
        setBudgetIfPresent(userId, catMap.get("rent"), 25000.0);
        setBudgetIfPresent(userId, catMap.get("transport"), 6000.0);
        setBudgetIfPresent(userId, catMap.get("shopping"), 8000.0);
        setBudgetIfPresent(userId, catMap.get("entertainment"), 4500.0);
        setBudgetIfPresent(userId, catMap.get("bills"), 6500.0);
        setBudgetIfPresent(userId, catMap.get("health"), 3500.0);
        setBudgetIfPresent(userId, catMap.get("education"), 3000.0);

        // 2. Generate 3 Months of Transactions (Current month, month - 1, month - 2)
        LocalDate today = LocalDate.now();
        YearMonth currentYm = YearMonth.from(today);

        for (int i = 2; i >= 0; i--) {
            YearMonth targetMonth = currentYm.minusMonths(i);
            boolean isCurrentMonth = (i == 0);
            int maxDay = isCurrentMonth ? Math.min(today.getDayOfMonth(), targetMonth.lengthOfMonth()) : targetMonth.lengthOfMonth();

            // Income
            addTx(userId, catMap.get("salary"), TransactionType.INCOME, 85000.0, "Monthly Corporate Salary", targetMonth.atDay(1));
            addTx(userId, catMap.get("freelance"), TransactionType.INCOME, 18000.0 + (i * 2500), "Web Development Project", targetMonth.atDay(Math.min(15, maxDay)));
            if (i != 1) {
                addTx(userId, catMap.get("investment"), TransactionType.INCOME, 3500.0, "Stock Dividend & Mutual Fund Returns", targetMonth.atDay(Math.min(20, maxDay)));
            }

            // Fixed Expenses
            addTx(userId, catMap.get("rent"), TransactionType.EXPENSE, 25000.0, "Apartment Monthly Rent", targetMonth.atDay(Math.min(2, maxDay)));
            addTx(userId, catMap.get("bills"), TransactionType.EXPENSE, 3200.0, "Electricity & Water Utility Bill", targetMonth.atDay(Math.min(5, maxDay)));
            addTx(userId, catMap.get("bills"), TransactionType.EXPENSE, 1200.0, "Fiber Broadband Internet", targetMonth.atDay(Math.min(6, maxDay)));
            addTx(userId, catMap.get("bills"), TransactionType.EXPENSE, 800.0, "Mobile Plan & Streaming Subscriptions", targetMonth.atDay(Math.min(8, maxDay)));

            // Variable Expenses
            addTx(userId, catMap.get("food"), TransactionType.EXPENSE, 3800.0, "Weekly Grocery Run (Supermarket)", targetMonth.atDay(Math.min(3, maxDay)));
            addTx(userId, catMap.get("food"), TransactionType.EXPENSE, 4200.0, "Grocery Restock & Organic Produce", targetMonth.atDay(Math.min(10, maxDay)));
            addTx(userId, catMap.get("food"), TransactionType.EXPENSE, 3900.0, "Supermarket & Pantry Essentials", targetMonth.atDay(Math.min(17, maxDay)));
            addTx(userId, catMap.get("food"), TransactionType.EXPENSE, 1800.0, "Weekend Dinner with Family", targetMonth.atDay(Math.min(12, maxDay)));

            addTx(userId, catMap.get("transport"), TransactionType.EXPENSE, 1600.0, "Fuel refill (Petrol station)", targetMonth.atDay(Math.min(4, maxDay)));
            addTx(userId, catMap.get("transport"), TransactionType.EXPENSE, 1200.0, "Monthly Metro Card Recharge", targetMonth.atDay(Math.min(14, maxDay)));
            addTx(userId, catMap.get("transport"), TransactionType.EXPENSE, 1500.0, "Fuel & Vehicle Service", targetMonth.atDay(Math.min(22, maxDay)));

            addTx(userId, catMap.get("entertainment"), TransactionType.EXPENSE, 1200.0, "Movie Tickets & Snacks", targetMonth.atDay(Math.min(9, maxDay)));
            addTx(userId, catMap.get("entertainment"), TransactionType.EXPENSE, 1800.0, "Concert & Weekend Leisure", targetMonth.atDay(Math.min(19, maxDay)));

            addTx(userId, catMap.get("health"), TransactionType.EXPENSE, 1500.0, "Gym Monthly Membership", targetMonth.atDay(Math.min(1, maxDay)));
            addTx(userId, catMap.get("health"), TransactionType.EXPENSE, 1200.0, "Pharmacy & Multivitamins", targetMonth.atDay(Math.min(16, maxDay)));

            addTx(userId, catMap.get("education"), TransactionType.EXPENSE, 1800.0, "Technical Certification & Books", targetMonth.atDay(Math.min(7, maxDay)));

            // Savings
            addTx(userId, catMap.get("savings"), TransactionType.SAVINGS, 15000.0, "Monthly SIP / Mutual Fund Investment", targetMonth.atDay(Math.min(2, maxDay)));
            addTx(userId, catMap.get("emergency fund"), TransactionType.SAVINGS, 5000.0, "Emergency Reserve Fund Contribution", targetMonth.atDay(Math.min(5, maxDay)));

            // In current month, add extra shopping & food to trigger realistic alerts
            if (isCurrentMonth) {
                addTx(userId, catMap.get("shopping"), TransactionType.EXPENSE, 4800.0, "Electronics / Headphone upgrade", targetMonth.atDay(Math.min(11, maxDay)));
                addTx(userId, catMap.get("shopping"), TransactionType.EXPENSE, 3800.0, "Apparel & Festive Clothes Sale", targetMonth.atDay(Math.min(18, maxDay)));
                // Triggers shopping over budget (8,600 / 8,000 = 107.5%)
                addTx(userId, catMap.get("food"), TransactionType.EXPENSE, 2500.0, "Gourmet Dinner & Takeout", targetMonth.atDay(Math.min(20, maxDay)));
                // Triggers food warning (16,200 / 15,000 = 108%)
            } else {
                addTx(userId, catMap.get("shopping"), TransactionType.EXPENSE, 3200.0, "Clothing & Accessories", targetMonth.atDay(Math.min(12, maxDay)));
                addTx(userId, catMap.get("shopping"), TransactionType.EXPENSE, 2100.0, "Home Decors", targetMonth.atDay(Math.min(24, maxDay)));
            }
        }

        // Run alert checks for current month
        if (catMap.get("food") != null) {
            alertService.checkBudget(userId, catMap.get("food").getId(), today);
        }
        if (catMap.get("shopping") != null) {
            alertService.checkBudget(userId, catMap.get("shopping").getId(), today);
        }
    }

    private void setBudgetIfPresent(int userId, Category category, double limit) throws SQLException {
        if (category != null) {
            Budget b = new Budget(userId, category.getId(), limit);
            budgetDAO.setBudget(b);
        }
    }

    private void addTx(int userId, Category cat, TransactionType type, double amount, String desc, LocalDate date) throws SQLException {
        if (cat != null) {
            Transaction tx = new Transaction(userId, cat.getId(), type, amount, desc, date);
            transactionDAO.create(tx);
        }
    }
}
