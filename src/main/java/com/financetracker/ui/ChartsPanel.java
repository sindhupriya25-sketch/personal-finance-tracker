package com.financetracker.ui;

import com.financetracker.dao.BudgetDAO;
import com.financetracker.dao.TransactionDAO;
import com.financetracker.model.Budget;
import com.financetracker.model.CategorySpend;
import com.financetracker.model.MonthlySummary;
import com.financetracker.model.TransactionType;
import com.financetracker.model.User;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Interactive Data Visualization panel powered by JFreeChart.
 * Renders Category Pie Charts, Monthly Spending Trends, Income vs Expense Bar Charts,
 * and Budget vs Actual comparisons with automatic refresh.
 */
public class ChartsPanel extends JPanel {
    private final User currentUser;
    private final MainFrame mainFrame;
    private final TransactionDAO transactionDAO;
    private final BudgetDAO budgetDAO;

    private JComboBox<String> timeframeCombo;
    private JTabbedPane chartTabs;

    private ChartPanel pieChartPanel;
    private ChartPanel trendChartPanel;
    private ChartPanel barChartPanel;
    private ChartPanel budgetChartPanel;

    private YearMonth selectedMonth;

    public ChartsPanel(User currentUser, MainFrame mainFrame) {
        this.currentUser = currentUser;
        this.mainFrame = mainFrame;
        this.transactionDAO = new TransactionDAO();
        this.budgetDAO = new BudgetDAO();
        this.selectedMonth = YearMonth.now();

        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        initComponents();
        refreshCharts();
    }

    private void initComponents() {
        // Top Toolbar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UITheme.BACKGROUND);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setBackground(UITheme.BACKGROUND);

        JLabel title = new JLabel("Data Visualization & Analytics");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Visual insights into your spending habits, trends, and budget compliance");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        headerText.add(title);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(subtitle);
        topPanel.add(headerText, BorderLayout.WEST);

        // Actions
        JPanel actionGrp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionGrp.setBackground(UITheme.BACKGROUND);

        actionGrp.add(new JLabel("Timeframe:"));
        timeframeCombo = new JComboBox<>(new String[]{
                "Selected Month",
                "Last 3 Months",
                "Last 6 Months",
                "Last 12 Months",
                "All Time"
        });
        timeframeCombo.setFont(UITheme.FONT_BODY);
        timeframeCombo.addActionListener(e -> refreshCharts());
        actionGrp.add(timeframeCombo);

        JButton exportImgBtn = UITheme.createSecondaryButton("Save Chart as Image");
        exportImgBtn.addActionListener(e -> exportActiveChart());
        actionGrp.add(exportImgBtn);

        JButton refreshBtn = UITheme.createPrimaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshCharts());
        actionGrp.add(refreshBtn);

        topPanel.add(actionGrp, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Tabbed Pane for Charts
        chartTabs = new JTabbedPane();
        chartTabs.setFont(UITheme.FONT_BODY_BOLD);
        chartTabs.setBackground(UITheme.CARD_BG);

        // Initialize Chart Panels with placeholders
        pieChartPanel = createStyledChartPanel(createCategoryPieChart(new DefaultPieDataset()));
        trendChartPanel = createStyledChartPanel(createSpendingTrendChart(new DefaultCategoryDataset()));
        barChartPanel = createStyledChartPanel(createIncomeExpenseBarChart(new DefaultCategoryDataset()));
        budgetChartPanel = createStyledChartPanel(createBudgetVsActualChart(new DefaultCategoryDataset()));

        chartTabs.addTab("Spending by Category (Pie Chart)", pieChartPanel);
        chartTabs.addTab("Monthly Spending Trend (Line Chart)", trendChartPanel);
        chartTabs.addTab("Income vs Expense vs Savings (Bar Chart)", barChartPanel);
        chartTabs.addTab("Budget vs Actual Spending (Comparison)", budgetChartPanel);

        add(chartTabs, BorderLayout.CENTER);
    }

    public void setMonth(YearMonth month) {
        this.selectedMonth = month;
        refreshCharts();
    }

