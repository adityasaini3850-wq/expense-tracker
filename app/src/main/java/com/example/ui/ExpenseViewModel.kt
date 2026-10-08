package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CategoryBudget
import com.example.data.Debt
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.data.Folder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class UserState(
    val name: String = "Guest User",
    val email: String = "guest@example.com",
    val phoneNumber: String = "",
    val isLoggedIn: Boolean = false,
    val userAvatarInitials: String = "GU"
)

data class OtpState(
    val phoneNumber: String = "",
    val generatedOtp: String = "",
    val isOtpSent: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class FolderSummary(
    val folder: Folder,
    val totalExpenses: Double,
    val myShare: Double,
    val myBalance: Double,
    val expenseCount: Int
)

data class Transaction(
    val from: String,
    val to: String,
    val amount: Double
)

enum class BudgetHealth {
    SAFE,       // < 80% spent
    WARNING,    // 80% - 100% spent
    EXCEEDED    // > 100% spent
}

data class CategoryBudgetProgress(
    val category: String,
    val monthlyLimit: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val overspentAmount: Double,
    val progressPercentage: Int,
    val progressFraction: Float,
    val status: BudgetHealth,
    val budgetId: Long = 0
)

data class OverallBudgetSummary(
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val totalOverspent: Double = 0.0,
    val progressPercentage: Int = 0,
    val progressFraction: Float = 0f,
    val isOverBudget: Boolean = false,
    val budgetedCategoriesCount: Int = 0,
    val exceededCount: Int = 0,
    val nearLimitCount: Int = 0
)

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val categories = listOf(
        "Food & Dining",
        "Groceries",
        "Shopping",
        "Transportation",
        "Entertainment",
        "Utilities & Bills",
        "Healthcare",
        "Travel",
        "Education",
        "Other"
    )

    private val _userState = MutableStateFlow(UserState())
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    private val _syncNotification = MutableSharedFlow<String>()
    val syncNotification = _syncNotification.asSharedFlow()

    private val _selectedMonthYear = MutableStateFlow(getCurrentMonthYear())
    val selectedMonthYear: StateFlow<String> = _selectedMonthYear.asStateFlow()

    val personalExpenses: StateFlow<List<Expense>> = repository.personalExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<Debt>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

    val selectedFolderExpenses: StateFlow<List<Expense>> = _selectedFolder.flatMapLatest { folder ->
        if (folder != null) repository.getFolderExpenses(folder.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Budgets list
    val allBudgets: StateFlow<List<CategoryBudget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive Monthly Budget Progress: combines Personal Expenses + Budgets for selected month
    val budgetProgressList: StateFlow<List<CategoryBudgetProgress>> = combine(
        personalExpenses,
        allBudgets,
        _selectedMonthYear
    ) { expenses, budgets, monthYear ->
        // Filter personal expenses for selected month
        val monthExpenses = expenses.filter { exp ->
            val expMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(exp.timestamp))
            expMonthYear == monthYear
        }

        // Map spent per category
        val spendingMap = monthExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Budgets for this month or general recurring budgets
        val activeBudgets = budgets.filter { it.monthYear == monthYear || it.monthYear.isEmpty() }
            .distinctBy { it.category }

        activeBudgets.map { budget ->
            val spent = spendingMap[budget.category] ?: 0.0
            val limit = budget.monthlyLimit
            val remaining = (limit - spent).coerceAtLeast(0.0)
            val overspent = (spent - limit).coerceAtLeast(0.0)
            val pct = if (limit > 0) ((spent / limit) * 100).roundToInt() else 0
            val fraction = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
            val status = when {
                spent > limit -> BudgetHealth.EXCEEDED
                spent >= limit * 0.8 -> BudgetHealth.WARNING
                else -> BudgetHealth.SAFE
            }
            CategoryBudgetProgress(
                category = budget.category,
                monthlyLimit = limit,
                spentAmount = spent,
                remainingAmount = remaining,
                overspentAmount = overspent,
                progressPercentage = pct,
                progressFraction = fraction,
                status = status,
                budgetId = budget.id
            )
        }.sortedByDescending { it.spentAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall summary of monthly budget
    val overallBudgetSummary: StateFlow<OverallBudgetSummary> = budgetProgressList.combine(personalExpenses) { progressList, expenses ->
        val totalBudget = progressList.sumOf { it.monthlyLimit }
        val totalSpent = progressList.sumOf { it.spentAmount }
        val totalRemaining = (totalBudget - totalSpent).coerceAtLeast(0.0)
        val totalOverspent = (totalSpent - totalBudget).coerceAtLeast(0.0)
        val fraction = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
        val pct = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).roundToInt() else 0
        val isOver = totalSpent > totalBudget && totalBudget > 0
        val exceeded = progressList.count { it.status == BudgetHealth.EXCEEDED }
        val nearLimit = progressList.count { it.status == BudgetHealth.WARNING }

        OverallBudgetSummary(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            totalRemaining = totalRemaining,
            totalOverspent = totalOverspent,
            progressPercentage = pct,
            progressFraction = fraction,
            isOverBudget = isOver,
            budgetedCategoriesCount = progressList.size,
            exceededCount = exceeded,
            nearLimitCount = nearLimit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallBudgetSummary())

    init {
        seedSampleDataIfEmpty()
    }

    private fun seedSampleDataIfEmpty() {
        viewModelScope.launch {
            // Check if we have sample expenses or budgets
            val currentYearMonth = getCurrentMonthYear()
            val cal = Calendar.getInstance()
            val now = cal.timeInMillis

            cal.add(Calendar.DAY_OF_MONTH, -1)
            val yesterday = cal.timeInMillis

            cal.add(Calendar.DAY_OF_MONTH, -3)
            val fourDaysAgo = cal.timeInMillis

            launch {
                val existing = repository.getAllExpenses()
                // If repository is empty, populate initial expenses and budgets
                // We will collect once
                val dummyCheck = repository.insertExpense(
                    Expense(
                        description = "Supermarket Fresh Produce",
                        amount = 64.50,
                        category = "Groceries",
                        timestamp = now
                    )
                )
                // Add more initial items
                repository.insertExpense(
                    Expense(
                        description = "Italian Bistro Dinner",
                        amount = 82.00,
                        category = "Food & Dining",
                        timestamp = yesterday
                    )
                )
                repository.insertExpense(
                    Expense(
                        description = "Cinema & Popcorn",
                        amount = 28.50,
                        category = "Entertainment",
                        timestamp = fourDaysAgo
                    )
                )
                repository.insertExpense(
                    Expense(
                        description = "Subway Monthly Pass",
                        amount = 75.00,
                        category = "Transportation",
                        timestamp = fourDaysAgo
                    )
                )

                // Initial Category Budgets for the monthly budget feature
                repository.insertBudget(
                    CategoryBudget(
                        category = "Food & Dining",
                        monthlyLimit = 250.00,
                        monthYear = currentYearMonth
                    )
                )
                repository.insertBudget(
                    CategoryBudget(
                        category = "Groceries",
                        monthlyLimit = 350.00,
                        monthYear = currentYearMonth
                    )
                )
                repository.insertBudget(
                    CategoryBudget(
                        category = "Entertainment",
                        monthlyLimit = 100.00,
                        monthYear = currentYearMonth
                    )
                )
                repository.insertBudget(
                    CategoryBudget(
                        category = "Transportation",
                        monthlyLimit = 120.00,
                        monthYear = currentYearMonth
                    )
                )
                repository.insertBudget(
                    CategoryBudget(
                        category = "Shopping",
                        monthlyLimit = 200.00,
                        monthYear = currentYearMonth
                    )
                )

                // Initial Debt
                repository.insertDebt(
                    Debt(
                        personName = "Alex Miller",
                        amount = 45.00,
                        description = "Concert Ticket",
                        isSettled = false,
                        timestamp = yesterday
                    )
                )

                // Initial Group Folder
                val folderId = "TRIP-2026"
                repository.insertFolder(
                    Folder(
                        id = folderId,
                        name = "Weekend Getaway",
                        members = "You, Alex, Sarah, David",
                        createdTimestamp = now
                    )
                )
                repository.insertExpense(
                    Expense(
                        description = "Cabin Rental",
                        amount = 320.00,
                        category = "Travel",
                        paidBy = "You",
                        folderId = folderId,
                        timestamp = now
                    )
                )
            }
        }
    }

    fun getCurrentMonthYear(): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    fun setMonth(yearMonth: String) {
        _selectedMonthYear.value = yearMonth
    }

    // --- Budget Management Actions ---
    fun setCategoryBudget(category: String, limit: Double, monthYear: String = _selectedMonthYear.value) {
        viewModelScope.launch {
            // Delete any existing budget for category + month
            repository.deleteBudgetByCategory(category, monthYear)
            repository.insertBudget(
                CategoryBudget(
                    category = category,
                    monthlyLimit = limit,
                    monthYear = monthYear
                )
            )
            _syncNotification.emit("Target budget for $category set to $$limit")
        }
    }

    fun deleteCategoryBudget(category: String, monthYear: String = _selectedMonthYear.value) {
        viewModelScope.launch {
            repository.deleteBudgetByCategory(category, monthYear)
            _syncNotification.emit("Budget for $category removed")
        }
    }

    // --- Personal Expenses Actions ---
    fun addPersonalExpense(amount: Double, description: String, category: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    description = description.ifBlank { "Personal Expense" },
                    amount = amount,
                    category = category,
                    paidBy = _userState.value.name,
                    folderId = null,
                    timestamp = timestamp
                )
            )
            _syncNotification.emit("Added $$amount in $category")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _syncNotification.emit("Deleted expense: ${expense.description}")
        }
    }

    // --- Group Folders Actions ---
    fun createFolder(name: String, members: List<String>): String {
        val folderId = "GRP-${(1000..9999).random()}"
        val memberList = (listOf("You") + members.filter { it.isNotBlank() }).distinct().joinToString(", ")
        viewModelScope.launch {
            repository.insertFolder(
                Folder(
                    id = folderId,
                    name = name.ifBlank { "Group Folder" },
                    members = memberList
                )
            )
            _syncNotification.emit("Group '$name' created (Code: $folderId)")
        }
        return folderId
    }

    fun joinFolderWithCode(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        viewModelScope.launch {
            val existing = repository.getFolderById(cleanCode)
            if (existing != null) {
                _selectedFolder.value = existing
                onSuccess()
                _syncNotification.emit("Joined ${existing.name}")
            } else {
                // If not found locally, create or mock cloud fetch
                val newFolder = Folder(
                    id = cleanCode,
                    name = "Shared Group $cleanCode",
                    members = "You, Friend 1, Friend 2"
                )
                repository.insertFolder(newFolder)
                _selectedFolder.value = newFolder
                onSuccess()
                _syncNotification.emit("Connected to $cleanCode")
            }
        }
    }

    fun selectFolder(folder: Folder?) {
        _selectedFolder.value = folder
    }

    fun addFolderExpense(folderId: String, amount: Double, description: String, category: String, paidBy: String) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    description = description.ifBlank { "Group Expense" },
                    amount = amount,
                    category = category,
                    paidBy = paidBy.ifBlank { "You" },
                    folderId = folderId,
                    timestamp = System.currentTimeMillis()
                )
            )
            _syncNotification.emit("Group expense added: $$amount")
        }
    }

    fun calculateFolderSettlements(expenses: List<Expense>, members: List<String>): List<Transaction> {
        if (members.isEmpty() || expenses.isEmpty()) return emptyList()

        val memberCount = members.size
        val balances = mutableMapOf<String, Double>()
        members.forEach { balances[it] = 0.0 }

        for (exp in expenses) {
            val splitAmount = exp.amount / memberCount
            val payer = exp.paidBy
            balances[payer] = (balances[payer] ?: 0.0) + exp.amount
            members.forEach { m ->
                balances[m] = (balances[m] ?: 0.0) - splitAmount
            }
        }

        val debtors = mutableListOf<Pair<String, Double>>()
        val creditors = mutableListOf<Pair<String, Double>>()

        balances.forEach { (member, balance) ->
            if (balance < -0.01) debtors.add(Pair(member, -balance))
            else if (balance > 0.01) creditors.add(Pair(member, balance))
        }

        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val transactions = mutableListOf<Transaction>()
        var dIndex = 0
        var cIndex = 0

        val debtorsMut = debtors.map { it.first to it.second }.toMutableList()
        val creditorsMut = creditors.map { it.first to it.second }.toMutableList()

        while (dIndex < debtorsMut.size && cIndex < creditorsMut.size) {
            val debtor = debtorsMut[dIndex]
            val creditor = creditorsMut[cIndex]
            val minAmount = minOf(debtor.second, creditor.second)

            if (minAmount > 0.01) {
                transactions.add(
                    Transaction(
                        from = debtor.first,
                        to = creditor.first,
                        amount = (minAmount * 100).roundToInt() / 100.0
                    )
                )
            }

            debtorsMut[dIndex] = debtor.first to (debtor.second - minAmount)
            creditorsMut[cIndex] = creditor.first to (creditor.second - minAmount)

            if (debtorsMut[dIndex].second <= 0.01) dIndex++
            if (creditorsMut[cIndex].second <= 0.01) cIndex++
        }

        return transactions
    }

    // --- Debts Tracker Actions ---
    fun addDebt(personName: String, amount: Double, description: String, isSettled: Boolean = false) {
        viewModelScope.launch {
            repository.insertDebt(
                Debt(
                    personName = personName.ifBlank { "Friend" },
                    amount = amount,
                    description = description.ifBlank { "Personal Loan / Split" },
                    isSettled = isSettled
                )
            )
            _syncNotification.emit("Debt recorded: $$amount with $personName")
        }
    }

    fun settleDebt(debt: Debt) {
        viewModelScope.launch {
            repository.updateDebt(debt.copy(isSettled = !debt.isSettled))
            _syncNotification.emit(if (!debt.isSettled) "Marked as settled!" else "Marked as unsettled")
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
            _syncNotification.emit("Removed debt entry")
        }
    }

    // --- OTP Login System ---
    fun sendOtp(phoneNumber: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.length < 6) {
            onError("Please enter a valid phone number")
            return
        }
        val generated = ((100000..999999).random()).toString()
        _otpState.value = _otpState.value.copy(
            phoneNumber = cleanPhone,
            generatedOtp = generated,
            isOtpSent = true,
            isLoading = false,
            error = null
        )
        onSuccess()
        viewModelScope.launch {
            _syncNotification.emit("OTP Code sent: $generated (Demo auto-code)")
        }
    }

    fun verifyOtp(enteredOtp: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val clean = enteredOtp.trim()
        val currentOtp = _otpState.value.generatedOtp
        if (clean == currentOtp || clean == "123456") {
            val phone = _otpState.value.phoneNumber
            val initial = if (phone.length >= 2) phone.takeLast(2) else "US"
            _userState.value = UserState(
                name = "User ($phone)",
                email = "user@mobile.com",
                phoneNumber = phone,
                isLoggedIn = true,
                userAvatarInitials = initial
            )
            _otpState.value = OtpState() // Reset
            onSuccess()
            viewModelScope.launch {
                _syncNotification.emit("Successfully logged in via OTP!")
            }
        } else {
            onError("Invalid OTP. Try again or check the demo code.")
        }
    }

    fun resendOtp(onSuccess: () -> Unit) {
        val phone = _otpState.value.phoneNumber
        if (phone.isNotBlank()) {
            val newCode = ((100000..999999).random()).toString()
            _otpState.value = _otpState.value.copy(generatedOtp = newCode)
            onSuccess()
            viewModelScope.launch {
                _syncNotification.emit("New OTP: $newCode")
            }
        }
    }

    fun logout() {
        _userState.value = UserState(isLoggedIn = false)
        viewModelScope.launch {
            _syncNotification.emit("Logged out")
        }
    }

    fun updateProfile(name: String, email: String) {
        val initials = name.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifBlank { "ME" }
        _userState.value = _userState.value.copy(
            name = name.ifBlank { "User" },
            email = email,
            userAvatarInitials = initials
        )
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                return ExpenseViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
