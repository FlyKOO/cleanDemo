package com.example.cleanup.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * 用于管理定时清理任务的存储与读取
 */
class ScheduledCleanupRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "scheduled_cleanups"
        private const val KEY_TASKS = "tasks"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val lock = Any()

    fun getScheduledCleanups(): List<ScheduledCleanup> {
        val json = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(item.toScheduledCleanup())
                }
            }
        }.getOrElse { emptyList() }
    }

    fun addScheduledCleanup(task: ScheduledCleanup): List<ScheduledCleanup> = synchronized(lock) {
        val updated = getScheduledCleanups() + task
        saveScheduledCleanups(updated)
        updated
    }

    fun updateScheduledCleanup(task: ScheduledCleanup): List<ScheduledCleanup> = synchronized(lock) {
        val updated = getScheduledCleanups().map { existing ->
            if (existing.id == task.id) task else existing
        }
        saveScheduledCleanups(updated)
        updated
    }

    fun deleteScheduledCleanup(id: String): List<ScheduledCleanup> = synchronized(lock) {
        val updated = getScheduledCleanups().filterNot { it.id == id }
        saveScheduledCleanups(updated)
        updated
    }

    fun toggleScheduledCleanup(id: String): List<ScheduledCleanup> = synchronized(lock) {
        val updated = getScheduledCleanups().map { existing ->
            if (existing.id == id) {
                existing.copy(isEnabled = !existing.isEnabled)
            } else {
                existing
            }
        }
        saveScheduledCleanups(updated)
        updated
    }

    private fun saveScheduledCleanups(tasks: List<ScheduledCleanup>) {
        val array = JSONArray().apply {
            tasks.forEach { task ->
                put(task.toJson())
            }
        }
        prefs.edit { putString(KEY_TASKS, array.toString()) }
    }

    private fun JSONObject.toScheduledCleanup(): ScheduledCleanup {
        val typesArray = optJSONArray("cleanupTypes") ?: JSONArray()
        val types = buildList {
            for (index in 0 until typesArray.length()) {
                val typeName = typesArray.optString(index)
                ScheduledCleanupType.values().firstOrNull { it.name == typeName }?.let { add(it) }
            }
        }
        val lastRunValue = if (isNull("lastRun")) null else optLong("lastRun")
        val frequencyName = optString("frequency", CleanupFrequency.DAILY.name)
        val frequency = runCatching { CleanupFrequency.valueOf(frequencyName) }
            .getOrElse { CleanupFrequency.DAILY }
        val id = optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val name = optString("name")
        return ScheduledCleanup(
            id = id,
            name = name,
            isEnabled = optBoolean("isEnabled", true),
            frequency = frequency,
            time = optString("time", "00:00"),
            cleanupTypes = types,
            lastRun = lastRunValue,
            nextRun = optLong("nextRun", System.currentTimeMillis())
        )
    }

    private fun ScheduledCleanup.toJson(): JSONObject {
        val typesArray = JSONArray().apply {
            cleanupTypes.forEach { type ->
                put(type.name)
            }
        }
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("isEnabled", isEnabled)
            put("frequency", frequency.name)
            put("time", time)
            put("cleanupTypes", typesArray)
            if (lastRun == null) {
                put("lastRun", JSONObject.NULL)
            } else {
                put("lastRun", lastRun)
            }
            put("nextRun", nextRun)
        }
    }
}

enum class CleanupFrequency {
    DAILY, WEEKLY, MONTHLY
}

enum class ScheduledCleanupType {
    CACHE, JUNK, DUPLICATE, LARGE_FILES
}

data class ScheduledCleanup(
    val id: String,
    val name: String,
    val isEnabled: Boolean,
    val frequency: CleanupFrequency,
    val time: String,
    val cleanupTypes: List<ScheduledCleanupType>,
    val lastRun: Long?,
    val nextRun: Long
)
