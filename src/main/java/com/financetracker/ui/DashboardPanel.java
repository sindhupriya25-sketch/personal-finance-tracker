package com.financetracker.ui;

import com.financetracker.dao.AlertDAO;
import com.financetracker.dao.BudgetDAO;
import com.financetracker.dao.TransactionDAO;
import com.financetracker.model.Budget;
import com.financetracker.model.BudgetAlert;
import com.financetracker.model.MonthlySummary;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import com.financetracker.service.SampleDataService;
import com.financetracker.ui.components.BudgetProgressBar;
import com.financetracker.ui.components.StatCard;
import com.financetracker.ui.components.TransactionDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Main dashboard view displaying financial KPIs, budget progress bars, active alert banners, and recent activity.
 */
public class DashboardPanel extends JPanel {
    private final User currentUser;
    private final MainFrame mainFrame;
    private final TransactionDAO transactionDAO;
    private final BudgetDAO budgetDAO;
    private final AlertDAO alertDAO;
    private final SampleDataService sampleDataService;

    private StatCard incomeCard;
    private StatCard expenseCard;
    private StatCard savingsCard;
    private StatCard balanceCard;

    private JPanel alertBannerPanel;
    private JLabel alertBannerText;
    private JPanel budgetListPanel;
    private JTable recentTable;
    private DefaultTableModel tableModel;

    private YearMonth selectedMonth;

