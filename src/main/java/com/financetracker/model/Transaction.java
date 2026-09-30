package com.financetracker.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Entity model representing an income, expense, or savings transaction.
 */
public class Transaction {
    private int id;
    private int userId;
    private int categoryId;
    private String categoryName;
    private TransactionType type;
    private double amount;
    private String description;
    private LocalDate date;

    public Transaction() {
    }

    public Transaction(int id, int userId, int categoryId, String categoryName,
                       TransactionType type, double amount, String description, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.date = date;
    }

    public Transaction(int userId, int categoryId, TransactionType type, double amount,
                       String description, LocalDate date) {
        this(0, userId, categoryId, null, type, amount, description, date);
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getFormattedDate() {
        return date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "";
    }

    @Override
    public String toString() {
        return String.format("Transaction[id=%d, type=%s, category=%s, amount=%.2f, date=%s]",
                id, type, categoryName, amount, getFormattedDate());
    }
}