    public void refreshCharts() {
        if (currentUser == null) return;

        try {
            LocalDate fromDate = null;
            LocalDate toDate = null;
            String timeframe = (String) timeframeCombo.getSelectedItem();
            int monthsCount = 6;

            if ("Selected Month".equals(timeframe)) {
                fromDate = selectedMonth.atDay(1);
                toDate = selectedMonth.atEndOfMonth();
                monthsCount = 1;
            } else if ("Last 3 Months".equals(timeframe)) {
                fromDate = selectedMonth.minusMonths(2).atDay(1);
                toDate = selectedMonth.atEndOfMonth();
                monthsCount = 3;
            } else if ("Last 6 Months".equals(timeframe)) {
                fromDate = selectedMonth.minusMonths(5).atDay(1);
                toDate = selectedMonth.atEndOfMonth();
                monthsCount = 6;
            } else if ("Last 12 Months".equals(timeframe)) {
                fromDate = selectedMonth.minusMonths(11).atDay(1);
                toDate = selectedMonth.atEndOfMonth();
                monthsCount = 12;
            }

            // 1. Category Pie Chart
            List<CategorySpend> spends = transactionDAO.getCategorySpends(currentUser.getId(), fromDate, toDate, TransactionType.EXPENSE);
            DefaultPieDataset pieDataset = new DefaultPieDataset();
            for (CategorySpend cs : spends) {
                pieDataset.setValue(cs.getCategoryName(), cs.getTotalAmount());
            }
            JFreeChart pieChart = createCategoryPieChart(pieDataset);
            pieChartPanel.setChart(pieChart);

            // 2. Spending Trend Line Chart & 3. Income vs Expense Bar Chart
            List<MonthlySummary> summaries = transactionDAO.getMonthlySummaries(currentUser.getId(), monthsCount);
            DefaultCategoryDataset trendDataset = new DefaultCategoryDataset();
            DefaultCategoryDataset barDataset = new DefaultCategoryDataset();

            for (MonthlySummary s : summaries) {
                trendDataset.addValue(s.getTotalExpense(), "Expense Trend", s.getMonthKey());
                trendDataset.addValue(s.getTotalSavings(), "Savings Trend", s.getMonthKey());

                barDataset.addValue(s.getTotalIncome(), "Total Income", s.getMonthKey());
                barDataset.addValue(s.getTotalExpense(), "Total Expenses", s.getMonthKey());
                barDataset.addValue(s.getTotalSavings(), "Total Savings", s.getMonthKey());
            }

            JFreeChart trendChart = createSpendingTrendChart(trendDataset);
            trendChartPanel.setChart(trendChart);

            JFreeChart barChart = createIncomeExpenseBarChart(barDataset);
            barChartPanel.setChart(barChart);

            // 4. Budget vs Actual Comparison Bar Chart
            int year = selectedMonth.getYear();
            int month = selectedMonth.getMonthValue();
            List<Budget> budgets = budgetDAO.getBudgetUsages(currentUser.getId(), year, month);
            DefaultCategoryDataset budgetDataset = new DefaultCategoryDataset();

            for (Budget b : budgets) {
                budgetDataset.addValue(b.getMonthlyLimit(), "Budget Limit", b.getCategoryName());
                budgetDataset.addValue(b.getCurrentSpent(), "Actual Spent", b.getCategoryName());
            }

            JFreeChart budgetChart = createBudgetVsActualChart(budgetDataset);
            budgetChartPanel.setChart(budgetChart);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load chart data: " + e.getMessage(), "Chart Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JFreeChart createCategoryPieChart(DefaultPieDataset dataset) {
        JFreeChart chart = ChartFactory.createPieChart(
                "Expense Distribution by Category",
                dataset,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(UITheme.CARD_BG);
        chart.getTitle().setFont(UITheme.FONT_HEADER);
        chart.getTitle().setPaint(UITheme.TEXT_MAIN);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(UITheme.CARD_BG);
        plot.setOutlineVisible(false);
        plot.setLabelFont(UITheme.FONT_SMALL);
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})", new DecimalFormat("₹#,##0.00"), new DecimalFormat("0.0%")
        ));
        plot.setSimpleLabels(true);
        plot.setShadowPaint(null);

