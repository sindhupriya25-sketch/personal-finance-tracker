package com.financetracker.ui.components;

import com.financetracker.dao.CategoryDAO;
import com.financetracker.dao.TransactionDAO;
import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import com.financetracker.service.BudgetAlertService;
import com.financetracker.service.BudgetAlertService.BudgetCheckResult;
import com.financetracker.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Modal dialog for creating and editing income, expense, and savings transactions with real-time budget alert checking.
 */
public class TransactionDialog extends JDialog {
    private final User currentUser;
    private final Transaction editingTransaction;
    private final CategoryDAO categoryDAO;
    private final TransactionDAO transactionDAO;
    private final BudgetAlertService alertService;
    private boolean saved = false;

    private JComboBox<TransactionType> typeCombo;
    private JComboBox<Category> categoryCombo;
    private JTextField amountField;
    private JTextField dateField;
    private JTextField descriptionField;
    private JLabel errorLabel;

    public TransactionDialog(Window parent, User currentUser, Transaction transactionToEdit) {
        super(parent, transactionToEdit == null ? "Add Transaction" : "Edit Transaction", ModalityType.APPLICATION_MODAL);
        this.currentUser = currentUser;
        this.editingTransaction = transactionToEdit;
        this.categoryDAO = new CategoryDAO();
        this.transactionDAO = new TransactionDAO();
        this.alertService = new BudgetAlertService();

        initComponents();
        populateFields();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(0, 16));
        contentPane.setBackground(UITheme.CARD_BG);
        contentPane.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(contentPane);

        // Header Title
        JLabel titleLabel = new JLabel(editingTransaction == null ? "Record New Transaction" : "Edit Transaction");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);
        contentPane.add(titleLabel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.weightx = 1.0;

        // 1. Transaction Type
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Type:"), gbc);
        typeCombo = new JComboBox<>(TransactionType.values());
        typeCombo.setFont(UITheme.FONT_BODY);
        typeCombo.addActionListener(e -> updateCategoriesForSelectedType());
        gbc.gridx = 1;
        formPanel.add(typeCombo, gbc);

        // 2. Category
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Category:"), gbc);
        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(UITheme.FONT_BODY);
        gbc.gridx = 1;
        formPanel.add(categoryCombo, gbc);

        // 3. Amount
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Amount (₹):"), gbc);
        amountField = new JTextField(15);
        amountField.setFont(UITheme.FONT_BODY);
        gbc.gridx = 1;
        formPanel.add(amountField, gbc);

        // 4. Date
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        dateField = new JTextField(LocalDate.now().toString(), 15);
        dateField.setFont(UITheme.FONT_BODY);
        gbc.gridx = 1;
        formPanel.add(dateField, gbc);

        // 5. Description
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Description:"), gbc);
        descriptionField = new JTextField(15);
        descriptionField.setFont(UITheme.FONT_BODY);
        gbc.gridx = 1;
        formPanel.add(descriptionField, gbc);

        // Error message
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        formPanel.add(errorLabel, gbc);

        contentPane.add(formPanel, BorderLayout.CENTER);

        // Button Bar
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonBar.setBackground(UITheme.CARD_BG);

        JButton cancelButton = UITheme.createSecondaryButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = UITheme.createPrimaryButton("Save Transaction");
        saveButton.addActionListener(e -> onSave());

        buttonBar.add(cancelButton);
        buttonBar.add(saveButton);
        contentPane.add(buttonBar, BorderLayout.SOUTH);
    }

    private void updateCategoriesForSelectedType() {
        TransactionType selectedType = (TransactionType) typeCombo.getSelectedItem();
        if (selectedType == null) return;

        Category previouslySelected = (Category) categoryCombo.getSelectedItem();
        categoryCombo.removeAllItems();

        try {
            List<Category> categories = categoryDAO.getByType(selectedType);
            for (Category cat : categories) {
                categoryCombo.addItem(cat);
            }
            if (previouslySelected != null) {
                for (int i = 0; i < categoryCombo.getItemCount(); i++) {
                    if (categoryCombo.getItemAt(i).getId() == previouslySelected.getId()) {
                        categoryCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            errorLabel.setText("Failed to load categories: " + e.getMessage());
        }
    }

    private void populateFields() {
        updateCategoriesForSelectedType();

        if (editingTransaction != null) {
            typeCombo.setSelectedItem(editingTransaction.getType());
            updateCategoriesForSelectedType();

            for (int i = 0; i < categoryCombo.getItemCount(); i++) {
                if (categoryCombo.getItemAt(i).getId() == editingTransaction.getCategoryId()) {
                    categoryCombo.setSelectedIndex(i);
                    break;
                }
            }

            amountField.setText(String.format(java.util.Locale.US, "%.2f", editingTransaction.getAmount()));
            dateField.setText(editingTransaction.getFormattedDate());
            descriptionField.setText(editingTransaction.getDescription());
        }
    }

    private void onSave() {
        errorLabel.setText(" ");
        String amtStr = amountField.getText().trim();
        String dateStr = dateField.getText().trim();
        String desc = descriptionField.getText().trim();

        if (amtStr.isEmpty()) {
            errorLabel.setText("Please enter a transaction amount.");
            amountField.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount <= 0) {
                errorLabel.setText("Amount must be greater than zero.");
                amountField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid numeric amount (e.g. 25.50).");
            amountField.requestFocus();
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            errorLabel.setText("Date must be in YYYY-MM-DD format.");
            dateField.requestFocus();
            return;
        }

        Category selectedCategory = (Category) categoryCombo.getSelectedItem();
        if (selectedCategory == null) {
            errorLabel.setText("Please select a category.");
            categoryCombo.requestFocus();
            return;
        }

        TransactionType type = (TransactionType) typeCombo.getSelectedItem();

        try {
            if (editingTransaction == null) {
                Transaction tx = new Transaction(
                        currentUser.getId(),
                        selectedCategory.getId(),
                        type,
                        amount,
                        desc,
                        date
                );
                transactionDAO.create(tx);
            } else {
                editingTransaction.setCategoryId(selectedCategory.getId());
                editingTransaction.setType(type);
                editingTransaction.setAmount(amount);
                editingTransaction.setDescription(desc);
                editingTransaction.setDate(date);
                transactionDAO.update(editingTransaction);
            }

            saved = true;

            // Trigger real-time budget alert check if expense or savings
            if (type == TransactionType.EXPENSE || type == TransactionType.SAVINGS) {
                BudgetCheckResult checkResult = alertService.checkBudget(currentUser.getId(), selectedCategory.getId(), date);
                if (checkResult.isAlert()) {
                    if (checkResult.isExceeded()) {
                        JOptionPane.showMessageDialog(this,
                                checkResult.getMessage(),
                                "Budget Exceeded Alert",
                                JOptionPane.ERROR_MESSAGE);
                    } else if (checkResult.isWarning()) {
                        JOptionPane.showMessageDialog(this,
                                checkResult.getMessage(),
                                "Budget Warning (>= 80%)",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
            }

            dispose();
        } catch (SQLException e) {
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
