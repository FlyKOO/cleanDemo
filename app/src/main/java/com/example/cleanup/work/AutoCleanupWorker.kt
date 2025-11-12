package com.example.cleanup.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cleanup.data.AutoCleanupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 周期性执行自动清理任务的Worker。
 *
 * Worker会检查自动清理条件，只有在满足时才会真正触发清理，避免无谓的资源消耗。
 */
class AutoCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val autoCleanupManager = AutoCleanupManager(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val shouldCleanup = autoCleanupManager.shouldPerformAutoCleanup()
            if (shouldCleanup) {
                val result = autoCleanupManager.performAutoCleanup()
                if (result.success) Result.success() else Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

