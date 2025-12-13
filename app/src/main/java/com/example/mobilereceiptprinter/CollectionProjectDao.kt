package com.example.mobilereceiptprinter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy

@Dao
interface CollectionProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: CollectionProject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<CollectionProject>)

    @Update
    suspend fun update(project: CollectionProject)

    @Delete
    suspend fun delete(project: CollectionProject)

    @Query("SELECT * FROM collection_projects ORDER BY createdDate DESC, createdTime DESC")
    suspend fun getAllProjects(): List<CollectionProject>

    @Query("SELECT * FROM collection_projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): CollectionProject?

    @Query("SELECT * FROM collection_projects WHERE syncStatus = :status")
    suspend fun getProjectsBySyncStatus(status: String): List<CollectionProject>

    @Query("DELETE FROM collection_projects")
    suspend fun deleteAllProjects()

    @Query("SELECT COUNT(*) FROM collection_projects")
    suspend fun getProjectCount(): Int

    // Get project summaries with statistics
    @Query("""
        SELECT 
            cp.id as projectId,
            cp.name as projectName,
            COUNT(cr.id) as receiptCount,
            COALESCE(SUM(CAST(REPLACE(r.amount, ',', '') AS REAL)), 0) as totalAmount,
            cp.createdDate as createdDate,
            cp.createdTime as createdTime
        FROM collection_projects cp
        LEFT JOIN collected_receipts cr ON cp.id = cr.projectId
        LEFT JOIN receipts r ON cr.receiptId = r.id
        GROUP BY cp.id
        ORDER BY cp.createdDate DESC, cp.createdTime DESC
    """)
    suspend fun getProjectSummaries(): List<ProjectSummary>

    // Get receipts collected in a specific project
    @Query("""
        SELECT r.*, cr.collectionDate, cr.collectionTime, cr.collectorName, cr.scannedBy
        FROM receipts r
        INNER JOIN collected_receipts cr ON r.id = cr.receiptId
        WHERE cr.projectId = :projectId
        ORDER BY cr.collectionDate DESC, cr.collectionTime DESC
    """)
    suspend fun getReceiptsForProject(projectId: String): List<CollectedReceiptWithDetails>

    // Get count of receipts in a project
    @Query("""
        SELECT COUNT(*)
        FROM collected_receipts
        WHERE projectId = :projectId
    """)
    suspend fun getReceiptCountForProject(projectId: String): Int

    // Get total amount for a project
    @Query("""
        SELECT COALESCE(SUM(CAST(REPLACE(r.amount, ',', '') AS REAL)), 0)
        FROM receipts r
        INNER JOIN collected_receipts cr ON r.id = cr.receiptId
        WHERE cr.projectId = :projectId
    """)
    suspend fun getTotalAmountForProject(projectId: String): Double

    // Check if a receipt is already collected in a specific project
    @Query("""
        SELECT COUNT(*) > 0
        FROM collected_receipts
        WHERE receiptId = :receiptId AND projectId = :projectId
    """)
    suspend fun isReceiptCollectedInProject(receiptId: String, projectId: String): Boolean

    // Update sync status
    @Query("UPDATE collection_projects SET syncStatus = :status WHERE id = :projectId")
    suspend fun updateSyncStatus(projectId: String, status: String)
}
