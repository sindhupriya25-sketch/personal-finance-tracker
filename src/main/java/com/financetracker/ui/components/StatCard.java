package com.financetracker.ui.components;

import com.financetracker.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Metric summary card component displaying KPI values, icon indicator, and trends.
 */
public class StatCard extends JPanel {
    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtextLabel;
    private final JPanel iconBadge;
    private final JLabel iconLabel;

    public StatCard(String title, String initialValue, String subtext, String iconSymbol, Color accentColor, Color badgeBgColor) {
        setLayout(new BorderLayout(12, 0));
        setBackground(UITheme.CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        // Left Icon Badge
        iconBadge = new JPanel(new GridBagLayout());
        iconBadge.setPreferredSize(new Dimension(48, 48));
        iconBadge.setBackground(badgeBgColor != null ? badgeBgColor : UITheme.PRIMARY_LIGHT);
        iconBadge.setBorder(new LineBorder(accentColor, 1, true));

        iconLabel = new JLabel(iconSymbol != null ? iconSymbol : "₹");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 20));
        iconLabel.setForeground(accentColor != null ? accentColor : UITheme.PRIMARY);
        iconBadge.add(iconLabel);

        add(iconBadge, BorderLayout.WEST);

        // Center Info
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(UITheme.CARD_BG);

        titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SMALL_BOLD);
        titleLabel.setForeground(UITheme.TEXT_MUTED);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(UITheme.FONT_KPI_NUMBER);
        valueLabel.setForeground(UITheme.TEXT_MAIN);

        subtextLabel = new JLabel(subtext);
        subtextLabel.setFont(UITheme.FONT_SMALL);
        subtextLabel.setForeground(UITheme.TEXT_MUTED);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(valueLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtextLabel);

        add(textPanel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setValueColor(Color color) {
        valueLabel.setForeground(color);
    }

    public void setSubtext(String subtext) {
        subtextLabel.setText(subtext);
    }
}
