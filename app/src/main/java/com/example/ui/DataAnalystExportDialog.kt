package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Expense
import com.example.data.Folder
import com.example.util.CsvExporter
import com.example.util.PdfWorkSampleExporter
import java.util.Locale

enum class ExportType(val title: String, val icon: ImageVector, val filenamePrefix: String) {
    TRANSACTIONS("Transactions", Icons.Default.Receipt, "transactions_history"),
    BUDGET_STATUS("Budget Status", Icons.Default.PieChart, "budget_status_variance"),
    ANALYST_MART("Analytical Mart", Icons.Default.TableChart, "expense_analyst_mart")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataAnalystExportDialog(
    expenses: List<Expense>,
    budgetProgressList: List<CategoryBudgetProgress>,
    folders: List<Folder>,
    selectedMonthYear: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedExportType by remember { mutableStateOf(ExportType.TRANSACTIONS) }
    var showRawCsv by remember { mutableStateOf(false) }
    var showInterviewPortfolio by remember { mutableStateOf(false) }

    // Generate CSV contents reactively
    val currentCsv = remember(selectedExportType, expenses, budgetProgressList, folders, selectedMonthYear) {
        when (selectedExportType) {
            ExportType.TRANSACTIONS -> CsvExporter.generateTransactionsCsv(expenses, folders)
            ExportType.BUDGET_STATUS -> CsvExporter.generateBudgetStatusCsv(budgetProgressList, selectedMonthYear)
            ExportType.ANALYST_MART -> CsvExporter.generateDataAnalystMartCsv(expenses, budgetProgressList, folders, selectedMonthYear)
        }
    }

    val lines = remember(currentCsv) { currentCsv.trim().lines() }
    val rowCount = remember(lines) { (lines.size - 1).coerceAtLeast(0) }
    val totalVolume = remember(expenses) { expenses.sumOf { it.amount } }
    val avgTransaction = remember(expenses) { if (expenses.isNotEmpty()) totalVolume / expenses.size else 0.0 }
    val maxTransaction = remember(expenses) { expenses.maxOfOrNull { it.amount } ?: 0.0 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("data_analyst_export_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Data Export & Analyst Hub",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "RFC 4180 CSV export for SQL, Python, Excel & Tableau",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_export_dialog_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Work Sample / Interview Portfolio button banner
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Data Analyst Interview Portfolio",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Schema design, SQL queries & Python EDA sample ready to present",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    PdfWorkSampleExporter.generateAndSharePdf(
                                        context = context,
                                        expenses = expenses,
                                        budgetProgressList = budgetProgressList,
                                        folders = folders
                                    )
                                },
                                modifier = Modifier.testTag("export_work_sample_pdf_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Report", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { showInterviewPortfolio = true },
                                modifier = Modifier.testTag("open_interview_sample_btn")
                            ) {
                                Text("View Portfolio", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Export Type Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportType.values().forEach { type ->
                        FilterChip(
                            selected = selectedExportType == type,
                            onClick = { selectedExportType = type },
                            label = { Text(type.title, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("export_tab_${type.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dataset Metrics Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricMiniCard(
                        title = "Rows",
                        value = "$rowCount",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "Total Volume",
                        value = "$${String.format(Locale.US, "%.1f", totalVolume)}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "Avg Txn",
                        value = "$${String.format(Locale.US, "%.1f", avgTransaction)}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "Max Txn",
                        value = "$${String.format(Locale.US, "%.1f", maxTransaction)}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Table / Raw Preview Toggle & Description
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedExportType) {
                            ExportType.TRANSACTIONS -> "Schema: transaction_id, date, amount, category, type..."
                            ExportType.BUDGET_STATUS -> "Schema: category, limit, spent, variance, health..."
                            ExportType.ANALYST_MART -> "Star Schema: Transactions enriched with Budget Limits"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { showRawCsv = !showRawCsv },
                        modifier = Modifier.testTag("toggle_raw_csv_btn")
                    ) {
                        Text(if (showRawCsv) "Table View" else "Raw CSV View")
                    }
                }

                // Table / Raw Content Container
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (showRawCsv) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = currentCsv,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.testTag("raw_csv_text")
                            )
                        }
                    } else {
                        // Formatted preview table
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                                items(lines) { line ->
                                    val isHeader = line == lines.firstOrNull()
                                    val columns = line.split(",")

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isHeader) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(vertical = 4.dp, horizontal = 6.dp)
                                    ) {
                                        columns.forEach { col ->
                                            Text(
                                                text = col.replace("\"", ""),
                                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Export Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            CsvExporter.copyToClipboard(
                                context = context,
                                label = "${selectedExportType.filenamePrefix}.csv",
                                csvContent = currentCsv
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_csv_clipboard_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy CSV", maxLines = 1, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            CsvExporter.exportAndShareCsv(
                                context = context,
                                filenamePrefix = selectedExportType.filenamePrefix,
                                csvContent = currentCsv
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_csv_file_btn")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", maxLines = 1, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            PdfWorkSampleExporter.generateAndSharePdf(
                                context = context,
                                expenses = expenses,
                                budgetProgressList = budgetProgressList,
                                folders = folders
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("export_pdf_work_sample_bottom_btn")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export PDF", maxLines = 1, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showInterviewPortfolio) {
        DataAnalystPortfolioDialog(
            expenses = expenses,
            budgetProgressList = budgetProgressList,
            folders = folders,
            onDismiss = { showInterviewPortfolio = false }
        )
    }
}

@Composable
fun MetricMiniCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DataAnalystPortfolioDialog(
    expenses: List<Expense> = emptyList(),
    budgetProgressList: List<CategoryBudgetProgress> = emptyList(),
    folders: List<Folder> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val sampleSql = """
-- 1. Monthly Category Spending vs Target Budget Variance Analysis
SELECT 
    b.category,
    b.monthly_limit AS target_budget,
    COALESCE(SUM(e.amount), 0) AS actual_spent,
    ROUND(b.monthly_limit - COALESCE(SUM(e.amount), 0), 2) AS budget_variance,
    ROUND((COALESCE(SUM(e.amount), 0) / NULLIF(b.monthly_limit, 0)) * 100, 1) AS utilization_pct,
    CASE 
        WHEN COALESCE(SUM(e.amount), 0) > b.monthly_limit THEN 'EXCEEDED'
        WHEN COALESCE(SUM(e.amount), 0) >= b.monthly_limit * 0.8 THEN 'WARNING'
        ELSE 'SAFE'
    END AS budget_status
FROM category_budgets b
LEFT JOIN expenses e 
    ON b.category = e.category 
    AND strftime('%Y-%m', datetime(e.timestamp/1000, 'unixepoch')) = b.month_year
GROUP BY b.category, b.monthly_limit, b.month_year
ORDER BY actual_spent DESC;

-- 2. Pareto 80/20 Cumulative Spending Distribution
WITH CategoryTotals AS (
    SELECT 
        category,
        SUM(amount) AS total_amount,
        COUNT(*) AS transaction_count
    FROM expenses
    GROUP BY category
),
RankedSpend AS (
    SELECT 
        category,
        total_amount,
        transaction_count,
        SUM(total_amount) OVER (ORDER BY total_amount DESC) AS cumulative_spend,
        SUM(total_amount) OVER () AS overall_total
    FROM CategoryTotals
)
SELECT 
    category,
    total_amount,
    transaction_count,
    ROUND((cumulative_spend / overall_total) * 100, 2) AS cumulative_spend_pct
FROM RankedSpend;
""".trimIndent()

    val samplePython = """
# Python / Pandas Exploratory Data Analysis (EDA) on Exported CSV
import pandas as pd
import numpy as np

# 1. Load exported transactions
df = pd.read_csv('transactions_history.csv', parse_dates=['date'])

# 2. Key descriptive statistics
print("Total Transaction Volume:", df['amount'].sum())
print("Mean Transaction Size:", df['amount'].mean())
print("Standard Deviation:", df['amount'].std())

# 3. Category Breakdown & 80/20 Distribution
category_summary = df.groupby('category').agg(
    total_spend=('amount', 'sum'),
    transaction_count=('amount', 'count'),
    mean_spend=('amount', 'mean')
).sort_values(by='total_spend', ascending=False)

category_summary['share_of_wallet_pct'] = (category_summary['total_spend'] / df['amount'].sum()) * 100
print(category_summary)

# 4. Outlier detection using IQR
q25, q75 = np.percentile(df['amount'], 25), np.percentile(df['amount'], 75)
iqr = q75 - q25
outliers = df[df['amount'] > (q75 + 1.5 * iqr)]
print(f"Detected {len(outliers)} anomalous/high-value transactions.")
""".trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Data Analyst Work Sample Portfolio", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "This work sample demonstrates end-to-end data pipeline capabilities: data model design in SQLite/Room, RFC-standardized CSV extraction, relational SQL analytics, and Python/Pandas EDA for business intelligence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Project Overview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("1. Problem Statement & Architecture", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("• Domain: Personal & Multi-party Financial Ledger with Monthly Category Budgets", fontSize = 12.sp)
                        Text("• Data Model: Star Schema pattern (fact_expenses, dim_budgets, dim_folders, dim_debts)", fontSize = 12.sp)
                        Text("• ETL Pipeline: Local transactional DB -> Clean RFC 4180 CSV Export -> Analytical Mart", fontSize = 12.sp)
                    }
                }

                // SQL Snippets
                Text("2. Production Analytical SQL Queries", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = sampleSql,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                }
                OutlinedButton(
                    onClick = {
                        CsvExporter.copyToClipboard(context, "Sample_SQL_Queries.sql", sampleSql)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy SQL Queries", fontSize = 11.sp)
                }

                // Python Snippets
                Text("3. Python / Pandas EDA Pipeline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = samplePython,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                }
                OutlinedButton(
                    onClick = {
                        CsvExporter.copyToClipboard(context, "Sample_Python_EDA.py", samplePython)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Python Script", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        PdfWorkSampleExporter.generateAndSharePdf(
                            context = context,
                            expenses = expenses,
                            budgetProgressList = budgetProgressList,
                            folders = folders
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("portfolio_export_pdf_btn")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Portfolio (PDF)")
                }

                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
