package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val category: String,
    val paidBy: String = "You",
    val folderId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey
    val id: String,
    val name: String,
    val members: String, // Comma separated list of member names
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val amount: Double,
    val description: String,
    val isSettled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val monthlyLimit: Double,
    val monthYear: String = "" // e.g. "2026-09" or empty for recurring monthly target
)
