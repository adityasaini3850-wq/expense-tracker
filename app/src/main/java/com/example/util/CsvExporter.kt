package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Expense
import com.example.data.Folder
import com.example.ui.CategoryBudgetProgress
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    private fun escapeCsv(value: Any?): String {
        val str = value?.toString() ?: ""
        return if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            "\"${str.replace("\"", "\"\"")}\""
        } else {
            str
        }
    }

    /**
     * Generates standard RFC 4180 CSV for all transactions
     */
    fun generateTransactionsCsv(expenses: List<Expense>, folders: List<Folder> = emptyList()): String {
        val folderMap = folders.associateBy { it.id }
        val sb = StringBuilder()

        // Standard CSV Header for data analytics ingestion (SQL, Pandas, Tableau, Power BI)
        sb.append("transaction_id,date,time,timestamp_ms,description,category,amount,currency,paid_by,expense_type,group_folder_id,group_name\n")

        for (exp in expenses.sortedByDescending { it.timestamp }) {
            val dateStr = dateFormat.format(Date(exp.timestamp))
            val timeStr = timeFormat.format(Date(exp.timestamp))
            val expenseType = if (exp.folderId.isNullOrEmpty()) "Personal" else "Group"
            val folderName = exp.folderId?.let { folderMap[it]?.name } ?: ""

            sb.append(escapeCsv(exp.id)).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(timeStr)).append(",")
            sb.append(escapeCsv(exp.timestamp)).append(",")
            sb.append(escapeCsv(exp.description)).append(",")
            sb.append(escapeCsv(exp.category)).append(",")
            sb.append(String.format(Locale.US, "%.2f", exp.amount)).append(",")
            sb.append("USD,")
            sb.append(escapeCsv(exp.paidBy)).append(",")
            sb.append(escapeCsv(expenseType)).append(",")
            sb.append(escapeCsv(exp.folderId ?: "")).append(",")
            sb.append(escapeCsv(folderName)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Generates CSV for Budget Status & Target Variance tracking
     */
    fun generateBudgetStatusCsv(budgetProgressList: List<CategoryBudgetProgress>, monthYear: String): String {
        val sb = StringBuilder()

        // Standard CSV Header for Budget Variance Analysis
        sb.append("budget_id,month_period,category,monthly_limit,actual_spent,remaining_amount,overspent_amount,utilization_percentage,health_status,is_over_budget\n")

        for (progress in budgetProgressList) {
            val isOver = progress.spentAmount > progress.monthlyLimit
            sb.append(escapeCsv(progress.budgetId)).append(",")
            sb.append(escapeCsv(monthYear)).append(",")
            sb.append(escapeCsv(progress.category)).append(",")
            sb.append(String.format(Locale.US, "%.2f", progress.monthlyLimit)).append(",")
            sb.append(String.format(Locale.US, "%.2f", progress.spentAmount)).append(",")
            sb.append(String.format(Locale.US, "%.2f", progress.remainingAmount)).append(",")
            sb.append(String.format(Locale.US, "%.2f", progress.overspentAmount)).append(",")
            sb.append(escapeCsv(progress.progressPercentage)).append(",")
            sb.append(escapeCsv(progress.status.name)).append(",")
            sb.append(escapeCsv(if (isOver) "TRUE" else "FALSE")).append("\n")
        }

        return sb.toString()
    }

    /**
     * Generates an enriched Analytical Star-Schema Fact/Dimension Mart CSV
     * Joining transaction facts with category monthly budget limits and health status.
     */
    fun generateDataAnalystMartCsv(
        expenses: List<Expense>,
        budgetProgressList: List<CategoryBudgetProgress>,
        folders: List<Folder>,
        monthYear: String
    ): String {
        val budgetMap = budgetProgressList.associateBy { it.category }
        val folderMap = folders.associateBy { it.id }
        val sb = StringBuilder()

        sb.append("transaction_id,date,time,month_period,description,category,amount,currency,paid_by,expense_type,group_name,budget_limit,budget_spent,budget_utilization_pct,budget_status\n")

        for (exp in expenses.sortedByDescending { it.timestamp }) {
            val dateStr = dateFormat.format(Date(exp.timestamp))
            val timeStr = timeFormat.format(Date(exp.timestamp))
            val expenseType = if (exp.folderId.isNullOrEmpty()) "Personal" else "Group"
            val folderName = exp.folderId?.let { folderMap[it]?.name } ?: "None"
            val budget = budgetMap[exp.category]

            sb.append(escapeCsv(exp.id)).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(timeStr)).append(",")
            sb.append(escapeCsv(monthYear)).append(",")
            sb.append(escapeCsv(exp.description)).append(",")
            sb.append(escapeCsv(exp.category)).append(",")
            sb.append(String.format(Locale.US, "%.2f", exp.amount)).append(",")
            sb.append("USD,")
            sb.append(escapeCsv(exp.paidBy)).append(",")
            sb.append(escapeCsv(expenseType)).append(",")
            sb.append(escapeCsv(folderName)).append(",")
            sb.append(String.format(Locale.US, "%.2f", budget?.monthlyLimit ?: 0.0)).append(",")
            sb.append(String.format(Locale.US, "%.2f", budget?.spentAmount ?: 0.0)).append(",")
            sb.append(escapeCsv(budget?.progressPercentage ?: 0)).append(",")
            sb.append(escapeCsv(budget?.status?.name ?: "NO_BUDGET")).append("\n")
        }

        return sb.toString()
    }

    /**
     * Writes CSV string to cache and triggers an Android Share / Send Intent
     */
    fun exportAndShareCsv(context: Context, filenamePrefix: String, csvContent: String) {
        try {
            val timestamp = fileTimestampFormat.format(Date())
            val filename = "${filenamePrefix}_$timestamp.csv"
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, filename)
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Data Analyst Export: $filename")
                putExtra(Intent.EXTRA_TEXT, "Exported financial data ($filename) generated by Expense Tracker.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Export & Share CSV via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Copy to clipboard if Intent fails or no handler
            copyToClipboard(context, filenamePrefix, csvContent)
            Toast.makeText(context, "Exported to clipboard: ${e.localizedMessage ?: "File error"}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copies CSV to system clipboard for easy inspection, emulator usage, or direct pasting into Excel / Sheets
     */
    fun copyToClipboard(context: Context, label: String, csvContent: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, csvContent)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "CSV copied to clipboard (${csvContent.lines().size - 1} rows)", Toast.LENGTH_SHORT).show()
    }
}
