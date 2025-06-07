package com.example.project1912.Services

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.*
import com.example.project1912.Utils.DailyRefreshManager

class DailyRefreshWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            println("DailyRefreshWorker: Starting daily refresh work...")
            
            val refreshManager = DailyRefreshManager(applicationContext)
            refreshManager.performDailyRefresh()
            
            println("DailyRefreshWorker: Daily refresh completed successfully")
            Result.success()
        } catch (e: Exception) {
            println("DailyRefreshWorker: Daily refresh failed: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }
}

object DailyRefreshService {
    
    private const val DAILY_REFRESH_WORK_NAME = "DailyRefreshWork"
    
    /**
     * Schedule daily refresh to run every day at midnight (00:00)
     */
    fun scheduleDailyRefresh(context: Context) {
        // Calculate initial delay to midnight
        val currentTime = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If current time is past midnight, schedule for next midnight
            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val initialDelay = midnight.timeInMillis - currentTime.timeInMillis
        
        println("DailyRefreshService: Scheduling daily refresh with initial delay of ${initialDelay / 1000 / 60} minutes")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val dailyRefreshRequest = PeriodicWorkRequestBuilder<DailyRefreshWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyRefreshRequest
        )
        
        println("DailyRefreshService: Daily refresh scheduled successfully")
    }
    
    /**
     * Cancel the daily refresh schedule
     */
    fun cancelDailyRefresh(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_REFRESH_WORK_NAME)
        println("DailyRefreshService: Daily refresh cancelled")
    }
    
    /**
     * Trigger manual refresh (for testing purposes)
     */
    fun triggerManualRefresh(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val manualRefreshRequest = OneTimeWorkRequestBuilder<DailyRefreshWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(manualRefreshRequest)
        println("DailyRefreshService: Manual refresh triggered")
    }
} 