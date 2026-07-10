package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val personalExpenses: Flow<List<Expense>> = expenseDao.getPersonalExpenses()
    val allFolders: Flow<List<Folder>> = expenseDao.getAllFolders()
    val allDebts: Flow<List<Debt>> = expenseDao.getAllDebts()

    fun getFolderExpenses(folderId: String): Flow<List<Expense>> {
        return expenseDao.getFolderExpenses(folderId)
    }

    suspend fun getFolderById(folderId: String): Folder? {
        return expenseDao.getFolderById(folderId)
    }

    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun insertFolder(folder: Folder) {
        expenseDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: Folder) {
        expenseDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: Folder) {
        expenseDao.deleteFolder(folder)
    }

    suspend fun insertDebt(debt: Debt) {
        expenseDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: Debt) {
        expenseDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) {
        expenseDao.deleteDebt(debt)
    }
}
