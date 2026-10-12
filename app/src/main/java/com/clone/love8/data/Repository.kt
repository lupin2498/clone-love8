package com.clone.love8.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Kho dữ liệu cục bộ: toàn bộ trạng thái app được lưu trong một tệp JSON. */
object Repository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val lock = Any()
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state.asStateFlow()

    private lateinit var file: File
    lateinit var appContext: Context
        private set

    @Volatile
    private var initialized = false

    val data: AppData get() = _state.value

    fun init(context: Context) {
        synchronized(lock) {
            if (initialized) return
            appContext = context.applicationContext
            file = File(appContext.filesDir, "love8_data.json")
            val parsed: AppData? = if (file.exists()) {
                runCatching { json.decodeFromString(AppData.serializer(), file.readText()) }.getOrNull()
            } else {
                null
            }
            var loaded = parsed ?: AppData()
            if (loaded.deviceId.isBlank()) loaded = loaded.copy(deviceId = UUID.randomUUID().toString())
            _state.value = loaded
            initialized = true
            save(loaded)
        }
    }

    fun update(transform: (AppData) -> AppData): AppData {
        synchronized(lock) {
            val updated = transform(_state.value)
            if (updated != _state.value) {
                _state.value = updated
                save(updated)
            }
            return updated
        }
    }

    fun reset() {
        synchronized(lock) {
            val fresh = AppData(deviceId = UUID.randomUUID().toString())
            _state.value = fresh
            save(fresh)
        }
    }

    private fun save(d: AppData) {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(AppData.serializer(), d))
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }
}
