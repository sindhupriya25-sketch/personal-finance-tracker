package com.financetracker.ui;

import com.financetracker.dao.CategoryDAO;
import com.financetracker.dao.TransactionDAO;
import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import com.financetracker.service.ReportService;
import com.financetracker.ui.components.TransactionDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction management panel supporting full CRUD, multi-criteria filtering, search, and CSV export.
 */
public class TransactionPanel extends JPanel {
    private final User currentUser;
    private final MainFrame mainFrame;
    private final TransactionDAO transactionDAO;
    private final CategoryDAO categoryDAO;
    private final ReportService reportService;

    private List<Transaction> currentTransactions = new ArrayList<>();
    private JTable transactionTable;
    private DefaultTableModel tableModel;

    private JTextField searchField;
    private JComboBox<String> typeFilterCombo;
    private JComboBox<Object> categoryFilterCombo;
    private JTextField fromDateField;
    private JTextField toDateField;

    private JButton editBtn;
    private JButton deleteBtn;
    private JLabel countAndTotalLabel;

    public TransactionPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame = mainFrame;
        this.transactionDAO = new TransactionDAO();
        this.categoryDAO = new CategoryDAO();
        this.reportService = new ReportService();

        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        loadCategoriesFilter();
        refreshData();
    }

    private void initComponents() {
        // 1. Top Header & Action Buttons
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setBackground(UITheme.BACKGROUND);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setBackground(UITheme.BACKGROUND);

        JLabel title = new JLabel("Transactions Log");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Record, categorize, filter, and audit your financial activities");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(subtitle);
        topPanel.add(headerText, BorderLayout.WEST);

        // Action Buttons
        JPanel actionGrp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionGrp.setBackground(UITheme.BACKGROUND);

        JButton exportCsvBtn = UITheme.createSecondaryButton("Export CSV");
        exportCsvBtn.addActionListener(e -> exportToCSV());

        editBtn = UITheme.createSecondaryButton("Edit");
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> editSelectedTransaction());

        deleteBtn = UITheme.createDangerButton("Delete");
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> deleteSelectedTransaction());

        JButton addBtn = UITheme.createPrimaryButton("+ New Transaction");
        addBtn.addActionListener(e -> openNewTransactionDialog());

        actionGrp.add(exportCsvBtn);
        actionGrp.add(editBtn);
        actionGrp.add(deleteBtn);
        actionGrp.add(addBtn);
        topPanel.add(actionGrp, BorderLayout.EAST);

        // 2. Filter Bar (Card)
        JPanel filterCard = UITheme.createCardPanel();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));

        // Search Input
        filterCard.add(new JLabel("Search:"));
        searchField = new JTextField(12);
        searchField.setFont(UITheme.FONT_BODY);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshData(); }
            public void removeUpdate(DocumentEvent e) { refreshData(); }
            public void changedUpdate(DocumentEvent e) { refreshData(); }
        });
        filterCard.add(searchField);

        // Type Filter
        filterCard.add(new JLabel("Type:"));
        typeFilterCombo = new JComboBox<>(new String[]{"All Types", "INCOME", "EXPENSE", "SAVINGS"});
        typeFilterCombo.setFont(UITheme.FONT_BODY);
        typeFilterCombo.addActionListener(e -> refreshData());
        filterCard.add(typeFilterCombo);

        // Category Filter
        filterCard.add(new JLabel("Category:"));
        categoryFilterCombo = new JComboBox<>();
        categoryFilterCombo.setFont(UITheme.FONT_BODY);
        categoryFilterCombo.addItem("All Categories");
        categoryFilterCombo.addActionListener(e -> refreshData());
        filterCard.add(categoryFilterCombo);

        // Date Range
        filterCard.add(new JLabel("From:"));
        fromDateField = new JTextField(8);
        fromDateField.setFont(UITheme.FONT_BODY);
        fromDateField.setToolTipText("YYYY-MM-DD");
        fromDateField.addActionListener(e -> refreshData());
        filterCard.add(fromDateField);

        filterCard.add(new JLabel("To:"));
        toDateField = new JTextField(8);
        toDateField.setFont(UITheme.FONT_BODY);
        toDateField.setToolTipText("YYYY-MM-DD");
        toDateField.addActionListener(e -> refreshData());
        filterCard.add(toDateField);

        JButton resetFiltersBtn = UITheme.createSecondaryButton("Reset");
        resetFiltersBtn.addActionListener(e -> resetFilters());
        filterCard.add(resetFiltersBtn);

        topPanel.add(filterCard, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // 3. Table Card
        JPanel tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 8));

        String[] cols = {"ID", "Date", "Type", "Category", "Description", "Amount (₹)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(tableModel);
        transactionTable.setFont(UITheme.FONT_BODY);
        transactionTable.setRowHeight(34);
        transactionTable.setShowVerticalLines(false);
        transactionTable.setShowHorizontalLines(true);
        transactionTable.setGridColor(new Color(241, 245, 249));
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        transactionTable.getTableHeader().setBackground(new Color(241, 245, 249));

        // Column Renderers & Sizing
        transactionTable.getColumnModel().getColumn(0).setMaxWidth(60); // ID
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        transactionTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        transactionTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Date

        transactionTable.getColumnModel().getColumn(2).setCellRenderer(new TransactionTypeCellRenderer()); // Type

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        transactionTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Amount

        // Selection Listener
        transactionTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSel = transactionTable.getSelectedRow() >= 0;
            editBtn.setEnabled(hasSel);
            deleteBtn.setEnabled(hasSel);
        });

        // Double click to edit
        transactionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && transactionTable.getSelectedRow() >= 0) {
                    editSelectedTransaction();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER, 1));
        tableCard.add(scrollPane, BorderLayout.CENTER);

        // Footer Summary Label
        countAndTotalLabel = new JLabel("Showing 0 transactions | Total Net: ₹0.00");
        countAndTotalLabel.setFont(UITheme.FONT_SMALL_BOLD);
        countAndTotalLabel.setForeground(UITheme.TEXT_MUTED);
        tableCard.add(countAndTotalLabel, BorderLayout.SOUTH);

        add(tableCard, BorderLayout.CENTER);
    }

    private void loadCategoriesFilter() {
        try {
            List<Category> categories = categoryDAO.getAll();
            categoryFilterCombo.removeAllItems();
            categoryFilterCombo.addItem("All Categories");
            for (Category c : categories) {
                categoryFilterCombo.addItem(c);
            }
        } catch (SQLException e) {
            System.err.println("Error loading categories filter: " + e.getMessage());
        }
    }

    private void resetFilters() {
        searchField.setText("");
        typeFilterCombo.setSelectedIndex(0);
        categoryFilterCombo.setSelectedIndex(0);
        fromDateField.setText("");
        toDateField.setText("");
        refreshData();
    }

    public void setDateRange(LocalDate from, LocalDate to) {
        fromDateField.setText(from != null ? from.toString() : "");
        toDateField.setText(to != null ? to.toString() : "");
        refreshData();
    }

    public void refreshData() {
        if (currentUser == null) return;

        LocalDate fromDate = null;
        if (!fromDateField.getText().trim().isEmpty()) {
            try {
                fromDate = LocalDate.parse(fromDateField.getText().trim());
            } catch (DateTimeParseException ignored) {
            }
        }

        LocalDate toDate = null;
        if (!toDateField.getText().trim().isEmpty()) {
            try {
                toDate = LocalDate.parse(toDateField.getText().trim());
            } catch (DateTimeParseException ignored) {
            }
        }

        Integer catId = null;
        Object selectedCat = categoryFilterCombo.getSelectedItem();
        if (selectedCat instanceof Category cat) {
            catId = cat.getId();
        }

        TransactionType type = null;
        String typeSel = (String) typeFilterCombo.getSelectedItem();
        if (typeSel != null && !typeSel.equals("All Types")) {
            type = TransactionType.valueOf(typeSel);
        }

        String search = searchField.getText().trim();

        try {
            currentTransactions = transactionDAO.getFiltered(
                    currentUser.getId(), fromDate, toDate, catId, type, search
            );

            tableModel.setRowCount(0);
            double totalIncome = 0;
            double totalExpense = 0;
            double totalSavings = 0;

            for (Transaction tx : currentTransactions) {
                tableModel.addRow(new Object[]{
                        tx.getId(),
                        tx.getFormattedDate(),
                        tx.getType(),
                        tx.getCategoryName(),
                        tx.getDescription(),
                        UITheme.formatCurrency(tx.getAmount())
                });

                if (tx.getType() == TransactionType.INCOME) totalIncome += tx.getAmount();
                else if (tx.getType() == TransactionType.EXPENSE) totalExpense += tx.getAmount();
                else if (tx.getType() == TransactionType.SAVINGS) totalSavings += tx.getAmount();
            }

            countAndTotalLabel.setText(String.format(
                    "Showing %d transactions  |  Income: %s  |  Expenses: %s  |  Savings: %s  |  Net: %s",
                    currentTransactions.size(),
                    UITheme.formatCurrency(totalIncome),
                    UITheme.formatCurrency(totalExpense),
                    UITheme.formatCurrency(totalSavings),
                    UITheme.formatCurrency(totalIncome - totalExpense - totalSavings)
            ));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load transactions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openNewTransactionDialog() {
        TransactionDialog dialog = new TransactionDialog(mainFrame, currentUser, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            mainFrame.notifyDataChanged();
        }
    }

    private void editSelectedTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentTransactions.size()) return;

        Transaction tx = currentTransactions.get(selectedRow);
        TransactionDialog dialog = new TransactionDialog(mainFrame, currentUser, tx);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            mainFrame.notifyDataChanged();
        }
    }

    private void deleteSelectedTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentTransactions.size()) return;

        Transaction tx = currentTransactions.get(selectedRow);
        int opt = JOptionPane.showConfirmDialog(this,
                String.format("Are you sure you want to delete transaction #%d (%s - %s: %s)?",
                        tx.getId(), tx.getFormattedDate(), tx.getCategoryName(), UITheme.formatCurrency(tx.getAmount())),
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            try {
                transactionDAO.delete(tx.getId(), currentUser.getId());
                mainFrame.notifyDataChanged();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to delete transaction: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportToCSV() {
        if (currentTransactions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transactions available to export.", "Empty Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("transactions_" + LocalDate.now() + ".csv"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            try {
                reportService.exportToCSV(file, currentTransactions);
                JOptionPane.showMessageDialog(this,
                        "Successfully exported " + currentTransactions.size() + " transactions to CSV:\n" + file.getAbsolutePath(),
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
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
