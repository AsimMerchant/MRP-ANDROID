package com.example.mobilereceiptprinter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(receipt: Receipt)

    @Update
    suspend fun update(receipt: Receipt)

    @Delete
    suspend fun delete(receipt: Receipt)

    @Query("SELECT * FROM receipts ORDER BY id DESC")
    suspend fun getAllReceipts(): List<Receipt>

    @Query("SELECT * FROM receipts WHERE biller = :billerName ORDER BY id DESC")
    suspend fun getReceiptsByBiller(billerName: String): List<Receipt>

    @Query("SELECT DISTINCT biller FROM receipts ORDER BY biller")
    suspend fun getAllBillers(): List<String>

    @Query("SELECT DISTINCT volunteer FROM receipts ORDER BY volunteer")
    suspend fun getAllVolunteers(): List<String>

    @Query("DELETE FROM receipts WHERE biller = :billerName")
    suspend fun deleteAllReceiptsFromBiller(billerName: String)

    @Query("DELETE FROM receipts")
    suspend fun deleteAllReceipts()
}
