package com.example.mobilereceiptprinter

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Lightweight HTTP server for sharing receipts/reports between devices
 * Runs on each device to serve its reports via REST API
 */
class ReportServer(
    private val context: Context,
    private val database: AppDatabase,
    port: Int = 0 // 0 = auto-assign available port
) : NanoHTTPD(port) {
    
    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        private const val TAG = "ReportServer"
    }
    
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        
        Log.d(TAG, "📨 HTTP Request: $method $uri from ${session.remoteIpAddress}")
        
        return try {
            when {
                method == Method.GET && uri == "/reports" -> {
                    Log.d(TAG, "📊 Serving all reports")
                    serveAllReports()
                }
                method == Method.GET && uri.startsWith("/reports/since/") -> {
                    val afterId = uri.substringAfterLast("/").toIntOrNull()
                    if (afterId != null) {
                        Log.d(TAG, "📊 Serving reports since ID: $afterId")
                        serveReportsAfter(afterId)
                    } else {
                        Log.w(TAG, "❌ Invalid afterId in request: $uri")
                        newFixedLengthResponse(
                            Response.Status.BAD_REQUEST, 
                            "application/json", 
                            """{"error":"Invalid afterId parameter"}"""
                        )
                    }
                }
                method == Method.GET && uri == "/health" -> {
                    Log.d(TAG, "💚 Health check")
                    newFixedLengthResponse(
                        Response.Status.OK, 
                        "application/json", 
                        """{"status":"healthy","server":"MRP-ReportServer"}"""
                    )
                }
                else -> {
                    Log.w(TAG, "❌ Unknown endpoint: $method $uri")
                    newFixedLengthResponse(
                        Response.Status.NOT_FOUND, 
                        "application/json", 
                        """{"error":"Endpoint not found"}"""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Server error handling request: $method $uri", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, 
                "application/json", 
                """{"error":"Internal server error: ${e.message}"}"""
            )
        }
    }
    
    private fun serveAllReports(): Response {
        return try {
            // Use runBlocking to handle the suspend function synchronously
            // This is acceptable for HTTP server responses where we need to block anyway
            val reports = kotlinx.coroutines.runBlocking {
                database.receiptDao().getAllReceipts()
            }
            
            Log.d(TAG, "📊 Serving ${reports.size} reports")
            
            val jsonResponse = gson.toJson(mapOf(
                "success" to true,
                "reports" to reports,
                "total" to reports.size,
                "timestamp" to System.currentTimeMillis(),
                "deviceInfo" to mapOf(
                    "model" to android.os.Build.MODEL,
                    "device" to android.os.Build.DEVICE
                )
            ))
            
            newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error serving all reports", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, 
                "application/json", 
                """{"error":"Failed to fetch reports: ${e.message}"}"""
            )
        }
    }
    
    private fun serveReportsAfter(afterId: Int): Response {
        return try {
            // Use runBlocking for incremental sync
            val reports = kotlinx.coroutines.runBlocking {
                database.receiptDao().getReceiptsAfterId(afterId)
            }
            
            Log.d(TAG, "📊 Serving ${reports.size} reports after ID: $afterId")
            
            val jsonResponse = gson.toJson(mapOf(
                "success" to true,
                "reports" to reports,
                "total" to reports.size,
                "afterId" to afterId,
                "timestamp" to System.currentTimeMillis(),
                "deviceInfo" to mapOf(
                    "model" to android.os.Build.MODEL,
                    "device" to android.os.Build.DEVICE
                )
            ))
            
            newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error serving reports after ID: $afterId", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, 
                "application/json", 
                """{"error":"Failed to fetch reports after ID $afterId: ${e.message}"}"""
            )
        }
    }
    
    /**
     * Start the HTTP server
     */
    fun startServer(): Int {
        return try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            val actualPort = listeningPort
            Log.d(TAG, "🚀 ReportServer started on port $actualPort")
            actualPort
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to start ReportServer", e)
            throw e
        }
    }
    
    /**
     * Stop the HTTP server
     */
    fun stopServer() {
        try {
            stop()
            Log.d(TAG, "🛑 ReportServer stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping ReportServer", e)
        }
    }
    
    /**
     * Get server info for logging/debugging
     */
    fun getServerInfo(): String {
        return if (isAlive) {
            "ReportServer running on port $listeningPort"
        } else {
            "ReportServer not running"
        }
    }
}