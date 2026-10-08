package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.Debt
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.data.Folder
import com.example.ui.BudgetHealth
import com.example.ui.CategoryBudgetProgress
import com.example.ui.ExpenseViewModel
import com.example.ui.OverallBudgetSummary
import com.example.ui.UserState
import com.example.ui.DataAnalystExportDialog
import com.example.ui.DataAnalystPortfolioDialog
import com.example.util.CsvExporter
import com.example.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        val repo = ExpenseRepository(db.expenseDao())
        ExpenseViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

enum class ScreenTab(val title: String, val icon: ImageVector) {
    EXPENSES("Expenses", Icons.Default.Receipt),
    BUDGETS("Budgets", Icons.Default.PieChart),
    GROUPS("Groups", Icons.Default.Group),
    DEBTS("Debts", Icons.Default.Payment),
    ANALYTICS("Analytics", Icons.Default.Analytics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ExpenseViewModel) {
    var currentTab by remember { mutableStateOf(ScreenTab.BUDGETS) }
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showOtpDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showWorkSampleDialog by remember { mutableStateOf(false) }

    // Dialog state for adding personal expense
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    // Dialog state for setting category budget
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var budgetCategoryToEdit by remember { mutableStateOf<String?>(null) }
    var budgetLimitToEdit by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(Unit) {
        viewModel.syncNotification.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle back button if deep in a group folder
    if (selectedFolder != null) {
        BackHandler {
            viewModel.selectFolder(null)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "App Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Expense Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (userState.isLoggedIn) "Logged in as ${userState.name}" else "Tap avatar to Login with OTP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Export CSV for Data Analysis
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("top_export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV for Data Analysis",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Work Sample / Interview Portfolio (PDF)
                    IconButton(
                        onClick = { showWorkSampleDialog = true },
                        modifier = Modifier.testTag("top_work_sample_pdf_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Interview Work Sample (PDF)",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Profile / OTP Login button
                    IconButton(
                        onClick = {
                            if (userState.isLoggedIn) {
                                showProfileDialog = true
                            } else {
                                showOtpDialog = true
                            }
                        },
                        modifier = Modifier.testTag("auth_profile_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (userState.isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (userState.isLoggedIn) userState.userAvatarInitials else "OTP",
                                    color = if (userState.isLoggedIn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (selectedFolder == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    ScreenTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedFolder == null) {
                when (currentTab) {
                    ScreenTab.EXPENSES -> {
                        FloatingActionButton(
                            onClick = { showAddExpenseDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("fab_add_expense")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Expense")
                        }
                    }
                    ScreenTab.BUDGETS -> {
                        FloatingActionButton(
                            onClick = {
                                budgetCategoryToEdit = null
                                budgetLimitToEdit = 100.0
                                showSetBudgetDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("fab_set_budget")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Set Target Budget")
                                Text("Set Budget", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedFolder != null) {
                FolderDetailScreen(
                    folder = selectedFolder!!,
                    viewModel = viewModel,
                    onBack = { viewModel.selectFolder(null) }
                )
            } else {
                when (currentTab) {
                    ScreenTab.EXPENSES -> {
                        PersonalExpensesTab(
                            viewModel = viewModel,
                            onAddExpenseClick = { showAddExpenseDialog = true },
                            onExportClick = { showExportDialog = true }
                        )
                    }
                    ScreenTab.BUDGETS -> {
                        MonthlyBudgetScreen(
                            viewModel = viewModel,
                            onSetBudgetClick = { category, limit ->
                                budgetCategoryToEdit = category
                                budgetLimitToEdit = limit
                                showSetBudgetDialog = true
                            },
                            onExportClick = { showExportDialog = true }
                        )
                    }
                    ScreenTab.GROUPS -> {
                        GroupFoldersTab(viewModel = viewModel)
                    }
                    ScreenTab.DEBTS -> {
                        DebtTrackerTab(viewModel = viewModel)
                    }
                    ScreenTab.ANALYTICS -> {
                        AnalyticsTab(
                            viewModel = viewModel,
                            onOpenExport = { showExportDialog = true },
                            onOpenWorkSample = { showWorkSampleDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Dialog States
    val exportExpenses by viewModel.personalExpenses.collectAsStateWithLifecycle()
    val exportBudgetProgressList by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val exportFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val exportMonthYear by viewModel.selectedMonthYear.collectAsStateWithLifecycle()

    // Dialogs
    if (showExportDialog) {
        DataAnalystExportDialog(
            expenses = exportExpenses,
            budgetProgressList = exportBudgetProgressList,
            folders = exportFolders,
            selectedMonthYear = exportMonthYear,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showWorkSampleDialog) {
        DataAnalystPortfolioDialog(
            expenses = exportExpenses,
            budgetProgressList = exportBudgetProgressList,
            folders = exportFolders,
            onDismiss = { showWorkSampleDialog = false }
        )
    }
    if (showAddExpenseDialog) {
        AddPersonalExpenseDialog(
            viewModel = viewModel,
            onDismiss = { showAddExpenseDialog = false }
        )
    }

    if (showSetBudgetDialog) {
        SetCategoryBudgetDialog(
            viewModel = viewModel,
            initialCategory = budgetCategoryToEdit,
            initialLimit = budgetLimitToEdit,
            onDismiss = { showSetBudgetDialog = false }
        )
    }

    if (showOtpDialog) {
        OtpLoginDialog(
            viewModel = viewModel,
            onDismiss = { showOtpDialog = false }
        )
    }

    if (showProfileDialog) {
        UserProfileDialog(
            userState = userState,
            onDismiss = { showProfileDialog = false },
            onLogout = {
                viewModel.logout()
                showProfileDialog = false
            }
        )
    }
}

// =========================================================================
// MONTHLY BUDGET SCREEN (Target Spending Limit per Category & Tracking)
// =========================================================================

@Composable
fun MonthlyBudgetScreen(
    viewModel: ExpenseViewModel,
    onSetBudgetClick: (String?, Double) -> Unit,
    onExportClick: () -> Unit = {}
) {
    val budgetProgressList by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val overallSummary by viewModel.overallBudgetSummary.collectAsStateWithLifecycle()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsStateWithLifecycle()

    val formattedMonth = remember(selectedMonthYear) {
        try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(selectedMonthYear)
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            selectedMonthYear
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("monthly_budget_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Month",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Monthly Budget Target",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedMonth,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onExportClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("budget_export_csv_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export")
                        }

                        OutlinedButton(
                            onClick = { onSetBudgetClick(null, 150.0) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("quick_add_budget_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Limit")
                        }
                    }
                }
            }
        }

        // Overall Monthly Budget Status Card
        item {
            OverallBudgetCard(summary = overallSummary)
        }

        // Category Budget Header & Count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Limits (${budgetProgressList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (overallSummary.exceededCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${overallSummary.exceededCount} Exceeded",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Empty state
        if (budgetProgressList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "No Budgets",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Category Budgets Set",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Define spending targets per category to stay on track and prevent overspending this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { onSetBudgetClick(null, 200.0) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Set Your First Budget")
                        }
                    }
                }
            }
        } else {
            // Category budget progress list
            items(budgetProgressList, key = { it.category }) { progress ->
                CategoryBudgetCard(
                    progress = progress,
                    onEdit = {
                        onSetBudgetClick(progress.category, progress.monthlyLimit)
                    },
                    onDelete = {
                        viewModel.deleteCategoryBudget(progress.category)
                    }
                )
            }
        }

        // Suggestions for uncapped categories
        item {
            val budgetedCategories = budgetProgressList.map { it.category }.toSet()
            val unbudgeted = viewModel.categories.filter { it !in budgetedCategories }
            if (unbudgeted.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quick Setup for Other Categories",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(unbudgeted) { cat ->
                            OutlinedButton(
                                onClick = { onSetBudgetClick(cat, 150.0) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    getCategoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = getCategoryColor(cat)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(cat)
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacing for FAB
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun OverallBudgetCard(summary: OverallBudgetSummary) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overall_budget_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Monthly Spending",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", summary.totalSpent)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (summary.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (summary.isOverBudget) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (summary.isOverBudget) "Over Budget" else "Target Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (summary.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", summary.totalBudget)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (summary.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val animatedProgress by animateFloatAsState(targetValue = summary.progressFraction, label = "progress")
                val barColor = when {
                    summary.isOverBudget -> MaterialTheme.colorScheme.error
                    summary.progressFraction >= 0.8f -> Color(0xFFF59E0B)
                    else -> MaterialTheme.colorScheme.primary
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${summary.progressPercentage}% of total limit",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                    Text(
                        text = if (summary.isOverBudget) {
                            "Over by $${String.format(Locale.US, "%.2f", summary.totalOverspent)}"
                        } else {
                            "$${String.format(Locale.US, "%.2f", summary.totalRemaining)} remaining"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Summary metrics pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "Budgeted",
                    value = "${summary.budgetedCategoriesCount} Categories",
                    color = MaterialTheme.colorScheme.primary
                )
                if (summary.nearLimitCount > 0) {
                    MetricChip(
                        label = "Near Limit",
                        value = "${summary.nearLimitCount}",
                        color = Color(0xFFF59E0B)
                    )
                }
                if (summary.exceededCount > 0) {
                    MetricChip(
                        label = "Over Limit",
                        value = "${summary.exceededCount}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun MetricChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun CategoryBudgetCard(
    progress: CategoryBudgetProgress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = getCategoryColor(progress.category)
    val statusColor = when (progress.status) {
        BudgetHealth.EXCEEDED -> MaterialTheme.colorScheme.error
        BudgetHealth.WARNING -> Color(0xFFF59E0B)
        BudgetHealth.SAFE -> Color(0xFF10B981)
    }

    val animatedFraction by animateFloatAsState(targetValue = progress.progressFraction, label = "fraction")

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_card_${progress.category.replace(" ", "_").lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Category Icon & Name, Status Badge, Edit / Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = categoryColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getCategoryIcon(progress.category),
                                contentDescription = progress.category,
                                tint = categoryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = progress.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (progress.status == BudgetHealth.EXCEEDED) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Exceeded",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Exceeded by $${String.format(Locale.US, "%.2f", progress.overspentAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (progress.status == BudgetHealth.WARNING) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Near limit",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", progress.remainingAmount)} left (Near limit)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "On track",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", progress.remainingAmount)} left",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Edit and Delete buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_budget_${progress.category}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_budget_${progress.category}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Budget",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Spending and Limit Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", progress.spentAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (progress.status == BudgetHealth.EXCEEDED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = "Limit: $${String.format(Locale.US, "%.2f", progress.monthlyLimit)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Percentage and health status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${progress.progressPercentage}% used",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = when (progress.status) {
                        BudgetHealth.EXCEEDED -> "OVER BUDGET"
                        BudgetHealth.WARNING -> "80%+ USED"
                        BudgetHealth.SAFE -> "ON TRACK"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =========================================================================
// SET / EDIT CATEGORY BUDGET DIALOG
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetCategoryBudgetDialog(
    viewModel: ExpenseViewModel,
    initialCategory: String?,
    initialLimit: Double,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(initialCategory ?: viewModel.categories.first())
    }
    var limitInput by remember {
        mutableStateOf(if (initialLimit > 0) String.format(Locale.US, "%.2f", initialLimit) else "")
    }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetAmounts = listOf(50.0, 100.0, 250.0, 500.0, 1000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCategory != null) "Edit Category Budget" else "Set Target Monthly Limit",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Define a monthly spending target for this category. We will notify you as you approach or exceed it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        leadingIcon = {
                            Icon(
                                getCategoryIcon(selectedCategory),
                                contentDescription = null,
                                tint = getCategoryColor(selectedCategory)
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("budget_category_select")
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        viewModel.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                leadingIcon = {
                                    Icon(
                                        getCategoryIcon(cat),
                                        contentDescription = null,
                                        tint = getCategoryColor(cat)
                                    )
                                },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Target Amount Input
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = {
                        limitInput = it
                        errorMessage = null
                    },
                    label = { Text("Monthly Spending Limit ($)") },
                    placeholder = { Text("e.g. 350.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    leadingIcon = {
                        Text(
                            "$",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )

                // Quick preset buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetAmounts.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        limitInput = String.format(Locale.US, "%.0f", preset)
                                        errorMessage = null
                                    }
                            ) {
                                Text(
                                    text = "$${preset.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = limitInput.toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                    } else {
                        viewModel.setCategoryBudget(selectedCategory, parsed)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =========================================================================
// PERSONAL EXPENSES TAB
// =========================================================================

@Composable
fun PersonalExpensesTab(
    viewModel: ExpenseViewModel,
    onAddExpenseClick: () -> Unit,
    onExportClick: () -> Unit = {}
) {
    val expenses by viewModel.personalExpenses.collectAsStateWithLifecycle()
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    val filteredExpenses = remember(expenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) expenses
        else expenses.filter { it.category == selectedCategoryFilter }
    }

    val totalSpent = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("personal_expenses_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCategoryFilter != null) "$selectedCategoryFilter Expenses" else "Total Personal Spending",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        OutlinedButton(
                            onClick = onExportClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            modifier = Modifier.testTag("personal_expenses_export_csv_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", totalSpent)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${filteredExpenses.size} transactions recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Category Filter Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    val isAll = selectedCategoryFilter == null
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedCategoryFilter = null }
                    ) {
                        Text(
                            text = "All Categories",
                            color = if (isAll) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                items(viewModel.categories) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedCategoryFilter = cat }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(cat),
                                contentDescription = cat,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else getCategoryColor(cat),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = cat,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "No Expenses",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No expenses recorded",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredExpenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    onDelete = { viewModel.deleteExpense(expense) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun ExpenseCard(expense: Expense, onDelete: () -> Unit) {
    val categoryColor = getCategoryColor(expense.category)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = categoryColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(expense.category),
                            contentDescription = expense.category,
                            tint = categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTimestamp(expense.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// ADD PERSONAL EXPENSE DIALOG
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalExpenseDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(viewModel.categories.first()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Personal Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Starbucks Latte") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_desc")
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        errorText = null
                    },
                    label = { Text("Amount ($)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_amount")
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        leadingIcon = {
                            Icon(
                                getCategoryIcon(selectedCategory),
                                contentDescription = null,
                                tint = getCategoryColor(selectedCategory)
                            )
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        viewModel.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                leadingIcon = {
                                    Icon(
                                        getCategoryIcon(cat),
                                        contentDescription = null,
                                        tint = getCategoryColor(cat)
                                    )
                                },
                                onClick = {
                                    selectedCategory = cat
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amount.toDoubleOrNull()
                    if (parsed == null || parsed <= 0) {
                        errorText = "Enter a valid amount"
                    } else {
                        viewModel.addPersonalExpense(
                            amount = parsed,
                            description = description,
                            category = selectedCategory
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("submit_add_expense")
            ) {
                Text("Add Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =========================================================================
// GROUPS / SPLITWISE FOLDERS TAB
// =========================================================================

@Composable
fun GroupFoldersTab(viewModel: ExpenseViewModel) {
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("group_folders_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Splitwise Group Folders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Share expenses with friends and track splits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("btn_create_folder")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Group")
                }

                OutlinedButton(
                    onClick = { showJoinDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("btn_join_folder")
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Join Code")
                }
            }
        }

        if (folders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No shared groups yet. Create or join one!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(folders, key = { it.id }) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { viewModel.selectFolder(folder) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, members ->
                viewModel.createFolder(name, members)
                showCreateDialog = false
            }
        )
    }

    if (showJoinDialog) {
        JoinFolderDialog(
            viewModel = viewModel,
            onDismiss = { showJoinDialog = false }
        )
    }
}

@Composable
fun FolderCard(folder: Folder, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Column {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code: ${folder.id} • ${folder.members}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: Folder,
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val expenses by viewModel.selectedFolderExpenses.collectAsStateWithLifecycle()
    val memberList = remember(folder.members) {
        folder.members.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val settlements = remember(expenses, memberList) {
        viewModel.calculateFolderSettlements(expenses, memberList)
    }
    val totalFolderAmount = remember(expenses) { expenses.sumOf { it.amount } }

    var showAddFolderExpense by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFolderExpense = true },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Shared Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Total Group Spending",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalFolderAmount)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Members: ${folder.members}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Settlements
            if (settlements.isNotEmpty()) {
                item {
                    Text(
                        text = "Suggested Settlements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(settlements) { trans ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${trans.from} owes ${trans.to}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", trans.amount)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Group Expenses (${expenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(expenses) { exp ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(exp.description, fontWeight = FontWeight.Bold)
                            Text(
                                "Paid by ${exp.paidBy} • ${exp.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "$${String.format(Locale.US, "%.2f", exp.amount)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    if (showAddFolderExpense) {
        AddFolderExpenseDialog(
            folderId = folder.id,
            members = memberList,
            viewModel = viewModel,
            onDismiss = { showAddFolderExpense = false }
        )
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var membersInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group Folder", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Hawaii Trip 2026") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = membersInput,
                    onValueChange = { membersInput = it },
                    label = { Text("Other Members (comma separated)") },
                    placeholder = { Text("Alex, Maya, David") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        val list = membersInput.split(",").map { it.trim() }
                        onCreate(groupName, list)
                    }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun JoinFolderDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Group Folder", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.uppercase()
                        error = null
                    },
                    label = { Text("Group Code") },
                    placeholder = { Text("e.g. TRIP-2026") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank()) {
                        viewModel.joinFolderWithCode(
                            code = code,
                            onSuccess = onDismiss,
                            onError = { error = it }
                        )
                    }
                }
            ) { Text("Join") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFolderExpenseDialog(
    folderId: String,
    members: List<String>,
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(members.firstOrNull() ?: "You") }
    var category by remember { mutableStateOf(viewModel.categories.first()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shared Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paidBy,
                    onValueChange = { paidBy = it },
                    label = { Text("Paid By") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amount.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        viewModel.addFolderExpense(
                            folderId = folderId,
                            amount = parsed,
                            description = description,
                            category = category,
                            paidBy = paidBy
                        )
                        onDismiss()
                    } else {
                        errorText = "Invalid amount"
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =========================================================================
// DEBTS TRACKER TAB
// =========================================================================

@Composable
fun DebtTrackerTab(viewModel: ExpenseViewModel) {
    val debts by viewModel.allDebts.collectAsStateWithLifecycle()
    var showAddDebtDialog by remember { mutableStateOf(false) }

    val totalPending = remember(debts) {
        debts.filter { !it.isSettled }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("debt_tracker_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pending Receivables / Debts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalPending)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Button(
                        onClick = { showAddDebtDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Debt")
                    }
                }
            }
        }

        if (debts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No debts or IOUs recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(debts, key = { it.id }) { debt ->
                DebtCard(
                    debt = debt,
                    onToggleSettle = { viewModel.settleDebt(debt) },
                    onDelete = { viewModel.deleteDebt(debt) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onAdd = { person, amt, desc ->
                viewModel.addDebt(person, amt, desc)
                showAddDebtDialog = false
            }
        )
    }
}

@Composable
fun DebtCard(
    debt: Debt,
    onToggleSettle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (debt.isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.personName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = debt.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", debt.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (debt.isSettled) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                )

                Button(
                    onClick = onToggleSettle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (debt.isSettled) Color(0xFF10B981) else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = if (debt.isSettled) "Settled" else "Settle",
                        color = if (debt.isSettled) Color.White else MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, String) -> Unit
) {
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Lent / Borrowed Amount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("Person Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Reason / Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = amount.toDoubleOrNull()
                    if (person.isNotBlank() && p != null && p > 0) {
                        onAdd(person, p, desc)
                    } else {
                        err = "Check fields"
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// =========================================================================
// ANALYTICS & REPORTS TAB
// =========================================================================

@Composable
fun AnalyticsTab(
    viewModel: ExpenseViewModel,
    onOpenExport: () -> Unit = {},
    onOpenWorkSample: () -> Unit = {}
) {
    val context = LocalContext.current
    val expenses by viewModel.personalExpenses.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsStateWithLifecycle()

    val total = remember(expenses) { expenses.sumOf { it.amount } }
    val avgTransaction = remember(expenses) { if (expenses.isNotEmpty()) total / expenses.size else 0.0 }
    val maxTransaction = remember(expenses) { expenses.maxOfOrNull { it.amount } ?: 0.0 }
    val overBudgetCount = remember(budgetProgress) { budgetProgress.count { it.status == BudgetHealth.EXCEEDED } }

    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("analytics_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Analytics & Export Header Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Data Analysis & Spending Intelligence",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total Volume: $${String.format(Locale.US, "%.2f", total)} across ${expenses.size} entries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenExport,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("analytics_export_csv_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                com.example.util.PdfWorkSampleExporter.generateAndSharePdf(
                                    context = context,
                                    expenses = expenses,
                                    budgetProgressList = budgetProgress,
                                    folders = allFolders
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1.15f).testTag("analytics_export_pdf_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Sample", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenWorkSample,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.05f).testTag("analytics_work_sample_btn")
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Portfolio", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Data Analyst KPI Metrics
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Statistical Summary & Pacing",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(title = "Mean Txn", value = "$${String.format(Locale.US, "%.2f", avgTransaction)}", modifier = Modifier.weight(1f))
                        MetricCard(title = "Max Single", value = "$${String.format(Locale.US, "%.2f", maxTransaction)}", modifier = Modifier.weight(1f))
                        MetricCard(title = "Over Budget", value = "$overBudgetCount Categories", modifier = Modifier.weight(1.2f))
                    }
                }
            }
        }

        // Quick CSV Export Actions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Data Exports (RFC 4180 CSV)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Export structured tabular files ready for Pandas, SQL import, Excel, Tableau, or Power BI data pipelines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Export Transaction History
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Transaction History", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${expenses.size} rows with timestamps & categories", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {
                                val csv = CsvExporter.generateTransactionsCsv(expenses, allFolders)
                                CsvExporter.copyToClipboard(context, "transactions.csv", csv)
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Transactions CSV", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                val csv = CsvExporter.generateTransactionsCsv(expenses, allFolders)
                                CsvExporter.exportAndShareCsv(context, "transactions_history", csv)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Transactions CSV", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Export Budget Status & Variance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Budget Status & Variance", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${budgetProgress.size} targets with spent & health status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {
                                val csv = CsvExporter.generateBudgetStatusCsv(budgetProgress, selectedMonthYear)
                                CsvExporter.copyToClipboard(context, "budget_status.csv", csv)
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Budget CSV", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                val csv = CsvExporter.generateBudgetStatusCsv(budgetProgress, selectedMonthYear)
                                CsvExporter.exportAndShareCsv(context, "budget_status_variance", csv)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Budget CSV", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Export Analytical Mart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Star-Schema Analytical Mart", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Denormalized facts joined with monthly targets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {
                                val csv = CsvExporter.generateDataAnalystMartCsv(expenses, budgetProgress, allFolders, selectedMonthYear)
                                CsvExporter.copyToClipboard(context, "analyst_mart.csv", csv)
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Mart CSV", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                val csv = CsvExporter.generateDataAnalystMartCsv(expenses, budgetProgress, allFolders, selectedMonthYear)
                                CsvExporter.exportAndShareCsv(context, "expense_analyst_mart", csv)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Mart CSV", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Category Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(categoryTotals) { (cat, catTotal) ->
            val fraction = if (total > 0) (catTotal / total).toFloat() else 0f
            val pct = (fraction * 100).toInt()
            val color = getCategoryColor(cat)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(getCategoryIcon(cat), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                            Text(cat, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "$${String.format(Locale.US, "%.2f", catTotal)} ($pct%)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// =========================================================================
// OTP LOGIN & USER PROFILE DIALOGS
// =========================================================================

@Composable
fun OtpLoginDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val otpState by viewModel.otpState.collectAsStateWithLifecycle()
    var phoneInput by remember { mutableStateOf(otpState.phoneNumber) }
    var enteredOtp by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (otpState.isOtpSent) "Enter Verification Code" else "Mobile Login with OTP",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!otpState.isOtpSent) {
                    Text(
                        text = "Enter your phone number to receive a secure one-time verification code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = {
                            phoneInput = it
                            localError = null
                        },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1 (555) 000-0000") },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = localError != null,
                        supportingText = localError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth().testTag("phone_input")
                    )
                } else {
                    Text(
                        text = "Code sent to ${otpState.phoneNumber}. Check notification toast or use demo code: ${otpState.generatedOtp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = {
                            enteredOtp = it
                            localError = null
                        },
                        label = { Text("6-Digit OTP") },
                        placeholder = { Text("123456") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = localError != null,
                        supportingText = localError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth().testTag("otp_code_input")
                    )
                    TextButton(
                        onClick = { viewModel.resendOtp {} }
                    ) {
                        Text("Resend Code")
                    }
                }
            }
        },
        confirmButton = {
            if (!otpState.isOtpSent) {
                Button(
                    onClick = {
                        viewModel.sendOtp(
                            phoneNumber = phoneInput,
                            onSuccess = {},
                            onError = { localError = it }
                        )
                    },
                    modifier = Modifier.testTag("send_otp_btn")
                ) {
                    Text("Send OTP")
                }
            } else {
                Button(
                    onClick = {
                        viewModel.verifyOtp(
                            enteredOtp = enteredOtp,
                            onSuccess = onDismiss,
                            onError = { localError = it }
                        )
                    },
                    modifier = Modifier.testTag("verify_otp_btn")
                ) {
                    Text("Verify & Login")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UserProfileDialog(
    userState: UserState,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userState.userAvatarInitials,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(userState.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(userState.phoneNumber.ifBlank { "Mobile User" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// =========================================================================
// HELPERS
// =========================================================================

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food & dining", "food" -> Icons.Default.Fastfood
        "groceries" -> Icons.Default.ShoppingCart
        "shopping" -> Icons.Default.ShoppingBag
        "transportation", "transport" -> Icons.Default.TrendingUp
        "entertainment" -> Icons.Default.Movie
        "utilities & bills", "utilities" -> Icons.Default.Receipt
        "healthcare", "health" -> Icons.Default.LocalHospital
        "travel" -> Icons.Default.Flight
        "education" -> Icons.Default.School
        else -> Icons.Default.AccountBalanceWallet
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food & dining", "food" -> Color(0xFFEF4444)
        "groceries" -> Color(0xFF10B981)
        "shopping" -> Color(0xFFEC4899)
        "transportation", "transport" -> Color(0xFF3B82F6)
        "entertainment" -> Color(0xFF8B5CF6)
        "utilities & bills", "utilities" -> Color(0xFFF59E0B)
        "healthcare", "health" -> Color(0xFF14B8A6)
        "travel" -> Color(0xFF06B6D4)
        "education" -> Color(0xFF6366F1)
        else -> Color(0xFF64748B)
    }
}

fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}
