package com.financetracker.model;

/**
 * Entity model representing a category monthly spending limit.
 */
public class Budget {
    private int id;
    private int userId;
    private int categoryId;
    private String categoryName;
    private double monthlyLimit;
    private double currentSpent; // Transient / computed field for UI progress

    public Budget() {
    }

    public Budget(int id, int userId, int categoryId, String categoryName, double monthlyLimit) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.monthlyLimit = monthlyLimit;
    }

    public Budget(int userId, int categoryId, double monthlyLimit) {
        this(0, userId, categoryId, null, monthlyLimit);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public double getCurrentSpent() {
        return currentSpent;
    }

    public void setCurrentSpent(double currentSpent) {
        this.currentSpent = currentSpent;
    }

    public double getRemaining() {
        return monthlyLimit - currentSpent;
    }

    public double getPercentageUsed() {
        if (monthlyLimit <= 0) return 0.0;
        return (currentSpent / monthlyLimit) * 100.0;
    }

    public boolean isExceeded() {
        return currentSpent >= monthlyLimit && monthlyLimit > 0;
    }

    public boolean isWarning() {
        return getPercentageUsed() >= 80.0 && !isExceeded();
    }

    @Override
    public String toString() {
        return String.format("Budget[id=%d, category=%s, limit=%.2f, spent=%.2f (%.1f%%)]",
                id, categoryName, monthlyLimit, currentSpent, getPercentageUsed());
    }
}
