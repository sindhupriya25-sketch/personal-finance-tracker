package com.financetracker.ui.components;

import com.financetracker.model.Budget;
import com.financetracker.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Visual progress bar rendering spending against budget limit with dynamic threshold coloring.
 */
public class BudgetProgressBar extends JPanel {
    private final Budget budget;

    public BudgetProgressBar(Budget budget) {
        this.budget = budget;
        setLayout(new BorderLayout(0, 4));
        setBackground(UITheme.CARD_BG);
        setBorder(new EmptyBorder(4, 0, 8, 0));

        // Top info header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.CARD_BG);

        JLabel nameLabel = new JLabel(budget.getCategoryName() != null ? budget.getCategoryName() : "Category");
        nameLabel.setFont(UITheme.FONT_BODY_BOLD);
        nameLabel.setForeground(UITheme.TEXT_MAIN);

        double pct = budget.getPercentageUsed();
        String pctStr = String.format("%.1f%%", pct);
        JLabel pctLabel = new JLabel(pctStr);
        pctLabel.setFont(UITheme.FONT_SMALL_BOLD);

        if (pct >= 100.0) {
            pctLabel.setForeground(UITheme.DANGER);
            pctLabel.setText(pctStr + " (EXCEEDED)");
        } else if (pct >= 80.0) {
            pctLabel.setForeground(UITheme.WARNING_DARK);
            pctLabel.setText(pctStr + " (NEAR LIMIT)");
        } else {
            pctLabel.setForeground(UITheme.SUCCESS_DARK);
        }

        headerPanel.add(nameLabel, BorderLayout.WEST);
        headerPanel.add(pctLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Progress Bar
        JProgressBar progressBar = new JProgressBar(0, 100);
        int intPct = (int) Math.min(100, Math.round(pct));
        progressBar.setValue(intPct);
        progressBar.setPreferredSize(new Dimension(100, 10));
        progressBar.setStringPainted(false);

        if (pct >= 100.0) {
            progressBar.setForeground(UITheme.DANGER);
        } else if (pct >= 80.0) {
            progressBar.setForeground(UITheme.WARNING);
        } else {
            progressBar.setForeground(UITheme.SUCCESS);
        }
        progressBar.setBackground(new Color(241, 245, 249));

        add(progressBar, BorderLayout.CENTER);

        // Subtext: Spent vs Limit
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(UITheme.CARD_BG);

        String spentText = String.format("Spent: %s of %s",
                UITheme.formatCurrency(budget.getCurrentSpent()),
                UITheme.formatCurrency(budget.getMonthlyLimit()));
        JLabel spentLabel = new JLabel(spentText);
        spentLabel.setFont(UITheme.FONT_SMALL);
        spentLabel.setForeground(UITheme.TEXT_MUTED);

        double remaining = budget.getRemaining();
        String remText = remaining >= 0
                ? String.format("Remaining: %s", UITheme.formatCurrency(remaining))
                : String.format("Over: %s", UITheme.formatCurrency(Math.abs(remaining)));
        JLabel remLabel = new JLabel(remText);
        remLabel.setFont(UITheme.FONT_SMALL_BOLD);
        remLabel.setForeground(remaining >= 0 ? UITheme.TEXT_MUTED : UITheme.DANGER);

        footerPanel.add(spentLabel, BorderLayout.WEST);
        footerPanel.add(remLabel, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);
    }
}
