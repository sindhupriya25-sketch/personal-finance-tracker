package com.financetracker.ui;

import com.financetracker.model.TransactionType;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Design system constants, fonts, colors, and reusable component builders.
 */
public class UITheme {

    // Color Palette
    public static final Color PRIMARY = new Color(37, 99, 235);        // Vibrant Blue
    public static final Color PRIMARY_DARK = new Color(29, 78, 216);
    public static final Color PRIMARY_LIGHT = new Color(239, 246, 255);

    public static final Color SUCCESS = new Color(16, 185, 129);       // Emerald Green
    public static final Color SUCCESS_DARK = new Color(4, 120, 87);
    public static final Color SUCCESS_LIGHT = new Color(236, 253, 245);

    public static final Color DANGER = new Color(239, 68, 68);         // Coral Red
    public static final Color DANGER_DARK = new Color(185, 28, 28);
    public static final Color DANGER_LIGHT = new Color(254, 242, 242);

    public static final Color WARNING = new Color(245, 158, 11);       // Amber
    public static final Color WARNING_DARK = new Color(180, 83, 9);
    public static final Color WARNING_LIGHT = new Color(254, 252, 232);

    public static final Color INFO = new Color(59, 130, 246);          // Sky Blue
    public static final Color INFO_LIGHT = new Color(240, 249, 255);

    public static final Color BACKGROUND = new Color(248, 250, 252);   // Slate 50
    public static final Color CARD_BG = Color.WHITE;
    public static final Color SIDEBAR_BG = new Color(15, 23, 42);      // Slate 900
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);   // Slate 800
    public static final Color SIDEBAR_ACTIVE = new Color(37, 99, 235);

    public static final Color TEXT_MAIN = new Color(15, 23, 42);       // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139);   // Slate 500
    public static final Color TEXT_LIGHT = new Color(203, 213, 225);   // Slate 300
    public static final Color BORDER = new Color(226, 232, 240);       // Slate 200

    // Typography
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_KPI_NUMBER = new Font("Segoe UI", Font.BOLD, 24);

    // Formatters
    public static final Locale LOCALE_IN = new Locale("en", "IN");
    public static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_IN);
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    /**
     * Formats currency amounts safely with Rupee symbol.
     */
    public static String formatCurrency(double amount) {
        return "₹" + String.format(Locale.US, "%,.2f", amount);
    }

    /**
     * Creates a standard styled primary action button.
     */
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(PRIMARY);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Creates a standard secondary action button.
     */
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY);
        btn.setForeground(TEXT_MAIN);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(7, 14, 7, 14)
        ));
        return btn;
    }

    /**
     * Creates a danger/delete action button.
     */
    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(DANGER);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Creates a card panel with standard border and padding.
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));
        return panel;
    }

    /**
     * Returns color associated with a transaction type.
     */
    public static Color getTypeColor(TransactionType type) {
        if (type == null) return TEXT_MAIN;
        return switch (type) {
            case INCOME -> SUCCESS_DARK;
            case EXPENSE -> DANGER_DARK;
            case SAVINGS -> PRIMARY_DARK;
        };
    }
}
