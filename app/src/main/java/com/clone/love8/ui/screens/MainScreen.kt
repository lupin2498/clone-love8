package com.clone.love8.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clone.love8.data.AppData
import com.clone.love8.data.Repository
import com.clone.love8.sync.SyncManager
import com.clone.love8.ui.theme.LoveCream
import com.clone.love8.ui.theme.LovePink
import com.clone.love8.ui.theme.LovePinkLight
import kotlinx.coroutines.launch

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Tình yêu", Icons.Filled.Favorite),
    MAP("Vị trí", Icons.Filled.LocationOn),
    PET("Thú cưng", Icons.Filled.Face),
    DAYS("Ngày lễ", Icons.Filled.DateRange),
    MORE("Thêm", Icons.Filled.Menu),
}

enum class SubPageId { DIARY, PAIRING, SETTINGS, PREMIUM, ABOUT }

@Composable
fun Love8Root() {
    val data by Repository.state.collectAsStateWithLifecycle()
    if (!data.onboarded) {
        OnboardingScreen()
    } else {
        MainScreen(data)
    }
}

@Composable
fun MainScreen(data: AppData) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var sub by rememberSaveable { mutableStateOf<SubPageId?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    val open: (SubPageId) -> Unit = { sub = it }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        SyncManager.incoming.collect { e ->
            val partner = Repository.data.partner.name.ifBlank { "Người ấy" }
            val m = e.message
            val text = when {
                m != null && m.kind == "miss_you" -> "💗 $partner đang nhớ bạn!"
                m != null && m.kind == "heart" -> "💖 $partner gửi bạn một trái tim"
                m != null -> "💬 $partner: ${m.text}"
                e.pet != null -> e.pet.log.firstOrNull()?.text
                e.specialDay != null -> "📅 $partner đã thêm \"${e.specialDay.title}\""
                e.diary != null -> "📔 $partner vừa viết nhật ký mới"
                else -> null
            }
            if (text != null) snackbar.showSnackbar(text)
        }
    }

    val current = sub
    if (current != null) {
        BackHandler { sub = null }
        val back = { sub = null }
        when (current) {
            SubPageId.DIARY -> DiaryScreen(data, back)
            SubPageId.PAIRING -> PairingScreen(data, back)
            SubPageId.SETTINGS -> SettingsScreen(data, back)
            SubPageId.PREMIUM -> PremiumScreen(back)
            SubPageId.ABOUT -> AboutScreen(back)
        }
        return
    }

    BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }

    Scaffold(
        containerColor = LoveCream,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LovePink,
                            selectedTextColor = LovePink,
                            indicatorColor = LovePinkLight,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).consumeWindowInsets(inner).imePadding()) {
            when (tab) {
                Tab.HOME -> HomeScreen(data, showMessage, open)
                Tab.MAP -> MapScreen(data, showMessage, open)
                Tab.PET -> PetScreen(data, showMessage)
                Tab.DAYS -> SpecialDaysScreen(data, showMessage)
                Tab.MORE -> MoreScreen(data, open)
            }
        }
    }
}
