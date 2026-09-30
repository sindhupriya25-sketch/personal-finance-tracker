package com.financetracker.ui;

import com.financetracker.dao.BudgetDAO;
import com.financetracker.model.Budget;
import com.financetracker.model.User;
import com.financetracker.ui.components.BudgetDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Budget management view allowing users to configure category limits and monitor budget health.
 */
public class BudgetPanel extends JPanel {
    private final User currentUser;
    private final MainFrame mainFrame;
    private final BudgetDAO budgetDAO;

    private List<Budget> currentBudgets = new ArrayList<>();
    private JTable budgetTable;
    private DefaultTableModel tableModel;

    private JButton editBtn;
    private JButton deleteBtn;
    private JLabel summaryLabel;
    private YearMonth selectedMonth;

    public BudgetPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame = mainFrame;
        this.budgetDAO = new BudgetDAO();
        this.selectedMonth = YearMonth.now();

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Top Toolbar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UITheme.BACKGROUND);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setBackground(UITheme.BACKGROUND);

        JLabel title = new JLabel("Budget Management");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Establish monthly spending limits per category and track expenditure thresholds");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(subtitle);
        topPanel.add(headerText, BorderLayout.WEST);

        // Action Buttons
        JPanel actionGrp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionGrp.setBackground(UITheme.BACKGROUND);

        editBtn = UITheme.createSecondaryButton("Edit Limit");
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> editSelectedBudget());

        deleteBtn = UITheme.createDangerButton("Remove");
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> deleteSelectedBudget());

        JButton addBtn = UITheme.createPrimaryButton("+ Set Category Budget");
        addBtn.addActionListener(e -> openSetBudgetDialog());

        actionGrp.add(editBtn);
        actionGrp.add(deleteBtn);
        actionGrp.add(addBtn);
        topPanel.add(actionGrp, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Budget Table Card
        JPanel cardPanel = UITheme.createCardPanel();
        cardPanel.setLayout(new BorderLayout(0, 10));

        String[] cols = {"Category", "Monthly Limit", "Spent (This Month)", "Remaining", "Usage %", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        budgetTable = new JTable(tableModel);
        budgetTable.setFont(UITheme.FONT_BODY);
        budgetTable.setRowHeight(36);
        budgetTable.setShowGrid(false);
        budgetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        budgetTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        budgetTable.getTableHeader().setBackground(new Color(241, 245, 249));

        // Format columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        budgetTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        budgetTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        budgetTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        budgetTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        budgetTable.getColumnModel().getColumn(5).setCellRenderer(new BudgetStatusRenderer());

        budgetTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSel = budgetTable.getSelectedRow() >= 0;
            editBtn.setEnabled(hasSel);
            deleteBtn.setEnabled(hasSel);
        });

        budgetTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && budgetTable.getSelectedRow() >= 0) {
                    editSelectedBudget();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(budgetTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER, 1));
        cardPanel.add(scrollPane, BorderLayout.CENTER);

        summaryLabel = new JLabel("Total Budgeted: ₹0.00 | Total Spent: ₹0.00");
        summaryLabel.setFont(UITheme.FONT_SMALL_BOLD);
        summaryLabel.setForeground(UITheme.TEXT_MUTED);
        cardPanel.add(summaryLabel, BorderLayout.SOUTH);

        add(cardPanel, BorderLayout.CENTER);
    }

    public void setMonth(YearMonth month) {
        this.selectedMonth = month;
        refreshData();
    }

    public void refreshData() {
        if (currentUser == null) return;

        try {
            int year = selectedMonth.getYear();
            int month = selectedMonth.getMonthValue();
            currentBudgets = budgetDAO.getBudgetUsages(currentUser.getId(), year, month);

            tableModel.setRowCount(0);
            double totalBudget = 0;
            double totalSpent = 0;

            for (Budget b : currentBudgets) {
                double pct = b.getPercentageUsed();
                String status = pct >= 100.0 ? "EXCEEDED" : (pct >= 80.0 ? "WARNING" : "ON TRACK");

                tableModel.addRow(new Object[]{
                        b.getCategoryName(),
                        UITheme.formatCurrency(b.getMonthlyLimit()),
                        UITheme.formatCurrency(b.getCurrentSpent()),
                        UITheme.formatCurrency(b.getRemaining()),
                        String.format("%.1f%%", pct),
                        status
                });

                totalBudget += b.getMonthlyLimit();
                totalSpent += b.getCurrentSpent();
            }

            summaryLabel.setText(String.format("Configured Budgets: %d  |  Total Monthly Limit: %s  |  Actual Spent: %s  |  Remaining: %s",
                    currentBudgets.size(),
                    UITheme.formatCurrency(totalBudget),
                    UITheme.formatCurrency(totalSpent),
                    UITheme.formatCurrency(totalBudget - totalSpent)));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load budgets: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSetBudgetDialog() {
        BudgetDialog dialog = new BudgetDialog(mainFrame, currentUser, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            mainFrame.notifyDataChanged();
        }
    }

    private void editSelectedBudget() {
        int selectedRow = budgetTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentBudgets.size()) return;

        Budget budget = currentBudgets.get(selectedRow);
        BudgetDialog dialog = new BudgetDialog(mainFrame, currentUser, budget);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            mainFrame.notifyDataChanged();
        }
    }

    private void deleteSelectedBudget() {
        int selectedRow = budgetTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentBudgets.size()) return;

        Budget budget = currentBudgets.get(selectedRow);
        int opt = JOptionPane.showConfirmDialog(this,
                String.format("Are you sure you want to remove the budget for '%s' ($%s/mo)?",
                        budget.getCategoryName(), UITheme.formatCurrency(budget.getMonthlyLimit())),
                "Confirm Remove Budget",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            try {
                budgetDAO.deleteBudget(budget.getId(), currentUser.getId());
                mainFrame.notifyDataChanged();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to delete budget: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class BudgetStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            String str = value != null ? value.toString() : "";
            label.setText(str);
            label.setFont(UITheme.FONT_SMALL_BOLD);

            if ("EXCEEDED".equals(str)) {
                label.setForeground(UITheme.DANGER);
            } else if ("WARNING".equals(str)) {
                label.setForeground(UITheme.WARNING_DARK);
            } else {
                label.setForeground(UITheme.SUCCESS_DARK);
            }
            return label;
        }
    }
}
