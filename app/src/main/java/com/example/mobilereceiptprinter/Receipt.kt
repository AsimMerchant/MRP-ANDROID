package com.example.mobilereceiptprinter

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val receiptNumber: Int,
    val biller: String,
    val volunteer: String,
    val amount: String,
    val date: String,
    val time: String
)

@Entity(tableName = "suggestions")
data class Suggestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "biller" or "volunteer"
    val name: String
)
