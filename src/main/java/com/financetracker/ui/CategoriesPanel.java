package com.financetracker.ui;

import com.financetracker.dao.CategoryDAO;
import com.financetracker.model.Category;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import com.financetracker.ui.components.CategoryDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Category management panel allowing users to create, rename, and manage custom categories.
 */
public class CategoriesPanel extends JPanel {
    private final User currentUser;
    private final MainFrame mainFrame;
    private final CategoryDAO categoryDAO;

    private List<Category> currentCategories = new ArrayList<>();
    private JTable categoryTable;
    private DefaultTableModel tableModel;

    private JComboBox<String> typeFilterCombo;
    private JButton editBtn;
    private JButton deleteBtn;
    private JLabel countLabel;

    public CategoriesPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame = mainFrame;
        this.categoryDAO = new CategoryDAO();

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Header & Actions
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UITheme.BACKGROUND);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setBackground(UITheme.BACKGROUND);

        JLabel title = new JLabel("Category Management");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Manage custom income, expense, and savings categories");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(subtitle);
        topPanel.add(headerText, BorderLayout.WEST);

        // Actions
        JPanel actionGrp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionGrp.setBackground(UITheme.BACKGROUND);

        actionGrp.add(new JLabel("Filter Type:"));
        typeFilterCombo = new JComboBox<>(new String[]{"All Types", "INCOME", "EXPENSE", "SAVINGS"});
        typeFilterCombo.setFont(UITheme.FONT_BODY);
        typeFilterCombo.addActionListener(e -> refreshData());
        actionGrp.add(typeFilterCombo);

        editBtn = UITheme.createSecondaryButton("Rename / Edit");
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> editSelectedCategory());

        deleteBtn = UITheme.createDangerButton("Delete");
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> deleteSelectedCategory());

        JButton addBtn = UITheme.createPrimaryButton("+ Add Category");
        addBtn.addActionListener(e -> openAddCategoryDialog());

        actionGrp.add(editBtn);
        actionGrp.add(deleteBtn);
        actionGrp.add(addBtn);
        topPanel.add(actionGrp, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Card with Table
        JPanel cardPanel = UITheme.createCardPanel();
        cardPanel.setLayout(new BorderLayout(0, 10));

        String[] cols = {"ID", "Category Name", "Classification Type"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        categoryTable = new JTable(tableModel);
        categoryTable.setFont(UITheme.FONT_BODY);
        categoryTable.setRowHeight(34);
        categoryTable.setShowGrid(false);
        categoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryTable.getTableHeader().setFont(UITheme.FONT_BODY_BOLD);
        categoryTable.getTableHeader().setBackground(new Color(241, 245, 249));

        categoryTable.getColumnModel().getColumn(0).setMaxWidth(60);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        categoryTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        categoryTable.getColumnModel().getColumn(2).setCellRenderer(new TransactionTypeCellRenderer());

        categoryTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSel = categoryTable.getSelectedRow() >= 0;
            editBtn.setEnabled(hasSel);
            deleteBtn.setEnabled(hasSel);
        });

        JScrollPane scrollPane = new JScrollPane(categoryTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER, 1));
        cardPanel.add(scrollPane, BorderLayout.CENTER);

        countLabel = new JLabel("Total Categories: 0");
        countLabel.setFont(UITheme.FONT_SMALL_BOLD);
        countLabel.setForeground(UITheme.TEXT_MUTED);
        cardPanel.add(countLabel, BorderLayout.SOUTH);

        add(cardPanel, BorderLayout.CENTER);
    }

    public void refreshData() {
        try {
            String filter = (String) typeFilterCombo.getSelectedItem();
            if (filter == null || "All Types".equals(filter)) {
                currentCategories = categoryDAO.getAll();
            } else {
                currentCategories = categoryDAO.getByType(TransactionType.valueOf(filter));
            }

            tableModel.setRowCount(0);
            for (Category c : currentCategories) {
                tableModel.addRow(new Object[]{
                        c.getId(),
                        c.getName(),
                        c.getType()
                });
            }

            countLabel.setText("Total Categories: " + currentCategories.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load categories: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openAddCategoryDialog() {
        CategoryDialog dialog = new CategoryDialog(mainFrame, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            mainFrame.notifyDataChanged();
        }
    }

    private void editSelectedCategory() {
        int row = categoryTable.getSelectedRow();
        if (row < 0 || row >= currentCategories.size()) return;

        Category cat = currentCategories.get(row);
        CategoryDialog dialog = new CategoryDialog(mainFrame, cat);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            mainFrame.notifyDataChanged();
        }
    }

    private void deleteSelectedCategory() {
        int row = categoryTable.getSelectedRow();
        if (row < 0 || row >= currentCategories.size()) return;

        Category cat = currentCategories.get(row);
        int opt = JOptionPane.showConfirmDialog(this,
                String.format("Are you sure you want to delete category '%s'?", cat.getName()),
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            try {
                categoryDAO.delete(cat.getId());
                mainFrame.notifyDataChanged();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot Delete Category", JOptionPane.ERROR_MESSAGE);
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
