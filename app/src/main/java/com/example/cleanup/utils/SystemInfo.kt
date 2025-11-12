package com.example.cleanup.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings.*
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

val Context.dataStore by preferencesDataStore("android_id_prefs")

object SystemInfo {
    val keyAndroidId = stringPreferencesKey("android_id_cached")
    var androidID = ""
    /**
     * - 优先使用缓存
     * - 其次尝试 ANDROID_ID
     * - 如果无效则生成 UUID
     */
    @SuppressLint("HardwareIds")
    suspend fun getAndroidID(context : Context): String {
        val prefs = context.dataStore.data.first()
        val cached = prefs[keyAndroidId]
        if (!cached.isNullOrBlank()) return cached
        // 读取系统 ANDROID_ID
        val rawId = Secure.getString(
            context.contentResolver,
            Secure.ANDROID_ID
        )
        val validId = rawId?.takeIf { id ->
            id.isNotBlank() &&
                    id.lowercase() != "9774d56d682e549c" && // 历史 bug 值
                    id.any { it != '0' }                    // 避免全零
        }
        val finalId = validId ?: UUID.randomUUID().toString()
        context.dataStore.edit { it[keyAndroidId] = finalId }
        androidID = finalId
        return finalId
    }

    fun networkType(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nw = cm.activeNetwork ?: return "none"
            val caps = cm.getNetworkCapabilities(nw) ?: return "none"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
                else -> "other"
            }
        } catch (_: Exception) {
            "unknown"
        }
    }

}