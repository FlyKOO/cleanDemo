package com.example.cleanup.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendOnceWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val success = sendData()
            if (success) Result.success()
            else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun sendData(): Boolean =
        withContext(Dispatchers.IO) {
            EVENT_25()
            true
        }
}