        // Modern color palette for slices
        Color[] sliceColors = {
                new Color(59, 130, 246), new Color(239, 68, 68), new Color(16, 185, 129),
                new Color(245, 158, 11), new Color(139, 92, 246), new Color(236, 72, 153),
                new Color(20, 184, 166), new Color(249, 115, 22), new Color(99, 102, 241)
        };
        int idx = 0;
        for (Object key : dataset.getKeys()) {
            plot.setSectionPaint((Comparable<?>) key, sliceColors[idx % sliceColors.length]);
            idx++;
        }

        return chart;
    }

    private JFreeChart createSpendingTrendChart(DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createLineChart(
                "Monthly Expenditure & Savings Trend",
                "Month",
                "Amount (₹)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(UITheme.CARD_BG);
        chart.getTitle().setFont(UITheme.FONT_HEADER);
        chart.getTitle().setPaint(UITheme.TEXT_MAIN);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(248, 250, 252));
        plot.setDomainGridlinePaint(UITheme.BORDER);
        plot.setRangeGridlinePaint(UITheme.BORDER);
        plot.setOutlineVisible(false);

        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesPaint(0, UITheme.DANGER);        // Expenses line
        renderer.setSeriesStroke(0, new BasicStroke(3.0f));
        renderer.setSeriesShapesVisible(0, true);

        renderer.setSeriesPaint(1, UITheme.PRIMARY);       // Savings line
        renderer.setSeriesStroke(1, new BasicStroke(3.0f));
        renderer.setSeriesShapesVisible(1, true);

        plot.setRenderer(renderer);
        return chart;
    }

    private JFreeChart createIncomeExpenseBarChart(DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
                "Monthly Cash Flow: Income vs Expenses vs Savings",
                "Month",
                "Amount (₹)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(UITheme.CARD_BG);
        chart.getTitle().setFont(UITheme.FONT_HEADER);
        chart.getTitle().setPaint(UITheme.TEXT_MAIN);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(248, 250, 252));
        plot.setDomainGridlinePaint(UITheme.BORDER);
        plot.setRangeGridlinePaint(UITheme.BORDER);
        plot.setOutlineVisible(false);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setSeriesPaint(0, UITheme.SUCCESS); // Income
        renderer.setSeriesPaint(1, UITheme.DANGER);  // Expenses
        renderer.setSeriesPaint(2, UITheme.PRIMARY); // Savings
        renderer.setItemMargin(0.04);

        return chart;
    }

    private JFreeChart createBudgetVsActualChart(DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
                "Category Budget vs Actual Expenditure (" + selectedMonth.getMonth().name() + " " + selectedMonth.getYear() + ")",
                "Category",
                "Amount (₹)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(UITheme.CARD_BG);
        chart.getTitle().setFont(UITheme.FONT_HEADER);
        chart.getTitle().setPaint(UITheme.TEXT_MAIN);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(248, 250, 252));
        plot.setDomainGridlinePaint(UITheme.BORDER);
        plot.setRangeGridlinePaint(UITheme.BORDER);
        plot.setOutlineVisible(false);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(org.jfree.chart.axis.CategoryLabelPositions.UP_45);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setSeriesPaint(0, new Color(148, 163, 184)); // Budget Limit (Slate)
        renderer.setSeriesPaint(1, UITheme.PRIMARY);           // Actual Spent
        renderer.setItemMargin(0.02);

        return chart;
    }

    private ChartPanel createStyledChartPanel(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));
        panel.setMouseWheelEnabled(true);
        return panel;
    }

    private void exportActiveChart() {
        int selectedIndex = chartTabs.getSelectedIndex();
        ChartPanel activePanel = switch (selectedIndex) {
            case 0 -> pieChartPanel;
            case 1 -> trendChartPanel;
            case 2 -> barChartPanel;
            case 3 -> budgetChartPanel;
            default -> pieChartPanel;
        };

        JFileChooser chooser = new JFileChooser();
        String defaultName = "chart_" + chartTabs.getTitleAt(selectedIndex).replaceAll("[^a-zA-Z0-9]", "_") + ".png";
        chooser.setSelectedFile(new File(defaultName));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            try {
                ChartUtils.saveChartAsPNG(file, activePanel.getChart(), 900, 600);
                JOptionPane.showMessageDialog(this,
                        "Chart saved as image:\n" + file.getAbsolutePath(),
                        "Chart Saved",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to save chart: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
