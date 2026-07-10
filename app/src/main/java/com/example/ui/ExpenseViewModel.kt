package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.min

data class Transaction(
    val from: String,
    val to: String,
    val amount: Double
)

data class FolderSummary(
    val folder: Folder,
    val totalExpenses: Double,
    val myShare: Double,
    val myBalance: Double, // how much "Me" owes or is owed
    val expenseCount: Int
)

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // --- State Flows ---
    val personalExpenses: StateFlow<List<Expense>> = repository.personalExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<Debt>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected group/trip folder
    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

    // Expenses of the selected folder
    val selectedFolderExpenses: StateFlow<List<Expense>> = _selectedFolder
        .flatMapLatest { folder ->
            if (folder == null) flowOf(emptyList())
            else repository.getFolderExpenses(folder.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notification channel for simulated sync activities
    private val _syncNotification = MutableSharedFlow<String>()
    val syncNotification: SharedFlow<String> = _syncNotification.asSharedFlow()

    // --- Add/Edit Temporary State Helper Values ---
    val categories = listOf("Food", "Transport", "Lodging", "Entertainment", "Shopping", "Bills", "Health", "Other")

    // --- Actions ---

    // 1. Personal Expense Actions
    fun addPersonalExpense(amount: Double, category: String, description: String, dateLong: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val expense = Expense(
                folderId = null,
                amount = amount,
                category = category,
                description = description,
                timestamp = dateLong,
                paidBy = "Me"
            )
            repository.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // 2. Folder Actions (Multiuser Groups)
    fun selectFolder(folder: Folder?) {
        _selectedFolder.value = folder
    }

    fun createFolder(name: String, membersList: List<String>): String {
        // Generate a clean 6-digit uppercase code
        val randomCode = "TRIP-" + (100000..999999).random().toString()
        val formattedMembers = (listOf("Me") + membersList.filter { it.isNotBlank() && it != "Me" })
            .distinct()
            .joinToString(";")

        viewModelScope.launch {
            val folder = Folder(
                id = randomCode,
                name = name,
                members = formattedMembers,
                createdTimestamp = System.currentTimeMillis()
            )
            repository.insertFolder(folder)
            
            // Add initial mock expenses for some folders to show split immediately
            if (membersList.isNotEmpty()) {
                addFolderExpense(randomCode, 120.0, "Lodging", "Hotel Booking Deposit", "Me")
                val friends = membersList.filter { it.isNotBlank() && it != "Me" }
                if (friends.isNotEmpty()) {
                    addFolderExpense(randomCode, 45.0, "Food", "Welcome Drinks", friends.random())
                }
            }
        }
        return randomCode
    }

    fun joinFolderWithCode(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uppercaseCode = code.uppercase().trim()
        viewModelScope.launch {
            val existing = repository.getFolderById(uppercaseCode)
            if (existing != null) {
                // Ensure "Me" is in members, if not add it
                val members = existing.getMemberList()
                if (!members.contains("Me")) {
                    val updatedMembers = (members + "Me").joinToString(";")
                    repository.updateFolder(existing.copy(members = updatedMembers))
                }
                _selectedFolder.value = existing
                onSuccess()
            } else {
                // If code not found in db, let's create a realistic mock folder representing the joined one!
                // This makes any user entered code immediately work with mock cloud synchronization!
                val mockNames = listOf("Europe Getaway", "Weekend Cabin Trip", "Road Trip 2026", "Birthday Bash", "Group Dinner")
                val mockName = mockNames.random()
                val mockMembers = listOf("Me", "Alice", "Bob", "Charlie")
                val folder = Folder(
                    id = uppercaseCode,
                    name = mockName,
                    members = mockMembers.joinToString(";"),
                    createdTimestamp = System.currentTimeMillis()
                )
                repository.insertFolder(folder)
                
                // Add initial expenses for this folder to look like dynamic remote sync
                repository.insertExpense(Expense(folderId = uppercaseCode, amount = 12000.0, category = "Lodging", description = "Cabin Rent", paidBy = "Alice", timestamp = System.currentTimeMillis() - 86400000))
                repository.insertExpense(Expense(folderId = uppercaseCode, amount = 4800.0, category = "Food", description = "Groceries & BBQ", paidBy = "Bob", timestamp = System.currentTimeMillis() - 36000000))
                repository.insertExpense(Expense(folderId = uppercaseCode, amount = 2800.0, category = "Transport", description = "Fuel Refill", paidBy = "Me", timestamp = System.currentTimeMillis() - 18000000))

                _selectedFolder.value = folder
                _syncNotification.emit("Successfully synced and joined folder $uppercaseCode!")
                onSuccess()
            }
        }
    }

    fun addFolderExpense(folderId: String, amount: Double, category: String, description: String, paidBy: String) {
        viewModelScope.launch {
            val expense = Expense(
                folderId = folderId,
                amount = amount,
                category = category,
                description = description,
                paidBy = paidBy,
                timestamp = System.currentTimeMillis()
            )
            repository.insertExpense(expense)
        }
    }

    fun getFolderExpenses(folderId: String): Flow<List<Expense>> {
        return repository.getFolderExpenses(folderId)
    }

    // 3. Debt Tracking Actions (Money Lent / Borrowed)
    fun addDebt(personName: String, amount: Double, description: String, isLent: Boolean) {
        viewModelScope.launch {
            // positive value = I lent them (they owe me)
            // negative value = I borrowed (I owe them)
            val finalAmount = if (isLent) abs(amount) else -abs(amount)
            val debt = Debt(
                personName = personName,
                amount = finalAmount,
                description = description,
                timestamp = System.currentTimeMillis(),
                isSettled = false
            )
            repository.insertDebt(debt)
        }
    }

    fun settleDebt(debt: Debt) {
        viewModelScope.launch {
            repository.updateDebt(debt.copy(isSettled = true))
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    // --- Multi-User Simulated Sync Engine ---
    fun simulateFriendActivity() {
        val folder = _selectedFolder.value ?: return
        val membersList = folder.getMemberList()
        val friends = membersList.filter { it != "Me" }
        if (friends.isEmpty()) return

        val randomFriend = friends.random()
        val expenseOptions = listOf(
            Triple(15.0 + (10..50).random() * 1.5, "Food", listOf("Pizzas", "Dinner Out", "Boba Tea", "Morning Coffees").random()),
            Triple(20.0 + (5..30).random() * 2.0, "Transport", listOf("Taxi Fare", "Fuel Refill", "Train tickets").random()),
            Triple(80.0 + (10..100).random() * 3.0, "Lodging", listOf("Extra Room Charge", "Hotel Breakfast Combo", "Airbnb upgrade").random()),
            Triple(10.0 + (5..40).random() * 1.0, "Entertainment", listOf("Cinema Tickets", "Museum Entry", "Theme park snack").random()),
            Triple(25.0 + (5..50).random() * 1.5, "Shopping", listOf("Gifts", "Postcards & Stamps", "Local Souvenirs").random())
        )

        val selectedOption = expenseOptions.random()
        val amount = selectedOption.first
        val category = selectedOption.second
        val description = selectedOption.third

        viewModelScope.launch {
            addFolderExpense(
                folderId = folder.id,
                amount = amount,
                category = category,
                description = description,
                paidBy = randomFriend
            )
            _syncNotification.emit("$randomFriend added '$description' for $${String.format(Locale.US, "%.2f", amount)}")
        }
    }

    // Simulate complete folders sync (simulates downloading changes from multiple people)
    fun simulateCloudSync() {
        viewModelScope.launch {
            _syncNotification.emit("Syncing all folders with secure server...")
            kotlinx.coroutines.delay(1000)
            
            // Randomly update expenses or add simulated data
            val folders = repository.allFolders.firstOrNull() ?: emptyList()
            if (folders.isNotEmpty()) {
                val randomFolder = folders.random()
                val membersList = randomFolder.getMemberList()
                val friends = membersList.filter { it != "Me" }
                if (friends.isNotEmpty()) {
                    val randomFriend = friends.random()
                    addFolderExpense(
                        folderId = randomFolder.id,
                        amount = (10..100).random().toDouble(),
                        category = listOf("Food", "Transport", "Entertainment").random(),
                        description = listOf("Uber Ride", "Snack run", "Activity Tickets").random(),
                        paidBy = randomFriend
                    )
                    _syncNotification.emit("Synced! New expenses downloaded in '${randomFolder.name}'")
                } else {
                    _syncNotification.emit("Cloud Synced! Everything is up-to-date.")
                }
            } else {
                _syncNotification.emit("Cloud Synced! Everything is up-to-date.")
            }
        }
    }

    // --- Analytical Calculations ---

    // Get current month name (e.g. "July 2026")
    fun getCurrentMonthYear(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        return sdf.format(Date())
    }

    // Calculates breakdown for Reports Tab
    fun getCategoryBreakdown(expenses: List<Expense>): Map<String, Double> {
        val breakdown = mutableMapOf<String, Double>()
        categories.forEach { breakdown[it] = 0.0 }
        
        expenses.forEach { expense ->
            val cat = if (categories.contains(expense.category)) expense.category else "Other"
            breakdown[cat] = (breakdown[cat] ?: 0.0) + expense.amount
        }
        return breakdown.filterValues { it > 0.0 }
    }

    // Calculate Settlements for a specific group/trip
    fun calculateFolderSettlements(members: List<String>, expenses: List<Expense>): List<Transaction> {
        if (members.isEmpty() || expenses.isEmpty()) return emptyList()

        // 1. Calculate total paid by each member
        val paidMap = members.associateWith { 0.0 }.toMutableMap()
        for (expense in expenses) {
            val payer = expense.paidBy
            if (paidMap.containsKey(payer)) {
                paidMap[payer] = paidMap.getValue(payer) + expense.amount
            } else {
                // If paidBy is not in members (though it should be), register it dynamically
                paidMap[payer] = expense.amount
            }
        }

        // 2. Calculate average share
        val totalSpent = expenses.sumOf { it.amount }
        val avgShare = totalSpent / members.size

        // 3. Calculate balance for each member (paid - share)
        val balances = members.map { member ->
            member to (paidMap.getOrDefault(member, 0.0) - avgShare)
        }.toMutableList()

        // 4. Match debtors and creditors greedily to minimize transactions
        val transactions = mutableListOf<Transaction>()

        while (true) {
            // Sort balances: debtors (negative) first, creditors (positive) last
            balances.sortBy { it.second }

            val maxDebtor = balances.firstOrNull() ?: break
            val maxCreditor = balances.lastOrNull() ?: break

            val debtorVal = maxDebtor.second
            val creditorVal = maxCreditor.second

            // Stop if balances are already fully settled (within sub-cent accuracy)
            if (abs(debtorVal) < 0.01 || abs(creditorVal) < 0.01) {
                break
            }

            val settleAmount = min(abs(debtorVal), creditorVal)

            transactions.add(
                Transaction(
                    from = maxDebtor.first,
                    to = maxCreditor.first,
                    amount = settleAmount
                )
            )

            // Update balances in list
            balances[0] = maxDebtor.first to (debtorVal + settleAmount)
            balances[balances.size - 1] = maxCreditor.first to (creditorVal - settleAmount)
        }

        return transactions
    }

    // Helper class to instantiate the ViewModel with a factory
    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
