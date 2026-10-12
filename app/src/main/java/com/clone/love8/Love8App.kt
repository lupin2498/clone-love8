package com.clone.love8

import android.app.Application
import com.clone.love8.data.Repository
import com.clone.love8.util.Notifier
import com.clone.love8.work.SyncWorker
import org.osmdroid.config.Configuration
import java.io.File

class Love8App : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        Notifier.createChannels(this)
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
        SyncWorker.schedule(this)
    }

    companion object {
        @Volatile
        var foreground: Boolean = false
    }
}
