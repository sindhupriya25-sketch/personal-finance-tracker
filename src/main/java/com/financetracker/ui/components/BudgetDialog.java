package com.financetracker.ui.components;

import com.financetracker.dao.BudgetDAO;
import com.financetracker.dao.CategoryDAO;
import com.financetracker.model.Budget;
import com.financetracker.model.Category;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import com.financetracker.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Modal dialog for configuring monthly budget limits per category.
 */
public class BudgetDialog extends JDialog {
    private final User currentUser;
    private final Budget editingBudget;
    private final CategoryDAO categoryDAO;
    private final BudgetDAO budgetDAO;
    private boolean saved = false;

    private JComboBox<Category> categoryCombo;
    private JTextField limitField;
    private JLabel errorLabel;

    public BudgetDialog(Window parent, User currentUser, Budget budgetToEdit) {
        super(parent, budgetToEdit == null ? "Set Monthly Budget" : "Edit Budget", ModalityType.APPLICATION_MODAL);
        this.currentUser = currentUser;
        this.editingBudget = budgetToEdit;
        this.categoryDAO = new CategoryDAO();
        this.budgetDAO = new BudgetDAO();

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

        JLabel titleLabel = new JLabel(editingBudget == null ? "Set Category Monthly Budget" : "Update Budget Limit");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);
        contentPane.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 4, 8, 4);
        gbc.weightx = 1.0;

        // Category
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Category:"), gbc);
        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(UITheme.FONT_BODY);
        gbc.gridx = 1;
        formPanel.add(categoryCombo, gbc);

        // Limit
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Monthly Limit (₹):"), gbc);
        limitField = new JTextField(15);
        limitField.setFont(UITheme.FONT_BODY);
        gbc.gridx = 1;
        formPanel.add(limitField, gbc);

        // Error
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(errorLabel, gbc);

        contentPane.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonBar.setBackground(UITheme.CARD_BG);

        JButton cancelButton = UITheme.createSecondaryButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = UITheme.createPrimaryButton("Save Budget");
        saveButton.addActionListener(e -> onSave());

        buttonBar.add(cancelButton);
        buttonBar.add(saveButton);
        contentPane.add(buttonBar, BorderLayout.SOUTH);
    }

    private void populateFields() {
        try {
            List<Category> expenseCats = categoryDAO.getByType(TransactionType.EXPENSE);
            List<Category> savingsCats = categoryDAO.getByType(TransactionType.SAVINGS);

            for (Category c : expenseCats) categoryCombo.addItem(c);
            for (Category c : savingsCats) categoryCombo.addItem(c);

            if (editingBudget != null) {
                for (int i = 0; i < categoryCombo.getItemCount(); i++) {
                    if (categoryCombo.getItemAt(i).getId() == editingBudget.getCategoryId()) {
                        categoryCombo.setSelectedIndex(i);
                        break;
                    }
                }
                categoryCombo.setEnabled(false); // don't change category on edit
                limitField.setText(String.format(java.util.Locale.US, "%.2f", editingBudget.getMonthlyLimit()));
            }
        } catch (SQLException e) {
            errorLabel.setText("Failed to load categories: " + e.getMessage());
        }
    }

    private void onSave() {
        Category selectedCategory = (Category) categoryCombo.getSelectedItem();
        if (selectedCategory == null) {
            errorLabel.setText("Please select a category.");
            return;
        }

        String limitStr = limitField.getText().trim();
        if (limitStr.isEmpty()) {
            errorLabel.setText("Please enter a monthly budget limit.");
            limitField.requestFocus();
            return;
        }

        double limit;
        try {
            limit = Double.parseDouble(limitStr);
            if (limit <= 0) {
                errorLabel.setText("Budget limit must be greater than zero.");
                limitField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid numeric limit amount.");
            limitField.requestFocus();
            return;
        }

        try {
            Budget budget = new Budget(currentUser.getId(), selectedCategory.getId(), limit);
            budgetDAO.setBudget(budget);
            saved = true;
            dispose();
        } catch (SQLException e) {
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
