package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.ExpenseViewModel
import com.example.ui.Transaction
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room DB and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())
        
        // Instantiate ViewModel
        val viewModel: ExpenseViewModel by viewModels {
            ExpenseViewModel.Factory(repository)
        }

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // UI State
    var selectedTab by remember { mutableStateOf(0) } // 0: Personal, 1: Groups, 2: Debts, 3: Reports
    val personalExpenses by viewModel.personalExpenses.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val debts by viewModel.allDebts.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val selectedFolderExpenses by viewModel.selectedFolderExpenses.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showAddPersonalExpenseDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showJoinFolderDialog by remember { mutableStateOf(false) }
    var showAddFolderExpenseDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }

    // Listen for simulated multiplayer sync activities and show a Material snackbar!
    LaunchedEffect(Unit) {
        viewModel.syncNotification.collectLatest { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (selectedFolder == null) {
                NavigationBar(
                    containerColor = Color(0xFFF3EDF7),
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Personal", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp) },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = "Personal Expenses") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D1B20),
                            selectedTextColor = Color(0xFF1D1B20),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color(0xFF1D1B20).copy(alpha = 0.6f),
                            unselectedTextColor = Color(0xFF1D1B20).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_personal")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Trip Groups", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp) },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Group Trip Folders") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D1B20),
                            selectedTextColor = Color(0xFF1D1B20),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color(0xFF1D1B20).copy(alpha = 0.6f),
                            unselectedTextColor = Color(0xFF1D1B20).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_groups")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("Debt Logs", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp) },
                        icon = { Icon(Icons.Default.CompareArrows, contentDescription = "Money Lent or Borrowed") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D1B20),
                            selectedTextColor = Color(0xFF1D1B20),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color(0xFF1D1B20).copy(alpha = 0.6f),
                            unselectedTextColor = Color(0xFF1D1B20).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_debts")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        label = { Text("Reports", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp) },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = "Monthly Spend Reports") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D1B20),
                            selectedTextColor = Color(0xFF1D1B20),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color(0xFF1D1B20).copy(alpha = 0.6f),
                            unselectedTextColor = Color(0xFF1D1B20).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_reports")
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color(0xFFFDF7FF) // Match page body bg-[#fdf7ff]
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedFolder == null) {
                    val subtitle = when (selectedTab) {
                        0 -> viewModel.getCurrentMonthYear()
                        1 -> "Collaborative Splits"
                        2 -> "Lending Manager"
                        else -> "Analytics Breakdown"
                    }
                    val title = when (selectedTab) {
                        0 -> "Dashboard"
                        1 -> "Shared Folders"
                        2 -> "Lending"
                        else -> "Spend Reports"
                    }
                    HighDensityHeader(subtitle = subtitle, title = title, initials = "RS")
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = if (selectedFolder != null) 99 else selectedTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "tab_transitions"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> PersonalExpensesTab(
                                expenses = personalExpenses,
                                viewModel = viewModel,
                                onAddClick = { showAddPersonalExpenseDialog = true }
                            )
                            1 -> GroupFoldersTab(
                                folders = folders,
                                onFolderSelect = { folder -> viewModel.selectFolder(folder) },
                                onCreateFolderClick = { showCreateFolderDialog = true },
                                onJoinFolderClick = { showJoinFolderDialog = true },
                                viewModel = viewModel
                            )
                            2 -> DebtTrackerTab(
                                debts = debts,
                                viewModel = viewModel,
                                onAddDebtClick = { showAddDebtDialog = true }
                            )
                            3 -> ReportsTab(
                                personalExpenses = personalExpenses,
                                viewModel = viewModel
                            )
                            99 -> FolderDetailScreen(
                                folder = selectedFolder!!,
                                expenses = selectedFolderExpenses,
                                viewModel = viewModel,
                                onBackClick = { viewModel.selectFolder(null) },
                                onAddExpenseClick = { showAddFolderExpenseDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showAddPersonalExpenseDialog) {
        AddPersonalExpenseDialog(
            categories = viewModel.categories,
            onDismiss = { showAddPersonalExpenseDialog = false },
            onConfirm = { amount, category, desc ->
                viewModel.addPersonalExpense(amount, category, desc)
                showAddPersonalExpenseDialog = false
                Toast.makeText(context, "Personal expense added!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name, friendsString ->
                val friends = friendsString.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val code = viewModel.createFolder(name, friends)
                showCreateFolderDialog = false
                Toast.makeText(context, "Group created with code $code!", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showJoinFolderDialog) {
        JoinFolderDialog(
            onDismiss = { showJoinFolderDialog = false },
            onConfirm = { code ->
                viewModel.joinFolderWithCode(
                    code = code,
                    onSuccess = {
                        showJoinFolderDialog = false
                        Toast.makeText(context, "Joined shared folder!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    if (showAddFolderExpenseDialog && selectedFolder != null) {
        AddFolderExpenseDialog(
            folder = selectedFolder!!,
            categories = viewModel.categories,
            onDismiss = { showAddFolderExpenseDialog = false },
            onConfirm = { amount, category, desc, paidBy ->
                viewModel.addFolderExpense(selectedFolder!!.id, amount, category, desc, paidBy)
                showAddFolderExpenseDialog = false
                Toast.makeText(context, "Trip expense recorded!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onConfirm = { name, amount, desc, isLent ->
                viewModel.addDebt(name, amount, desc, isLent)
                showAddDebtDialog = false
                Toast.makeText(context, "Debt log saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ==========================================
// TAB 1: PERSONAL MANUAL EXPENSES
// ==========================================
@Composable
fun PersonalExpensesTab(
    expenses: List<Expense>,
    viewModel: ExpenseViewModel,
    onAddClick: () -> Unit
) {
    val totalSpend = expenses.sumOf { it.amount }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // High Density Purple Spent Card Container
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF6750A4)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "MONTHLY SPENDING",
                            color = Color(0xFFEADDFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", totalSpend)}",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+12% vs last month",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ACTIVE SPLITS",
                                color = Color(0xFFEADDFF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${expenses.size} Items",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "LATEST SPEND",
                                color = Color(0xFFEADDFF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (expenses.isNotEmpty()) "₹${String.format(Locale.US, "%.2f", expenses.first().amount)}" else "₹0.00",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row {
                IconButton(
                    onClick = { viewModel.simulateCloudSync() },
                    modifier = Modifier.testTag("personal_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Cloud",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_personal_expense_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No expenses recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Tap 'Add' to record your first manual expense.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseCard(expense = expense, onDelete = { viewModel.deleteExpense(expense) })
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: Expense, onDelete: () -> Unit) {
    val categoryColor = getCategoryColor(expense.category)
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFECE6F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_item_${expense.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Density category badge: 40x40, rounded-xl, bg-[#f7f2fa]
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFFF7F2FA),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(expense.category),
                    contentDescription = expense.category,
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { expense.category },
                    color = Color(0xFF1D1B20),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge Capsule: bg-color/10, text-color/100
                    Box(
                        modifier = Modifier
                            .background(
                                color = categoryColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = expense.category.uppercase(),
                            color = categoryColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = formatTimestamp(expense.timestamp),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pricing & Actions: Negative red pricing
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-₹${String.format(Locale.US, "%.2f", expense.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB3261E),
                    fontSize = 14.sp
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Expense",
                        tint = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 2: TRIP GROUPS (FOLDERS)
// ==========================================
@Composable
fun GroupFoldersTab(
    folders: List<Folder>,
    onFolderSelect: (Folder) -> Unit,
    onCreateFolderClick: () -> Unit,
    onJoinFolderClick: () -> Unit,
    viewModel: ExpenseViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Shared Trips",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Split costs with friends & family",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { viewModel.simulateCloudSync() },
                modifier = Modifier.testTag("group_sync_all_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync Folders",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Options to join or create
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCreateFolderClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("create_folder_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Create")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Folder")
            }

            OutlinedButton(
                onClick = onJoinFolderClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("join_folder_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Join")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Join with Code")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (folders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "No Groups",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No shared trip folders yet.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Create a folder for your trip and invite your friends using a unique access code!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            Text(
                text = "Active Folders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(folders) { folder ->
                    FolderCard(folder = folder, onClick = { onFolderSelect(folder) }, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FolderCard(folder: Folder, onClick: () -> Unit, viewModel: ExpenseViewModel) {
    val expensesFlow = remember(folder.id) { viewModel.getFolderExpenses(folder.id) }
    val expenses by expensesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val totalSpend = expenses.sumOf { it.amount }
    
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE6F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("folder_item_${folder.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEADDFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = Color(0xFF21005D),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = folder.name,
                        color = Color(0xFF1D1B20),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Code badge
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFFE8DEF8),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CODE: ${folder.id}",
                        color = Color(0xFF6750A4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MEMBERS",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${folder.getMemberList().size} connected",
                        color = Color(0xFF1D1B20),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL EXPENDITURE",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", totalSpend)}",
                        color = Color(0xFF6750A4),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show short list of member names
            Text(
                text = "Members: " + folder.getMemberList().joinToString(", "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

// ==========================================
// SUB-SCREEN: FOLDER DETAIL & SPLIT CALCULATIONS
// ==========================================
@Composable
fun FolderDetailScreen(
    folder: Folder,
    expenses: List<Expense>,
    viewModel: ExpenseViewModel,
    onBackClick: () -> Unit,
    onAddExpenseClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val members = folder.getMemberList()
    val totalTripSpend = expenses.sumOf { it.amount }
    val myShare = totalTripSpend / (if (members.isNotEmpty()) members.size else 1)
    
    // Compute total paid by Me
    val paidByMe = expenses.filter { it.paidBy == "Me" }.sumOf { it.amount }
    val myBalance = paidByMe - myShare // positive means people owe me, negative means I owe people

    // Calculate optimal debt transactions
    val transactions = viewModel.calculateFolderSettlements(members, expenses)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top app bar back controller
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = { viewModel.simulateFriendActivity() },
                modifier = Modifier.testTag("trigger_friend_simulate_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Simulate Activity",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Access Code card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Access Invite Code",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = folder.id,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(folder.id))
                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Code")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Multiplayer Sync Status banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Live Syncing",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connected Sync Room",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { viewModel.simulateFriendActivity() },
                        modifier = Modifier.testTag("friend_expense_action_btn")
                    ) {
                        Text("+ Friend Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { viewModel.simulateCloudSync() }
                    ) {
                        Text("Sync All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Split calculations stats
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Split Report Overview",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Trip Cost", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.US, "%.2f", totalTripSpend)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Your Share", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.US, "%.2f", myShare)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Your Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val textBalanceColor = if (myBalance >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                        val prefix = if (myBalance >= 0) "Owed +₹" else "You owe ₹"
                        Text(
                            text = "$prefix${String.format(Locale.US, "%.2f", abs(myBalance))}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textBalanceColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transactions settlements list
        Text(
            text = "Settlement suggestions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Settled",
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All costs are fully settled! Perfect balance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 140.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(transactions) { trans ->
                    TransactionSettlementRow(trans)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trip expenses header and FAB
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Folder Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Button(
                onClick = onAddExpenseClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_folder_expense_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Trip Expense")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Expense")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses added inside this folder yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses) { exp ->
                    FolderExpenseCard(expense = exp, onDelete = { viewModel.deleteExpense(exp) })
                }
            }
        }
    }
}

@Composable
fun TransactionSettlementRow(trans: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = trans.from,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Owes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .padding(horizontal = 4.dp)
            )
            Text(
                text = trans.to,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        }

        Text(
            text = "Owes ₹${String.format(Locale.US, "%.2f", trans.amount)}",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun FolderExpenseCard(expense: Expense, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = getCategoryColor(expense.category).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(expense.category),
                    contentDescription = expense.category,
                    tint = getCategoryColor(expense.category),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { expense.category },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Paid by ${expense.paidBy}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatTimestamp(expense.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pricing details & delete
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", expense.amount)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete expense",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 3: GENERAL DEBT TRACKER
// ==========================================
@Composable
fun DebtTrackerTab(
    debts: List<Debt>,
    viewModel: ExpenseViewModel,
    onAddDebtClick: () -> Unit
) {
    val activeDebts = debts.filter { !it.isSettled }
    val settledDebts = debts.filter { it.isSettled }

    val amountIOwe = activeDebts.filter { it.amount < 0 }.sumOf { abs(it.amount) }
    val amountOwedToMe = activeDebts.filter { it.amount > 0 }.sumOf { it.amount }
    val netBalance = amountOwedToMe - amountIOwe

    var showSettledHistory by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Money Lent / Borrowed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Keep track of direct money transfers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = onAddDebtClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_debt_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Debt Log")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Debt")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Debt Overview statistics card
        OutlinedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFECE6F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "LENDING BALANCE SUMMARY",
                            color = Color(0xFF6750A4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val prefix = if (netBalance >= 0) "+₹" else "-₹"
                        val netColor = if (netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFB3261E)
                        Text(
                            text = "$prefix${String.format(Locale.US, "%.2f", abs(netBalance))}",
                            color = netColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (netBalance >= 0) "Surplus" else "Deficit",
                            color = Color(0xFF6750A4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7FF))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "PEOPLE OWE YOU",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", amountOwedToMe)}",
                                color = Color(0xFF2E7D32),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7FF))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "YOU OWE PEOPLE",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", amountIOwe)}",
                                color = Color(0xFFB3261E),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle History Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showSettledHistory) "Settled Logs History" else "Active Debt Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            TextButton(
                onClick = { showSettledHistory = !showSettledHistory }
            ) {
                Text(if (showSettledHistory) "Show Active" else "Show History")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val listToDisplay = if (showSettledHistory) settledDebts else activeDebts

        if (listToDisplay.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (showSettledHistory) Icons.Default.History else Icons.Default.CheckCircleOutline,
                        contentDescription = "Empty",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (showSettledHistory) "No history found." else "Clear! Nobody owes you anything.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(listToDisplay) { debt ->
                    DebtCard(debt = debt, onSettle = { viewModel.settleDebt(debt) }, onDelete = { viewModel.deleteDebt(debt) })
                }
            }
        }
    }
}

@Composable
fun DebtCard(debt: Debt, onSettle: () -> Unit, onDelete: () -> Unit) {
    val isOwed = debt.amount > 0
    val absAmount = abs(debt.amount)
    val themeColor = if (isOwed) Color(0xFF2E7D32) else Color(0xFFB3261E)
    
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE6F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_item_${debt.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicator Badge: bg-color/10, text-color/100
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = themeColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOwed) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = if (isOwed) "Lent" else "Borrowed",
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.personName,
                    color = Color(0xFF1D1B20),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = debt.description.ifBlank { if (isOwed) "Lent money" else "Borrowed money" },
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTimestamp(debt.timestamp),
                    color = Color.LightGray,
                    fontSize = 9.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", absAmount)}",
                    fontWeight = FontWeight.Bold,
                    color = if (debt.isSettled) Color.Gray.copy(alpha = 0.6f) else themeColor,
                    fontSize = 15.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!debt.isSettled) {
                        TextButton(
                            onClick = onSettle,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Settle up",
                                color = Color(0xFF6750A4),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Log",
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: REPORTS & ANALYTICS
// ==========================================
@Composable
fun ReportsTab(
    personalExpenses: List<Expense>,
    viewModel: ExpenseViewModel
) {
    val totalPersonal = personalExpenses.sumOf { it.amount }
    val categoryMap = viewModel.getCategoryBreakdown(personalExpenses)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Monthly Spending Report",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = viewModel.getCurrentMonthYear(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (personalExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Empty Report",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Not enough data for calculations.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Add manual expenses in the Personal tab to populate details here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Interactive Donut Chart item
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))

                            // Donut Chart drawing inside a canvas!
                            Box(
                                modifier = Modifier.size(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var startAngle = -90f
                                    categoryMap.forEach { (cat, amount) ->
                                        val sweepAngle = ((amount / totalPersonal) * 360f).toFloat()
                                        drawArc(
                                            color = getCategoryColor(cat),
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        startAngle += sweepAngle
                                    }
                                }

                                // Center Total display
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Total Spent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.0f", totalPersonal)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Legend chips rows
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                maxItemsInEachRow = 3
                            ) {
                                categoryMap.forEach { (category, amount) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(getCategoryColor(category), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$category (${String.format(Locale.US, "%.0f", (amount / totalPersonal) * 100)}%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Detailed Progress List
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Analytical Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            categoryMap.forEach { (cat, amount) ->
                                val pct = (amount / totalPersonal).toFloat()
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = getCategoryIcon(cat),
                                                contentDescription = cat,
                                                tint = getCategoryColor(cat),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = cat, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        }
                                        Text(
                                            text = "₹${String.format(Locale.US, "%.2f", amount)} (${String.format(Locale.US, "%.0f", pct * 100)}%)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        color = getCategoryColor(cat),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        maxItemsInEachRow = maxItemsInEachRow
    ) {
        content()
    }
}

// ==========================================
// FORM DIALOGS IMPLEMENTATIONS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalExpenseDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Personal Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_expense_amount_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_expense_description_input")
                )

                // Category dropdown menu
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("add_expense_category_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
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
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(amount, selectedCategory, description)
                    }
                },
                modifier = Modifier.testTag("add_expense_submit_btn")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Collaborative Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "A shared folder stores transactions for specific trips or groups (e.g. going on a trip with 4 people). Everyone can add expense details, and the system automatically splits the bill.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Trip Folder Name") },
                    placeholder = { Text("e.g. Goa Trip 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_folder_name_input")
                )

                OutlinedTextField(
                    value = friends,
                    onValueChange = { friends = it },
                    label = { Text("Friend Names (comma-separated)") },
                    placeholder = { Text("Alice, Bob, Charlie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_folder_members_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, friends)
                    }
                },
                modifier = Modifier.testTag("create_folder_submit_btn")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun JoinFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Shared Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the specific folder code (e.g. TRIP-123456) shared by your friends to sync and access the shared database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Trip Code") },
                    placeholder = { Text("TRIP-XXXXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("join_code_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank()) {
                        onConfirm(code)
                    }
                },
                modifier = Modifier.testTag("join_code_submit")
            ) {
                Text("Join & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFolderExpenseDialog(
    folder: Folder,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var expandedCategory by remember { mutableStateOf(false) }
    
    val members = folder.getMemberList()
    var selectedPayer by remember { mutableStateOf("Me") }
    var expandedPayer by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Trip Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Payer Selection dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedPayer,
                    onExpandedChange = { expandedPayer = !expandedPayer }
                ) {
                    OutlinedTextField(
                        value = selectedPayer,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Who paid for this?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPayer) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPayer,
                        onDismissRequest = { expandedPayer = false }
                    ) {
                        members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member) },
                                onClick = {
                                    selectedPayer = member
                                    expandedPayer = false
                                }
                            )
                        }
                    }
                }

                // Category selection dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
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
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(amount, selectedCategory, description, selectedPayer)
                    }
                }
            ) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLent by remember { mutableStateOf(true) } // true = Lent (Given), false = Borrowed (Taken)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Money Log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isLent = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLent) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isLent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("I Lent Money")
                    }

                    Button(
                        onClick = { isLent = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isLent) Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isLent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("I Borrowed")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_debt_person_input")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_debt_amount_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_debt_description_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amount > 0) {
                        onConfirm(name, amount, description, isLent)
                    }
                },
                modifier = Modifier.testTag("add_debt_submit_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// UTILS & STYLE ADAPTERS
// ==========================================

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return sdf.format(Date(timestamp))
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Food" -> Icons.Default.Restaurant
        "Transport" -> Icons.Default.DirectionsCar
        "Lodging" -> Icons.Default.Hotel
        "Entertainment" -> Icons.Default.LocalPlay
        "Shopping" -> Icons.Default.LocalMall
        "Bills" -> Icons.Default.Receipt
        "Health" -> Icons.Default.HealthAndSafety
        else -> Icons.Default.AttachMoney
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Food" -> Color(0xFFEF5350)        // Red
        "Transport" -> Color(0xFF42A5F5)   // Blue
        "Lodging" -> Color(0xFFAB47BC)     // Purple
        "Entertainment" -> Color(0xFFFFCA28) // Amber
        "Shopping" -> Color(0xFF26A69A)    // Teal
        "Bills" -> Color(0xFFFF7043)       // Orange
        "Health" -> Color(0xFF66BB6A)      // Green
        else -> Color(0xFF78909C)          // Gray
    }
}

@Composable
fun HighDensityHeader(
    subtitle: String,
    title: String,
    initials: String = "RS"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFDF7FF))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = subtitle.uppercase(),
                color = Color(0xFF6750A4),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = title,
                color = Color(0xFF1D1B20),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFEADDFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color(0xFF21005D),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
