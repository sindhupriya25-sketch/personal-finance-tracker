# 📊 Personal Finance Tracker — Presentation Slides & Speaker Notes

**Course**: Object-Oriented Programming (OOPS) / Micro Project Presentation  
**Project Title**: Personal Finance Tracker (Desktop Application)  
**Tech Stack**: Java 17+, Swing (FlatLaf), SQLite JDBC, JFreeChart, OpenPDF, Maven  
**Architecture**: MVC (Model-View-Controller) + DAO (Data Access Object) Pattern  

---

## 🖥️ Slide 1: Title & Introduction
- **Project Title**: Personal Finance Tracker
- **Subtitle**: Smart Budgeting, Cash Flow Analytics & Real-Time Alert System
- **Presenter**: [Your Name]
- **Domain**: Financial Planning & Personal Budgeting
- **Key Highlight**: Full-featured Java desktop app with zero-config SQLite, real-time threshold warnings, interactive charts, and PDF statement generation in Indian Rupees (₹).

> 🗣️ **Speaker Note**:  
> *"Good morning respected faculty and peers. Today, I am presenting my micro-project: the Personal Finance Tracker — a secure desktop application developed in Java to help individuals take complete control of their monthly budgets, cash flow, and investments with real-time intelligence."*

---

## 🎯 Slide 2: Problem Statement & Objectives
- **Problem**: 
  - Difficulty in tracking daily multi-category expenses manually.
  - Lack of timely alerts before exceeding category budgets.
  - Fragmented records with no visual trends or professional report exports.
- **Project Objectives**:
  1. Record and categorize Income, Expenses, and Savings seamlessly.
  2. Implement real-time budget threshold warnings (80% warning, 100% exceeded).
  3. Visualize spending patterns using dynamic JFreeChart visualizations.
  4. Export audited statements into CSV and OpenPDF documents.
  5. Ensure zero-configuration local SQLite database with cryptographic password security.

> 🗣️ **Speaker Note**:  
> *"Most people lose track of where their money goes every month. Our objective was to build a desktop app that does not just record numbers, but actively prevents overspending with real-time threshold alerts and intuitive visual charts."*

---

## 🛠️ Slide 3: Technology Stack & Tools
| Tier | Technologies Used |
| :--- | :--- |
| **Language** | Java 17+ (Object-Oriented Programming) |
| **GUI Framework** | Java Swing + FlatLaf 3.4.1 (Modern Clean Dark/Light Design) |
| **Database** | SQLite 3 with `sqlite-jdbc` Driver (Auto-initialized) |
| **Visualization** | JFreeChart 1.5.4 (Pie, Line, Bar, Budget vs Actual Charts) |
| **Document Generation** | OpenPDF 1.3.39 (Financial Statement PDF Export) |
| **Build & Testing** | Maven (Shade Fat JAR) + JUnit 5 (21 Automated Tests) |

> 🗣️ **Speaker Note**:  
> *"We utilized Java 17+ with the FlatLaf modern UI library. For data storage, we chose SQLite for zero-setup portability. Data visualization is powered by JFreeChart, and PDF statements are generated dynamically using OpenPDF."*

---

## 🏛️ Slide 4: System Architecture (MVC + DAO Pattern)
- **Model Layer**: Entities (`User`, `Category`, `Transaction`, `Budget`, `BudgetAlert`, `MonthlySummary`).
- **DAO Layer**: Encapsulates all SQL queries with `PreparedStatement` (`UserDAO`, `CategoryDAO`, `TransactionDAO`, `BudgetDAO`, `AlertDAO`).
- **Service Layer**: Business logic (`BudgetAlertService`, `UserService`, `PasswordUtil`, `ReportService`, `SampleDataService`).
- **UI / View Layer**: Clean panels (`DashboardPanel`, `TransactionPanel`, `BudgetPanel`, `ChartsPanel`, `ReportsPanel`, `AlertsPanel`).

> 🗣️ **Speaker Note**:  
> *"The project strictly follows the MVC + DAO architecture. The DAO layer cleanly separates database queries from business rules. Every query uses PreparedStatement to eliminate SQL injection risks, and passwords are protected using SHA-256 with 16-byte random salts."*

---

## 📈 Slide 5: Core Features & Real-Time Alerts
1. **Executive Dashboard**:
   - 4 Live KPI Cards: Total Income, Total Expenses, Total Savings, Net Balance in ₹.
   - Dynamic Category Budget Progress Bars (Green <80%, Amber 80%-99%, Red ≥100%).
   - Live Notification Banner for active budget warnings.
2. **Real-Time Budget Limits**:
   - **≥80%**: Yellow Warning popup showing remaining budget balance.
   - **≥100%**: Red Alert popup showing calculated overage.
   - Persistent **Alerts Audit Log** maintaining historical notifications.
3. **1-Click 3-Month Demo Generator**: Populates realistic salaries, rent, groceries, shopping, and mutual fund SIPs.

> 🗣️ **Speaker Note**:  
> *"A standout feature is our real-time alert engine. As soon as you add an expense, the system evaluates your monthly spending for that category. If you hit 80%, you get a warning popup; if you exceed 100%, an alert popup triggers and logs into your audit trail."*

---

## 📊 Slide 6: Visual Analytics (JFreeChart) & Reporting
- **4 Interactive Visualizations**:
  1. *Category Spending Breakdown* (Pie Chart with % and amount labels).
  2. *Monthly Spending & Savings Trends* (Dual Line Chart).
  3. *Monthly Inflows vs. Outflows* (Income vs. Expense vs. Savings Bar Chart).
  4. *Budget vs. Actual Expenditure* (Comparative Bar Chart).
- **Exporting Capabilities**:
  - Export any chart directly to high-resolution PNG image.
  - Export transaction logs to CSV.
  - Export official **PDF Financial Statements** with summary metrics and category breakdowns.

> 🗣️ **Speaker Note**:  
> *"Our analytics module offers four interactive JFreeCharts to analyze financial health across various timeframes. Users can also export their financial statements to PDF with a single click."*

---

## 🧪 Slide 7: Testing & Quality Assurance
- **JUnit 5 Automated Test Suite**:
  - `UserDAOTest`: User registration, credential verification, duplicate prevention.
  - `CategoryDAOTest`: Custom category creation, rename, and referential integrity check.
  - `TransactionDAOTest`: Multi-filter queries, keyword search, monthly summaries.
  - `BudgetDAOTest`: Monthly budget limits, spent calculations, usage ratios.
  - `BudgetAlertServiceTest`: Validates 80% warning and 100% exceeded triggers.
  - `PasswordUtilTest`: Cryptographic salt generation and SHA-256 verification.
  - `ReportServiceTest`: PDF and CSV document generation verification.
- **Results**: **21 / 21 Tests Passed** (0 Failures, 0 Errors).

> 🗣️ **Speaker Note**:  
> *"To ensure code quality and robustness, we authored 21 JUnit tests covering the DAO layer, alert thresholds, and report generation, all executing cleanly with 100% pass rate."*

---

## 🚀 Slide 8: Conclusion & Future Scope
- **Conclusion**:
  - Successfully built a complete, responsive, and secure personal budgeting desktop tool.
  - Meets all micro-project objectives with clean code, modular architecture, and modern UX.
- **Future Enhancements**:
  - Bank statement parser (CSV / OFX / PDF auto-import).
  - Multi-currency conversion.
  - Automated recurring bills & subscription reminders.

> 🗣️ **Speaker Note**:  
> *"In conclusion, this project provides an end-to-end practical solution for personal financial management. In the future, we plan to add automated bank statement imports and bill reminder schedules. Thank you! We are now open to any questions."*
