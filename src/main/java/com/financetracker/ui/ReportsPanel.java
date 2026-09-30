package com.financetracker.ui;

import com.financetracker.dao.TransactionDAO;
import com.financetracker.model.CategorySpend;
import com.financetracker.model.MonthlySummary;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import com.financetracker.service.ReportService;
import com.financetracker.ui.components.StatCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Financial reporting view for generating comprehensive summaries, category breakdowns, and PDF/CSV exports.
 */
public class ReportsPanel extends JPanel {
    private final User currentUser;
    private final MainFrame mainFrame;
    private final TransactionDAO transactionDAO;
    private final ReportService reportService;

    private JComboBox<String> periodCombo;
    private JTextField fromField;
    private JTextField toField;

    private StatCard incomeCard;
    private StatCard expenseCard;
    private StatCard savingsCard;
    private StatCard balanceCard;
    private StatCard rateCard;

    private JTable breakdownTable;
    private DefaultTableModel tableModel;

    private List<CategorySpend> currentSpends = new ArrayList<>();
    private List<Transaction> currentTransactions = new ArrayList<>();
    private MonthlySummary currentSummary;
    private LocalDate activeFromDate;
    private LocalDate activeToDate;

    public ReportsPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame = mainFrame;
        this.transactionDAO = new TransactionDAO();
        this.reportService = new ReportService();

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        refreshReport();
    }

    private void initComponents() {
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(UITheme.BACKGROUND);

        // 1. Top Header & Export Buttons
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.BACKGROUND);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setBackground(UITheme.BACKGROUND);

        JLabel title = new JLabel("Financial Reports & Statements");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Generate detailed balance sheets, category distributions, and export official reports");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(subtitle);
        headerPanel.add(headerText, BorderLayout.WEST);

        // Export Buttons
        JPanel exportGrp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        exportGrp.setBackground(UITheme.BACKGROUND);

        JButton exportCsvBtn = UITheme.createSecondaryButton("Export CSV");
        exportCsvBtn.addActionListener(e -> exportCSV());

        JButton exportPdfBtn = UITheme.createPrimaryButton("Export PDF Report");
        exportPdfBtn.addActionListener(e -> exportPDF());

        exportGrp.add(exportCsvBtn);
        exportGrp.add(exportPdfBtn);
        headerPanel.add(exportGrp, BorderLayout.EAST);

        mainContent.add(headerPanel);
        mainContent.add(Box.createVerticalStrut(12));

        // 2. Filter Toolbar (Period Selector)
        JPanel filterCard = UITheme.createCardPanel();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));

        filterCard.add(new JLabel("Report Period:"));
        periodCombo = new JComboBox<>(new String[]{
                "Current Month",
                "Previous Month",
                "Last 3 Months",
                "Year to Date (Current Year)",
                "All Time",
                "Custom Date Range"
        });
        periodCombo.setFont(UITheme.FONT_BODY);
        periodCombo.addActionListener(e -> onPeriodChanged());
        filterCard.add(periodCombo);

        filterCard.add(new JLabel("From:"));
        fromField = new JTextField(8);
        fromField.setFont(UITheme.FONT_BODY);
        fromField.setEnabled(false);
        filterCard.add(fromField);

        filterCard.add(new JLabel("To:"));
        toField = new JTextField(8);
        toField.setFont(UITheme.FONT_BODY);
        toField.setEnabled(false);
        filterCard.add(toField);

        JButton applyBtn = UITheme.createSecondaryButton("Generate");
        applyBtn.addActionListener(e -> refreshReport());
        filterCard.add(applyBtn);

        mainContent.add(filterCard);
        mainContent.add(Box.createVerticalStrut(14));

        // 3. Executive KPI Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 12, 0));
        kpiGrid.setBackground(UITheme.BACKGROUND);
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));

        incomeCard = new StatCard("INCOME", "₹0.00", "Total inflows", "+", UITheme.SUCCESS, UITheme.SUCCESS_LIGHT);
        expenseCard = new StatCard("EXPENSES", "₹0.00", "Total outflows", "-", UITheme.DANGER, UITheme.DANGER_LIGHT);
        savingsCard = new StatCard("SAVINGS", "₹0.00", "Total reserves", "S", UITheme.PRIMARY, UITheme.PRIMARY_LIGHT);
        balanceCard = new StatCard("NET BALANCE", "₹0.00", "Net cash flow", "=", UITheme.TEXT_MAIN, new Color(241, 245, 249));
        rateCard = new StatCard("SAVINGS RATE", "0.0%", "Savings / Income", "%", new Color(109, 40, 217), new Color(245, 243, 255));

        kpiGrid.add(incomeCard);
        kpiGrid.add(expenseCard);
        kpiGrid.add(savingsCard);
        kpiGrid.add(balanceCard);
        kpiGrid.add(rateCard);

        mainContent.add(kpiGrid);
        mainContent.add(Box.createVerticalStrut(16));

        // 4. Category Breakdown Table Card
        JPanel tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JLabel breakdownTitle = new JLabel("Category-Wise Financial Breakdown");
        breakdownTitle.setFont(UITheme.FONT_HEADER);
        breakdownTitle.setForeground(UITheme.TEXT_MAIN);
        tableCard.add(breakdownTitle, BorderLayout.NORTH);

        String[] cols = {"Category Name", "Classification Type", "Total Amount ($)", "Transaction Count", "% of Total Outflow/Inflow"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        breakdownTable = new JTable(tableModel);
        breakdownTable.setFont(UITheme.FONT_BODY);
        breakdownTable.setRowHeight(34);
        breakdownTable.setShowGrid(false);
        breakdownTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        breakdownTable.getTableHeader().setBackground(new Color(241, 245, 249));

        breakdownTable.getColumnModel().getColumn(1).setCellRenderer(new TransactionTypeCellRenderer());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        breakdownTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        breakdownTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        breakdownTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(breakdownTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER, 1));
        scrollPane.setPreferredSize(new Dimension(500, 260));
        tableCard.add(scrollPane, BorderLayout.CENTER);

        mainContent.add(tableCard);

        JScrollPane outerScroll = new JScrollPane(mainContent);
        outerScroll.setBorder(null);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(outerScroll, BorderLayout.CENTER);
    }

    private void onPeriodChanged() {
        String period = (String) periodCombo.getSelectedItem();
        boolean isCustom = "Custom Date Range".equals(period);
        fromField.setEnabled(isCustom);
        toField.setEnabled(isCustom);

        LocalDate now = LocalDate.now();
        YearMonth currentYm = YearMonth.from(now);

        if ("Current Month".equals(period)) {
            fromField.setText(currentYm.atDay(1).toString());
            toField.setText(currentYm.atEndOfMonth().toString());
        } else if ("Previous Month".equals(period)) {
            YearMonth prev = currentYm.minusMonths(1);
            fromField.setText(prev.atDay(1).toString());
            toField.setText(prev.atEndOfMonth().toString());
        } else if ("Last 3 Months".equals(period)) {
            fromField.setText(currentYm.minusMonths(2).atDay(1).toString());
            toField.setText(currentYm.atEndOfMonth().toString());
        } else if ("Year to Date (Current Year)".equals(period)) {
            fromField.setText(LocalDate.of(now.getYear(), 1, 1).toString());
            toField.setText(now.toString());
        } else if ("All Time".equals(period)) {
            fromField.setText("");
            toField.setText("");
        }

        if (!isCustom) {
            refreshReport();
        }
    }

    public void refreshReport() {
        if (currentUser == null) return;

        try {
            String fromStr = fromField.getText().trim();
            String toStr = toField.getText().trim();

            activeFromDate = !fromStr.isEmpty() ? LocalDate.parse(fromStr) : null;
            activeToDate = !toStr.isEmpty() ? LocalDate.parse(toStr) : null;

            // Fetch filtered transactions
            currentTransactions = transactionDAO.getFiltered(
                    currentUser.getId(), activeFromDate, activeToDate, null, null, null
            );

            // Compute KPIs
            double totalIncome = 0;
            double totalExpense = 0;
            double totalSavings = 0;

            for (Transaction tx : currentTransactions) {
                if (tx.getType() == TransactionType.INCOME) totalIncome += tx.getAmount();
                else if (tx.getType() == TransactionType.EXPENSE) totalExpense += tx.getAmount();
                else if (tx.getType() == TransactionType.SAVINGS) totalSavings += tx.getAmount();
            }

            double net = totalIncome - totalExpense - totalSavings;
            double rate = totalIncome > 0 ? (totalSavings / totalIncome) * 100.0 : 0.0;

            currentSummary = new MonthlySummary("Report Period", totalIncome, totalExpense, totalSavings);

            incomeCard.setValue(UITheme.formatCurrency(totalIncome));
            expenseCard.setValue(UITheme.formatCurrency(totalExpense));
            savingsCard.setValue(UITheme.formatCurrency(totalSavings));
            balanceCard.setValue(UITheme.formatCurrency(net));
            balanceCard.setValueColor(net >= 0 ? UITheme.SUCCESS_DARK : UITheme.DANGER_DARK);
            rateCard.setValue(String.format("%.1f%%", rate));

            // Fetch Category Spends Breakdown
            currentSpends = transactionDAO.getCategorySpends(currentUser.getId(), activeFromDate, activeToDate, null);

            tableModel.setRowCount(0);
            for (CategorySpend cs : currentSpends) {
                tableModel.addRow(new Object[]{
                        cs.getCategoryName(),
                        cs.getType(),
                        UITheme.formatCurrency(cs.getTotalAmount()),
                        cs.getTransactionCount(),
                        String.format("%.1f%%", cs.getPercentageOfTotal())
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage(), "Report Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCSV() {
        if (currentTransactions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transaction records found for the selected period.", "Empty Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Financial_Report_" + LocalDate.now() + ".csv"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".csv")) {
                target = new File(target.getAbsolutePath() + ".csv");
            }
            try {
                reportService.exportToCSV(target, currentTransactions);
                JOptionPane.showMessageDialog(this,
                        "Report exported successfully to CSV:\n" + target.getAbsolutePath(),
                        "Export Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportPDF() {
        if (currentTransactions.isEmpty() && currentSpends.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transaction records found for the selected period.", "Empty Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Financial_Report_" + LocalDate.now() + ".pdf"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".pdf")) {
                target = new File(target.getAbsolutePath() + ".pdf");
            }
            try {
                reportService.exportToPDF(target, currentUser, activeFromDate, activeToDate, currentTransactions, currentSpends);
                JOptionPane.showMessageDialog(this,
                        "Official PDF Report generated successfully!\nFile: " + target.getAbsolutePath(),
                        "PDF Generation Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to generate PDF report: " + e.getMessage(), "PDF Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

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
