package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String, // Access code, e.g. "TRIP-X42B"
    val name: String,
    val members: String, // Semi-colon separated list of member names, e.g. "Me;Alice;Bob;Charlie"
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    fun getMemberList(): List<String> {
        if (members.isBlank()) return listOf("Me")
        return members.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
