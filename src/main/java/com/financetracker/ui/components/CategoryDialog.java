package com.financetracker.ui.components;

import com.financetracker.dao.CategoryDAO;
import com.financetracker.model.Category;
import com.financetracker.model.TransactionType;
import com.financetracker.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

/**
 * Modal dialog for creating and modifying custom categories.
 */
public class CategoryDialog extends JDialog {
    private final Category editingCategory;
    private final CategoryDAO categoryDAO;
    private boolean saved = false;

    private JTextField nameField;
    private JComboBox<TransactionType> typeCombo;
    private JLabel errorLabel;

    public CategoryDialog(Window parent, Category categoryToEdit) {
        super(parent, categoryToEdit == null ? "New Category" : "Edit Category", ModalityType.APPLICATION_MODAL);
        this.editingCategory = categoryToEdit;
        this.categoryDAO = new CategoryDAO();

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

        JLabel titleLabel = new JLabel(editingCategory == null ? "Create New Category" : "Edit Category");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);
        contentPane.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 4, 8, 4);
        gbc.weightx = 1.0;

        // Name
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Category Name:"), gbc);
        nameField = new JTextField(15);
        nameField.setFont(UITheme.FONT_BODY);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);

        // Type
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Category Type:"), gbc);
        typeCombo = new JComboBox<>(TransactionType.values());
        typeCombo.setFont(UITheme.FONT_BODY);
        gbc.gridx = 1;
        formPanel.add(typeCombo, gbc);

        // Error Label
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

        JButton saveButton = UITheme.createPrimaryButton("Save Category");
        saveButton.addActionListener(e -> onSave());

        buttonBar.add(cancelButton);
        buttonBar.add(saveButton);
        contentPane.add(buttonBar, BorderLayout.SOUTH);
    }

    private void populateFields() {
        if (editingCategory != null) {
            nameField.setText(editingCategory.getName());
            typeCombo.setSelectedItem(editingCategory.getType());
        }
    }

    private void onSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Please enter a category name.");
            nameField.requestFocus();
            return;
        }

        TransactionType type = (TransactionType) typeCombo.getSelectedItem();
        if (type == null) type = TransactionType.EXPENSE;

        try {
            if (editingCategory == null) {
                if (categoryDAO.getByName(name).isPresent()) {
                    errorLabel.setText("A category with this name already exists.");
                    return;
                }
                Category newCat = new Category(name, type);
                categoryDAO.create(newCat);
            } else {
                editingCategory.setName(name);
                editingCategory.setType(type);
                categoryDAO.update(editingCategory);
            }
            saved = true;
            dispose();
        } catch (SQLException e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
