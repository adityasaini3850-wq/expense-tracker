package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    // Expenses
    val personalExpenses: Flow<List<Expense>> = expenseDao.getPersonalExpenses()
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    fun getFolderExpenses(folderId: String): Flow<List<Expense>> = expenseDao.getFolderExpenses(folderId)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    // Folders
    val allFolders: Flow<List<Folder>> = expenseDao.getAllFolders()
    suspend fun getFolderById(id: String): Folder? = expenseDao.getFolderById(id)
    suspend fun insertFolder(folder: Folder) = expenseDao.insertFolder(folder)
    suspend fun updateFolder(folder: Folder) = expenseDao.updateFolder(folder)
    suspend fun deleteFolder(folder: Folder) = expenseDao.deleteFolder(folder)

    // Debts
    val allDebts: Flow<List<Debt>> = expenseDao.getAllDebts()
    suspend fun insertDebt(debt: Debt): Long = expenseDao.insertDebt(debt)
    suspend fun updateDebt(debt: Debt) = expenseDao.updateDebt(debt)
    suspend fun deleteDebt(debt: Debt) = expenseDao.deleteDebt(debt)

    // Budgets
    val allBudgets: Flow<List<CategoryBudget>> = expenseDao.getAllBudgets()
    fun getBudgetsForMonth(monthYear: String): Flow<List<CategoryBudget>> = expenseDao.getBudgetsForMonth(monthYear)
    suspend fun insertBudget(budget: CategoryBudget): Long = expenseDao.insertBudget(budget)
    suspend fun updateBudget(budget: CategoryBudget) = expenseDao.updateBudget(budget)
    suspend fun deleteBudgetByCategory(category: String, monthYear: String) =
        expenseDao.deleteBudgetByCategory(category, monthYear)
    suspend fun deleteBudget(budget: CategoryBudget) = expenseDao.deleteBudget(budget)
}
