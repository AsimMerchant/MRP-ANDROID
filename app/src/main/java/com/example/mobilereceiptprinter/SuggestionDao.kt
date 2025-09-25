package com.example.mobilereceiptprinter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SuggestionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSuggestion(suggestion: Suggestion)

    @Query("SELECT DISTINCT name FROM suggestions WHERE type = 'biller' ORDER BY name")
    suspend fun getAllBillerSuggestions(): List<String>

    @Query("SELECT DISTINCT name FROM suggestions WHERE type = 'volunteer' ORDER BY name")
    suspend fun getAllVolunteerSuggestions(): List<String>

    @Query("DELETE FROM suggestions")
    suspend fun clearAllSuggestions()

    @Query("INSERT OR IGNORE INTO suggestions (type, name) VALUES ('biller', :name)")
    suspend fun addBillerSuggestion(name: String)

    @Query("INSERT OR IGNORE INTO suggestions (type, name) VALUES ('volunteer', :name)")
    suspend fun addVolunteerSuggestion(name: String)
}