    public DashboardPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame = mainFrame;
        this.transactionDAO = new TransactionDAO();
        this.budgetDAO = new BudgetDAO();
        this.alertDAO = new AlertDAO();
        this.sampleDataService = new SampleDataService();
        this.selectedMonth = YearMonth.now();

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 24, 24));

        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Main scrollable content
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(UITheme.BACKGROUND);

        // 1. Header Toolbar with Quick Actions
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.BACKGROUND);

        JPanel titleGrp = new JPanel();
        titleGrp.setLayout(new BoxLayout(titleGrp, BoxLayout.Y_AXIS));
        titleGrp.setBackground(UITheme.BACKGROUND);

        JLabel title = new JLabel("Financial Dashboard");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Overview of your cash flow, budgets, and spending health");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        titleGrp.add(title);
        titleGrp.add(Box.createVerticalStrut(2));
        titleGrp.add(subtitle);
        headerPanel.add(titleGrp, BorderLayout.WEST);

        // Actions on right
        JPanel actionGrp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionGrp.setBackground(UITheme.BACKGROUND);

        JButton sampleDataBtn = UITheme.createSecondaryButton("Generate 3-Month Demo Data");
        sampleDataBtn.setToolTipText("Populates 3 months of realistic transactions and budgets");
        sampleDataBtn.addActionListener(e -> generateSampleData());

        JButton addTxBtn = UITheme.createPrimaryButton("+ Add Transaction");
        addTxBtn.addActionListener(e -> openAddTransactionDialog());

        actionGrp.add(sampleDataBtn);
        actionGrp.add(addTxBtn);
        headerPanel.add(actionGrp, BorderLayout.EAST);

        mainContent.add(headerPanel);
        mainContent.add(Box.createVerticalStrut(14));

        // 2. Alert Banner (Collapsible/Dynamic)
        alertBannerPanel = new JPanel(new BorderLayout(12, 0));
        alertBannerPanel.setBackground(UITheme.WARNING_LIGHT);
        alertBannerPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.WARNING, 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
        alertBannerText = new JLabel("No active budget warnings.");
        alertBannerText.setFont(UITheme.FONT_BODY_BOLD);
        alertBannerText.setForeground(UITheme.WARNING_DARK);
        alertBannerPanel.add(alertBannerText, BorderLayout.CENTER);

        JButton viewAlertsBtn = new JButton("View Alerts Log");
        viewAlertsBtn.setFont(UITheme.FONT_SMALL_BOLD);
        viewAlertsBtn.setForeground(UITheme.PRIMARY);
        viewAlertsBtn.setContentAreaFilled(false);
        viewAlertsBtn.setBorderPainted(false);
        viewAlertsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewAlertsBtn.addActionListener(e -> mainFrame.showPanel("ALERTS"));
        alertBannerPanel.add(viewAlertsBtn, BorderLayout.EAST);

        mainContent.add(alertBannerPanel);
        mainContent.add(Box.createVerticalStrut(14));

        // 3. KPI Cards Grid (4 cards)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiGrid.setBackground(UITheme.BACKGROUND);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        incomeCard = new StatCard("TOTAL INCOME", "₹0.00", "Monthly inflows", "+", UITheme.SUCCESS, UITheme.SUCCESS_LIGHT);
        expenseCard = new StatCard("TOTAL EXPENSES", "₹0.00", "Monthly outflows", "-", UITheme.DANGER, UITheme.DANGER_LIGHT);
        savingsCard = new StatCard("TOTAL SAVINGS", "₹0.00", "Allocated to reserves", "S", UITheme.PRIMARY, UITheme.PRIMARY_LIGHT);
        balanceCard = new StatCard("NET BALANCE", "₹0.00", "Income - (Exp + Sav)", "=", UITheme.TEXT_MAIN, new Color(241, 245, 249));

        kpiGrid.add(incomeCard);
        kpiGrid.add(expenseCard);
        kpiGrid.add(savingsCard);
        kpiGrid.add(balanceCard);

        mainContent.add(kpiGrid);
        mainContent.add(Box.createVerticalStrut(18));

        // 4. Split Section: Left = Budget Progress Bars (40%), Right = Recent Transactions (60%)
        JPanel splitPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        splitPanel.setBackground(UITheme.BACKGROUND);

        // Budget Usage Card
        JPanel budgetCard = UITheme.createCardPanel();
        budgetCard.setLayout(new BorderLayout(0, 10));

        JPanel budgetHeader = new JPanel(new BorderLayout());
        budgetHeader.setBackground(UITheme.CARD_BG);
        JLabel budgetTitle = new JLabel("Category Budget Limits");
        budgetTitle.setFont(UITheme.FONT_HEADER);
        budgetTitle.setForeground(UITheme.TEXT_MAIN);
        budgetHeader.add(budgetTitle, BorderLayout.WEST);

        JButton manageBudgetsBtn = new JButton("Manage Budgets");
        manageBudgetsBtn.setFont(UITheme.FONT_SMALL_BOLD);
        manageBudgetsBtn.setForeground(UITheme.PRIMARY);
        manageBudgetsBtn.setContentAreaFilled(false);
        manageBudgetsBtn.setBorderPainted(false);
        manageBudgetsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        manageBudgetsBtn.addActionListener(e -> mainFrame.showPanel("BUDGETS"));
        budgetHeader.add(manageBudgetsBtn, BorderLayout.EAST);
        budgetCard.add(budgetHeader, BorderLayout.NORTH);

        budgetListPanel = new JPanel();
        budgetListPanel.setLayout(new BoxLayout(budgetListPanel, BoxLayout.Y_AXIS));
        budgetListPanel.setBackground(UITheme.CARD_BG);
        JScrollPane budgetScroll = new JScrollPane(budgetListPanel);
        budgetScroll.setBorder(null);
        budgetScroll.setPreferredSize(new Dimension(300, 260));
        budgetCard.add(budgetScroll, BorderLayout.CENTER);

        // Recent Transactions Card
        JPanel recentCard = UITheme.createCardPanel();
        recentCard.setLayout(new BorderLayout(0, 10));

        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setBackground(UITheme.CARD_BG);
        JLabel recentTitle = new JLabel("Recent Transactions");
        recentTitle.setFont(UITheme.FONT_HEADER);
        recentTitle.setForeground(UITheme.TEXT_MAIN);
        recentHeader.add(recentTitle, BorderLayout.WEST);

        JButton viewAllTxBtn = new JButton("View All");
        viewAllTxBtn.setFont(UITheme.FONT_SMALL_BOLD);
        viewAllTxBtn.setForeground(UITheme.PRIMARY);
        viewAllTxBtn.setContentAreaFilled(false);
        viewAllTxBtn.setBorderPainted(false);
        viewAllTxBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewAllTxBtn.addActionListener(e -> mainFrame.showPanel("TRANSACTIONS"));
        recentHeader.add(viewAllTxBtn, BorderLayout.EAST);
        recentCard.add(recentHeader, BorderLayout.NORTH);

        // Recent Table
        String[] cols = {"Date", "Type", "Category", "Description", "Amount"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        recentTable = new JTable(tableModel);
        recentTable.setFont(UITheme.FONT_BODY);
        recentTable.setRowHeight(32);
        recentTable.setShowGrid(false);
        recentTable.setIntercellSpacing(new Dimension(0, 0));
        recentTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        recentTable.getTableHeader().setBackground(new Color(241, 245, 249));

        // Center alignment for Date & Type, Right for Amount
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        recentTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        recentTable.getColumnModel().getColumn(1).setCellRenderer(new TransactionTypeCellRenderer());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        recentTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        JScrollPane tableScroll = new JScrollPane(recentTable);
        tableScroll.setBorder(new LineBorder(UITheme.BORDER, 1));
        tableScroll.setPreferredSize(new Dimension(350, 260));
        recentCard.add(tableScroll, BorderLayout.CENTER);

        splitPanel.add(budgetCard);
        splitPanel.add(recentCard);

        mainContent.add(splitPanel);

        JScrollPane outerScroll = new JScrollPane(mainContent);
        outerScroll.setBorder(null);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(outerScroll, BorderLayout.CENTER);
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

            // 1. Refresh Summary KPIs
            MonthlySummary summary = transactionDAO.getMonthlySummary(currentUser.getId(), year, month);
            incomeCard.setValue(UITheme.formatCurrency(summary.getTotalIncome()));
            expenseCard.setValue(UITheme.formatCurrency(summary.getTotalExpense()));
            savingsCard.setValue(UITheme.formatCurrency(summary.getTotalSavings()));

            double net = summary.getNetBalance();
            balanceCard.setValue(UITheme.formatCurrency(net));
            balanceCard.setValueColor(net >= 0 ? UITheme.SUCCESS_DARK : UITheme.DANGER_DARK);

            // 2. Refresh Budgets
            budgetListPanel.removeAll();
            List<Budget> budgets = budgetDAO.getBudgetUsages(currentUser.getId(), year, month);
            boolean hasExceeded = false;
            boolean hasWarning = false;
            StringBuilder bannerMsg = new StringBuilder();

            if (budgets.isEmpty()) {
                JLabel emptyLbl = new JLabel("No monthly budgets set yet. Click 'Manage Budgets' to set limits.");
                emptyLbl.setFont(UITheme.FONT_SMALL);
                emptyLbl.setForeground(UITheme.TEXT_MUTED);
                emptyLbl.setBorder(new EmptyBorder(16, 8, 16, 8));
                budgetListPanel.add(emptyLbl);
            } else {
                for (Budget b : budgets) {
                    budgetListPanel.add(new BudgetProgressBar(b));
                    if (b.isExceeded()) {
                        hasExceeded = true;
                        if (bannerMsg.length() > 0) bannerMsg.append(" | ");
                        bannerMsg.append(String.format("%s budget EXCEEDED (%s of %s)",
                                b.getCategoryName(), UITheme.formatCurrency(b.getCurrentSpent()), UITheme.formatCurrency(b.getMonthlyLimit())));
                    } else if (b.isWarning()) {
                        hasWarning = true;
                        if (bannerMsg.length() > 0) bannerMsg.append(" | ");
                        bannerMsg.append(String.format("%s budget at %.0f%% (%s of %s)",
                                b.getCategoryName(), b.getPercentageUsed(), UITheme.formatCurrency(b.getCurrentSpent()), UITheme.formatCurrency(b.getMonthlyLimit())));
                    }
                }
            }
            budgetListPanel.revalidate();
            budgetListPanel.repaint();

            // 3. Update Alert Banner
            if (hasExceeded) {
                alertBannerPanel.setVisible(true);
                alertBannerPanel.setBackground(UITheme.DANGER_LIGHT);
                alertBannerPanel.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(UITheme.DANGER, 1, true),
                        new EmptyBorder(10, 16, 10, 16)
                ));
                alertBannerText.setText("Alert: " + bannerMsg);
                alertBannerText.setForeground(UITheme.DANGER_DARK);
            } else if (hasWarning) {
                alertBannerPanel.setVisible(true);
                alertBannerPanel.setBackground(UITheme.WARNING_LIGHT);
                alertBannerPanel.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(UITheme.WARNING, 1, true),
                        new EmptyBorder(10, 16, 10, 16)
                ));
                alertBannerText.setText("Warning: " + bannerMsg);
                alertBannerText.setForeground(UITheme.WARNING_DARK);
            } else {
                alertBannerPanel.setVisible(false);
            }

            // 4. Refresh Recent Transactions Table (last 8)
            tableModel.setRowCount(0);
            List<Transaction> transactions = transactionDAO.getFiltered(
                    currentUser.getId(),
                    selectedMonth.atDay(1),
                    selectedMonth.atEndOfMonth(),
                    null, null, null
            );

            int count = 0;
            for (Transaction tx : transactions) {
                if (count++ >= 8) break;
                tableModel.addRow(new Object[]{
                        tx.getFormattedDate(),
                        tx.getType(),
                        tx.getCategoryName(),
                        tx.getDescription(),
                        UITheme.formatCurrency(tx.getAmount())
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load dashboard data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAddTransactionDialog() {
        TransactionDialog dialog = new TransactionDialog(mainFrame, currentUser, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            mainFrame.notifyDataChanged();
        }
    }

    private void generateSampleData() {
        int opt = JOptionPane.showConfirmDialog(this,
                "This will generate 3 months of realistic transactions and category budgets.\nOverwrite existing data?",
                "Generate Sample Data",
                JOptionPane.YES_NO_CANCEL_OPTION);

        if (opt == JOptionPane.YES_OPTION || opt == JOptionPane.NO_OPTION) {
            try {
                boolean clear = (opt == JOptionPane.YES_OPTION);
                sampleDataService.generateSampleData(currentUser.getId(), clear);
                JOptionPane.showMessageDialog(this,
                        "3 months of sample data generated successfully!\nCheck out your dashboard, charts, and budget reports.",
                        "Sample Data Ready",
                        JOptionPane.INFORMATION_MESSAGE);
                mainFrame.notifyDataChanged();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error generating sample data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Custom renderer to display transaction type badge.
     */
    private static class TransactionTypeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (value instanceof TransactionType type) {
                label.setText(type.getDisplayName());
                label.setForeground(UITheme.getTypeColor(type));
                label.setFont(UITheme.FONT_SMALL_BOLD);
            }
            return label;
        }
    }
}
