package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amount: Double, // positive means "I lent them money" (they owe me), negative means "I borrowed money" (I owe them)
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false
)
