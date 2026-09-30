package com.financetracker.model;

/**
 * Data Transfer Object representing aggregated spending per category.
 */
public class CategorySpend {
    private String categoryName;
    private TransactionType type;
    private double totalAmount;
    private int transactionCount;
    private double percentageOfTotal;

    public CategorySpend() {
    }

    public CategorySpend(String categoryName, TransactionType type, double totalAmount, int transactionCount) {
        this.categoryName = categoryName;
        this.type = type;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
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

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getPercentageOfTotal() {
        return percentageOfTotal;
    }

    public void setPercentageOfTotal(double percentageOfTotal) {
        this.percentageOfTotal = percentageOfTotal;
    }

    @Override
    public String toString() {
        return String.format("%s: $%.2f (%.1f%%, %d trans)",
                categoryName, totalAmount, percentageOfTotal, transactionCount);
    }
}
