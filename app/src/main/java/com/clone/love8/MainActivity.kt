package com.clone.love8

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clone.love8.sync.SyncManager
import com.clone.love8.ui.screens.Love8Root
import com.clone.love8.ui.theme.Love8Theme
import com.clone.love8.widget.WidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            Love8Theme {
                Love8Root()
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val ctx = applicationContext
                runCatching { SyncManager.sendStatus(ctx, force = true) }
                var round = 0
                while (isActive) {
                    runCatching { SyncManager.pull(ctx) }
                    round++
                    if (round % 12 == 0) runCatching { SyncManager.sendStatus(ctx) }
                    delay(8_000)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Love8App.foreground = true
    }

    override fun onStop() {
        super.onStop()
        Love8App.foreground = false
        runCatching { WidgetUpdater.updateAll(this) }
    }
}
