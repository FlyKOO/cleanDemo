package com.example.cleanup.utils

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限管理工具类
 */
class PermissionManager(private val context: Context) {
    
    /**
     * 检查存储权限
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 不需要存储权限
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 检查应用使用统计权限
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    /**
     * 检查查询所有包权限
     */
    fun hasQueryAllPackagesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.checkSelfPermission(android.Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 10 以下不需要此权限
        }
    }
    
    /**
     * 请求存储权限
     */
    fun requestStoragePermission(activity: android.app.Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                requestCode
            )
        }
    }
    
    /**
     * 打开应用使用统计权限设置页面
     */
    fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    /**
     * 打开应用权限设置页面
     */
    fun openAppPermissionsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    /**
     * 获取权限状态描述
     */
    fun getPermissionStatusDescription(): String {
        val granted = context.getString(com.example.cleanup.R.string.granted)
        val notGranted = context.getString(com.example.cleanup.R.string.not_granted)
        val storagePermission = context.getString(com.example.cleanup.R.string.storage_permission)
        val usageStatsPermission = context.getString(com.example.cleanup.R.string.usage_stats_permission)
        val queryAllPackagesPermission = context.getString(com.example.cleanup.R.string.query_all_packages_permission)
        
        val storageStatus = if (hasStoragePermission()) "✅ $granted" else "❌ $notGranted"
        val usageStatsStatus = if (hasUsageStatsPermission()) "✅ $granted" else "❌ $notGranted"
        val queryAllStatus = if (hasQueryAllPackagesPermission()) "✅ $granted" else "❌ $notGranted"
        
        return """
            $storagePermission: $storageStatus
            $usageStatsPermission: $usageStatsStatus
            $queryAllPackagesPermission: $queryAllStatus
        """.trimIndent()
    }
    
    /**
     * 检查所有必要权限
     */
    fun hasAllRequiredPermissions(): Boolean {
        return hasStoragePermission() && 
               hasUsageStatsPermission() && 
               hasQueryAllPackagesPermission()
    }
}

