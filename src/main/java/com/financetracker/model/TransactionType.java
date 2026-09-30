package com.financetracker.model;

import java.awt.Color;

/**
 * Enumeration representing the financial transaction classifications.
 */
public enum TransactionType {
    INCOME("Income", new Color(46, 125, 50), new Color(232, 245, 233)),
    EXPENSE("Expense", new Color(198, 40, 40), new Color(255, 235, 238)),
    SAVINGS("Savings", new Color(21, 101, 192), new Color(227, 242, 253));

    private final String displayName;
    private final Color primaryColor;
    private final Color badgeBackgroundColor;

    TransactionType(String displayName, Color primaryColor, Color badgeBackgroundColor) {
        this.displayName = displayName;
        this.primaryColor = primaryColor;
        this.badgeBackgroundColor = badgeBackgroundColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public Color getBadgeBackgroundColor() {
        return badgeBackgroundColor;
    }

    /**
     * Safely parses a string value into a TransactionType.
     *
     * @param value the string representation (e.g. "INCOME", "Income")
     * @return the corresponding TransactionType, or EXPENSE as default
     */
    public static TransactionType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return EXPENSE;
        }
        String normalized = value.trim().toUpperCase();
        for (TransactionType type : values()) {
            if (type.name().equalsIgnoreCase(normalized) || type.displayName.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return EXPENSE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
