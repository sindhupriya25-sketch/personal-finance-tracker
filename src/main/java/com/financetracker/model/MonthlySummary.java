package com.financetracker.model;

/**
 * Data Transfer Object representing aggregated financial metrics for a specific month or period.
 */
public class MonthlySummary {
    private String monthKey; // e.g. "2026-09" or "Sep 2026"
    private double totalIncome;
    private double totalExpense;
    private double totalSavings;

    public MonthlySummary() {
    }

    public MonthlySummary(String monthKey, double totalIncome, double totalExpense, double totalSavings) {
        this.monthKey = monthKey;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.totalSavings = totalSavings;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public void setMonthKey(String monthKey) {
        this.monthKey = monthKey;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(double totalExpense) {
        this.totalExpense = totalExpense;
    }

    public double getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(double totalSavings) {
        this.totalSavings = totalSavings;
    }

    public double getNetBalance() {
        return totalIncome - totalExpense - totalSavings;
    }

    public double getSavingsRate() {
        if (totalIncome <= 0) return 0.0;
        return (totalSavings / totalIncome) * 100.0;
    }

    @Override
    public String toString() {
        return String.format("Summary[%s: Income=%.2f, Expense=%.2f, Savings=%.2f, Net=%.2f]",
                monthKey, totalIncome, totalExpense, totalSavings, getNetBalance());
    }
}
