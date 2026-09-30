package com.financetracker.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entity model representing an alert triggered when spending reaches or exceeds a category budget.
 */
public class BudgetAlert {
    public enum AlertLevel {
        WARNING("Warning (>=80%)", "Budget Warning"),
        EXCEEDED("Exceeded (>=100%)", "Budget Exceeded");

        private final String label;
        private final String title;

        AlertLevel(String label, String title) {
            this.label = label;
            this.title = title;
        }

        public String getLabel() {
            return label;
        }

        public String getTitle() {
            return title;
        }
    }

    private int id;
    private int userId;
    private int categoryId;
    private String categoryName;
    private String monthYear; // "YYYY-MM"
    private double limitAmount;
    private double spentAmount;
    private double percentage;
    private AlertLevel alertLevel;
    private String message;
    private LocalDateTime timestamp;

    public BudgetAlert() {
        this.timestamp = LocalDateTime.now();
    }

    public BudgetAlert(int id, int userId, int categoryId, String categoryName, String monthYear,
                       double limitAmount, double spentAmount, double percentage,
                       AlertLevel alertLevel, String message, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.monthYear = monthYear;
        this.limitAmount = limitAmount;
        this.spentAmount = spentAmount;
        this.percentage = percentage;
        this.alertLevel = alertLevel;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public BudgetAlert(int userId, int categoryId, String categoryName, String monthYear,
                       double limitAmount, double spentAmount, double percentage,
                       AlertLevel alertLevel, String message) {
        this(0, userId, categoryId, categoryName, monthYear, limitAmount, spentAmount, percentage, alertLevel, message, LocalDateTime.now());
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

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public double getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(double limitAmount) {
        this.limitAmount = limitAmount;
    }

    public double getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(double spentAmount) {
        this.spentAmount = spentAmount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(AlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (%.1f%% spent)",
                alertLevel, categoryName, message, percentage);
    }
}
