package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE folderId IS NULL OR folderId = '' ORDER BY timestamp DESC")
    fun getPersonalExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE folderId = :folderId ORDER BY timestamp DESC")
    fun getFolderExpenses(folderId: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // --- Folders ---
    @Query("SELECT * FROM folders ORDER BY createdTimestamp DESC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    // --- Debts ---
    @Query("SELECT * FROM debts ORDER BY isSettled ASC, timestamp DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    // --- Budgets ---
    @Query("SELECT * FROM category_budgets")
    fun getAllBudgets(): Flow<List<CategoryBudget>>

    @Query("SELECT * FROM category_budgets WHERE monthYear = :monthYear OR monthYear = ''")
    fun getBudgetsForMonth(monthYear: String): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget): Long

    @Update
    suspend fun updateBudget(budget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE category = :category AND (monthYear = :monthYear OR monthYear = '')")
    suspend fun deleteBudgetByCategory(category: String, monthYear: String)

    @Delete
    suspend fun deleteBudget(budget: CategoryBudget)
}
