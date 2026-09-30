package com.financetracker.ui;

import com.financetracker.dao.AlertDAO;
import com.financetracker.model.BudgetAlert;
import com.financetracker.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Alerts log panel presenting historical and recent budget alerts.
 */
public class AlertsPanel extends JPanel {
    private final User currentUser;
    private final MainFrame mainFrame;
    private final AlertDAO alertDAO;

    private JTable alertsTable;
    private DefaultTableModel tableModel;
    private JLabel countLabel;

    public AlertsPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame = mainFrame;
        this.alertDAO = new AlertDAO();

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        refreshAlerts();
    }

    private void initComponents() {
        // Header & Actions
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UITheme.BACKGROUND);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setBackground(UITheme.BACKGROUND);

        JLabel title = new JLabel("Budget Alerts & Notification Log");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Audit trail of all budget threshold warnings (>=80%) and overage alerts (>=100%)");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(subtitle);
        topPanel.add(headerText, BorderLayout.WEST);

        // Actions
        JPanel actionGrp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionGrp.setBackground(UITheme.BACKGROUND);

        JButton clearBtn = UITheme.createDangerButton("Clear Alerts History");
        clearBtn.addActionListener(e -> clearAlerts());

        JButton refreshBtn = UITheme.createPrimaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshAlerts());

        actionGrp.add(clearBtn);
        actionGrp.add(refreshBtn);
        topPanel.add(actionGrp, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Table Card
        JPanel cardPanel = UITheme.createCardPanel();
        cardPanel.setLayout(new BorderLayout(0, 10));

        String[] cols = {"Timestamp", "Level", "Category", "Month", "Limit", "Spent", "% Used", "Alert Message"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        alertsTable = new JTable(tableModel);
        alertsTable.setFont(UITheme.FONT_BODY);
        alertsTable.setRowHeight(34);
        alertsTable.setShowGrid(false);
        alertsTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        alertsTable.getTableHeader().setBackground(new Color(241, 245, 249));

        alertsTable.getColumnModel().getColumn(0).setMaxWidth(140);
        alertsTable.getColumnModel().getColumn(1).setMaxWidth(100);
        alertsTable.getColumnModel().getColumn(1).setCellRenderer(new AlertLevelRenderer());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        alertsTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        alertsTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        alertsTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        JScrollPane scrollPane = new JScrollPane(alertsTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER, 1));
        cardPanel.add(scrollPane, BorderLayout.CENTER);

        countLabel = new JLabel("Total Logged Alerts: 0");
        countLabel.setFont(UITheme.FONT_SMALL_BOLD);
        countLabel.setForeground(UITheme.TEXT_MUTED);
        cardPanel.add(countLabel, BorderLayout.SOUTH);

        add(cardPanel, BorderLayout.CENTER);
    }

    public void refreshAlerts() {
        if (currentUser == null) return;

        try {
            List<BudgetAlert> alerts = alertDAO.getByUser(currentUser.getId(), 100);
            tableModel.setRowCount(0);

            for (BudgetAlert a : alerts) {
                tableModel.addRow(new Object[]{
                        a.getFormattedTimestamp(),
                        a.getAlertLevel().name(),
                        a.getCategoryName(),
                        a.getMonthYear(),
                        UITheme.formatCurrency(a.getLimitAmount()),
                        UITheme.formatCurrency(a.getSpentAmount()),
                        String.format("%.1f%%", a.getPercentage()),
                        a.getMessage().replace("\n", " -- ")
                });
            }

            countLabel.setText("Total Logged Alerts: " + alerts.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load alerts: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAlerts() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear your alerts log history?",
                "Clear Alerts",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            try {
                alertDAO.clearUserAlerts(currentUser.getId());
                refreshAlerts();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to clear alerts: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class AlertLevelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            String str = value != null ? value.toString() : "";
            label.setText(str);
            label.setFont(UITheme.FONT_SMALL_BOLD);

            if ("EXCEEDED".equals(str)) {
                label.setForeground(UITheme.DANGER);
            } else {
                label.setForeground(UITheme.WARNING_DARK);
            }
            return label;
        }
    }
}
