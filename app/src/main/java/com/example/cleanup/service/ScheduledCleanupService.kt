package com.example.cleanup.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.cleanup.data.NotificationManager
import com.example.cleanup.data.SettingsManager
import kotlinx.coroutines.*

class ScheduledCleanupService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private lateinit var settingsManager: SettingsManager
    
    companion object {
        private const val TAG = "ScheduledCleanupService"
    }
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManager(this)
        settingsManager = SettingsManager(this)
        Log.d(TAG, "ScheduledCleanupService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ScheduledCleanupService started")
        
        // 模拟定时清理任务执行
        serviceScope.launch {
            try {
                // 检查通知是否启用
                if (settingsManager.isNotificationsEnabled()) {
                    // 发送定时清理提醒通知
                    notificationManager.sendScheduledCleanupNotification(
                        "每日自动清理",
                        "02:00"
                    )
                    
                    // 模拟清理过程
                    delay(2000) // 等待2秒模拟清理过程
                    
                    // 发送清理完成通知
                    notificationManager.sendCleanupCompleteNotification(
                        1024 * 1024 * 50, // 50MB
                        "定时清理"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in scheduled cleanup", e)
            } finally {
                // 服务执行完成后自动停止
                stopSelf(startId)
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "ScheduledCleanupService destroyed")
    }
}





