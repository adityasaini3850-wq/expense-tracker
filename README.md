# 📱 Expense Tracker & Financial Intelligence System

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.00-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%20Design%203-Latest-006A60?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Room Database](https://img.shields.io/badge/Room%20DB-2.7.0-4CAF50?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20+%20Clean-FF6F00?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

A production-ready, local-first Android personal finance application and analytics intelligence platform built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Android Room (SQLite)**. 

Designed for both **everyday personal/group expense tracking** and as a **comprehensive Data Analyst portfolio work sample**, featuring automated budget variance alerting, a greedy debt-simplification graph algorithm, RFC-4180 CSV export pipelines, and a native multi-page executive PDF report generator.

---

## 🌟 Key Features

### 1. 💳 Personal Expense Tracking
* **Instant Logging:** Log expenditures with amount, category, merchant description, timestamp, and optional group folder tagging.
* **Category Breakdown:** Built-in categories (Food, Travel, Utilities, Shopping, Entertainment, Health, Subscriptions, Education, Other).
* **Live Aggregates:** Real-time calculation of total expenditures, monthly totals, category percentages, and highest-spend categories.

### 2. 🎯 Smart Category Budgeting & Pacing
* **Monthly Budget Allocation:** Define custom spending thresholds per category for each month.
* **Real-time Variance Tracking:** Immediate calculation of spent vs. remaining limits with color-coded progress bars.
* **Proactive Burn-Rate Alerts:**
  * 🟢 **Normal (< 80%):** Spending within safe projections.
  * 🟡 **Warning (80% - 100%):** Proactive alert before exceeding allocated limit.
  * 🔴 **Critical (> 100%):** Overspent alert displaying overrun amount in real time.

### 3. 👥 Group Split & Shared Bill Management (Splitwise Style)
* **Trip & Household Folders:** Organize shared expenditures into isolated group folders (e.g., "Road Trip", "Apartment Rent & Groceries").
* **Multi-Payer Tracking:** Track which group member paid for each item and specify member counts.
* **Minimum Cash Flow Debt Simplification Algorithm:** Solves the circular debt problem by converting an $O(N^2)$ matrix of pairwise debts into an optimal $O(N)$ direct settlement sequence, eliminating redundant intermediate transfers.

### 4. 📊 Data Analyst & Business Intelligence Suite
* **Interactive In-App Analytics Hub:** Preview analytical SQL queries, Pareto 80/20 analysis, rolling moving averages, and month-over-month variances directly in the UI.
* **RFC-4180 Standard CSV Export:** Clean CSV data stream with proper quotation, comma-escaping, and sanitization for immediate ingestion into BigQuery, Snowflake, Tableau, Excel, and Pandas.
* **In-App & Standalone PDF Work Sample Exporters:**
  * **Native Android `PdfDocument`:** Generates high-fidelity vector PDF reports directly on device and shares via Android `FileProvider`.
  * **Automated Python Generator (`build_work_sample_pdf.py`):** Standalone zero-dependency script to generate executive 6-page work sample PDF deliverables for interviewers.

### 5. 🎨 Modern Material 3 UI/UX
* **Edge-to-Edge:** Native Android edge-to-edge layout with full support for system window insets.
* **M3 Dynamic Color System:** Vibrant light and dark palettes following Material Design 3 guidelines.
* **Accessible & Touch-Friendly:** Strictly follows Android accessibility touch target standards (minimum 48dp).

---

## 🏗️ Architecture & Technology Stack

```
┌────────────────────────────────────────────────────────┐
│                   UI LAYER (COMPOSE)                   │
│   MainActivity ────► ExpenseViewModel ────► M3 Theme   │
│   ├── Personal Dashboard & Category Cards              │
│   ├── Smart Budget Variance Pacing UI                  │
│   ├── Group Folders & Debt Settlement Dialog           │
│   └── Data Analyst Export & Portfolio Dialog           │
└───────────────────────────┬────────────────────────────┘
                            │ StateFlow / Coroutines
┌───────────────────────────▼────────────────────────────┐
│                  DOMAIN / UTILITY LAYER                │
│   ├── Minimum Cash Flow Solver (Graph Simplification)  │
│   ├── Budget Variance & Pacing Calculator              │
│   ├── RFC-4180 CSV Exporter Engine                     │
│   └── Native Canvas PdfWorkSampleExporter              │
└───────────────────────────┬────────────────────────────┘
                            │ Repository Pattern
┌───────────────────────────▼────────────────────────────┐
│                    DATA LAYER (ROOM)                   │
│   ExpenseRepository ──► AppDatabase (Room 2.7.0)       │
│   └── ExpenseDao (SQLite Reactive Queries with Flow)   │
│       ├── ExpenseEntity (Transactions)                 │
│       ├── CategoryBudgetEntity (Monthly Targets)       │
│       └── FolderEntity (Group Folders)                 │
└────────────────────────────────────────────────────────┘
```

### Libraries & Tools
* **Language:** Kotlin 2.2.10
* **Build System:** Gradle 9.1.1 (Kotlin DSL)
* **UI Toolkit:** Jetpack Compose with Material 3 BOM `2024.09.00`
* **Local Persistence:** Android Room 2.7.0 (with KSP 2.3.5)
* **Reactive Programming:** Kotlin Coroutines & `StateFlow`
* **Lifecycle:** AndroidX Lifecycle Runtime Compose 2.8.7
* **Vector Graphics & PDF:** Android Native `android.graphics.pdf.PdfDocument` & `android.graphics.Canvas`
* **Security & Storage:** Android `FileProvider` with zero-permission media/file sharing (Google Play Policy compliant)

---

## 📁 Project Structure

```
├── app/
│   ├── build.gradle.kts                # Module-level Gradle configuration
│   ├── proguard-rules.pro              # Proguard optimization rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml     # Application manifest & FileProvider
│           ├── java/com/example/
│           │   ├── MainActivity.kt     # Main entry point, navigation & UI screens
│           │   ├── data/
│           │   │   ├── Models.kt       # Entities (Expense, Budget, Folder)
│           │   │   ├── ExpenseDao.kt   # SQLite queries and Room DAO
│           │   │   ├── AppDatabase.kt  # Room database definition & migrations
│           │   │   └── ExpenseRepository.kt # Clean abstraction over Room DAO
│           │   ├── ui/
│           │   │   ├── ExpenseViewModel.kt  # StateFlow, business logic & analytics
│           │   │   ├── DataAnalystExportDialog.kt # BI analytics & interview portfolio UI
│           │   │   └── theme/
│           │   │       ├── Color.kt    # Material 3 color definitions
│           │   │       ├── Theme.kt    # Dark / Light theme composable
│           │   │       └── Type.kt     # Typography styles
│           │   └── util/
│           │       ├── CsvExporter.kt  # RFC-4180 CSV generation engine
│           │       └── PdfWorkSampleExporter.kt # Android native PDF report generator
│           └── res/
│               ├── drawable/           # Launcher vectors & drawables
│               ├── mipmap-*/           # Adaptive launcher icons
│               ├── values/             # strings.xml, themes.xml
│               └── xml/file_paths.xml  # FileProvider cache path mappings
├── build.gradle.kts                    # Root Gradle configuration
├── settings.gradle.kts                 # Project settings & repositories
├── gradle/
│   └── libs.versions.toml              # Version catalog (dependencies & plugins)
├── build_work_sample_pdf.py            # Standalone 6-page PDF Work Sample generator
├── DATA_ANALYST_WORK_SAMPLE.md         # Full portfolio documentation & SQL queries
├── .gitignore                          # Clean Android Git ignore template
└── README.md                           # Project documentation (this file)
```

---

## 🚀 Getting Started & Building

### Prerequisites
* **Android Studio:** Ladybug (2024.2.1) or Meerkat+
* **JDK:** OpenJDK 17 or 21 (configured in Android Studio under Settings > Build > Gradle > Gradle JDK)
* **Android SDK:** Compile SDK 36, Target SDK 36, Minimum SDK 24 (Android 7.0+)

### Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/<your-username>/expense-tracker-android.git
   cd expense-tracker-android
   ```

2. **Open in Android Studio:**
   * Launch Android Studio.
   * Select **File > Open** and choose the repository root folder.
   * Allow Gradle to sync dependencies automatically.

3. **Build the Debug APK via Terminal:**
   ```bash
   gradle assembleDebug
   ```
   The output APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on a Connected Device or Emulator:**
   ```bash
   gradle installDebug
   ```

---

## 📄 Generating the Data Analyst Work Sample PDF

The project includes an executive-ready Data Analyst Portfolio Work Sample that can be produced in two ways:

### Option A: Directly Inside the Android App
1. Open the app on your phone or emulator.
2. Tap the **"Analytics & Export"** icon in the top app bar.
3. Tap **"Export PDF Work Sample"** or **"View Portfolio"**.
4. The app uses Android's native `PdfDocument` engine to render the executive report and launches an Android Share Sheet to open or email the PDF.

### Option B: Standalone Script (Terminal)
Run the bundled pure-Python generator:
```bash
python3 build_work_sample_pdf.py
```
This generates:
* `Data_Analyst_Work_Sample_Expense_Tracker.pdf` (Comprehensive 6-page portfolio report)
* `Work_Sample_Portfolio_Guide.pdf`

---

## 💼 How to Use This Project for Job Interviews

This project is tailored to demonstrate high competency across multiple roles:

### For Data Analyst / BI Engineer Roles:
* **Dimensional Modeling:** Point the interviewer to `DATA_ANALYST_WORK_SAMPLE.md` Section 2, showing the Star Schema design (`fact_expenses`, `dim_categories`, `dim_budgets`, `dim_dates`, `dim_folders`).
* **Advanced SQL:** Review the CTEs, Pareto 80/20 window functions, and month-over-month variance queries in `DataAnalystExportDialog.kt` and `DATA_ANALYST_WORK_SAMPLE.md`.
* **Data Ingestion & Integrity:** Walk through `CsvExporter.kt` to show how RFC-4180 standard escaping prevents data corruption during automated ETL pipelines.

### For Android / Mobile Engineer Roles:
* **Jetpack Compose Best Practices:** Highlight the declarative UI, state hoisting, and test tags (`Modifier.testTag(...)`) across `MainActivity.kt`.
* **Offline-First Room Persistence:** Explain the reactive DAO layer emitting `Flow<List<ExpenseEntity>>` and automatic database migrations in `AppDatabase.kt`.
* **Algorithms & Performance:** Explain the greedy Minimum Cash Flow debt simplification algorithm in `ExpenseViewModel.kt` that optimizes circular group debt settlement.

---

## 🐙 Step-by-Step Guide to Post on GitHub

Follow these steps to publish this complete working code to your own GitHub profile:

1. **Initialize Git repository:**
   ```bash
   git init
   git branch -M main
   ```

2. **Stage and commit all source files:**
   ```bash
   git add .
   git commit -m "feat: initial commit of Expense Tracker & Financial Intelligence System"
   ```

3. **Create a new repository on GitHub:**
   * Go to [github.com/new](https://github.com/new).
   * Enter repository name: `expense-tracker-android` (or your choice).
   * Leave it Public and do **NOT** check "Initialize with README" (since you already have this one).

4. **Add remote and push:**
   ```bash
   git remote add origin https://github.com/<your-username>/expense-tracker-android.git
   git push -u origin main
   ```

5. **(Optional) Attach the APK as a GitHub Release:**
   * Run `gradle assembleRelease` or `gradle assembleDebug`.
   * On your GitHub repo page, click **Releases > Draft a new release**.
   * Tag version `v1.0.0`, title `Initial Release v1.0.0`, and drag-and-drop the APK file.

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

*Crafted with Kotlin, Jetpack Compose, and Material 3.*
