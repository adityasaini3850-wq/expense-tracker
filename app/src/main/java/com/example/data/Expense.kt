package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: String?, // null means personal expense, non-null is the trip folder ID
    val amount: Double,
    val category: String, // e.g. Food, Transport, Lodging, Entertainment, Shopping, Other
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paidBy: String = "Me" // Who paid for this expense
)
