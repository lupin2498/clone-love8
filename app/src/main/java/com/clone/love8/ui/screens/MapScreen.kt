package com.clone.love8.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.clone.love8.data.AppData
import com.clone.love8.data.Repository
import com.clone.love8.sync.SyncManager
import com.clone.love8.ui.components.AvatarView
import com.clone.love8.ui.components.LoveCard
import com.clone.love8.ui.theme.LoveCream
import com.clone.love8.ui.theme.LoveSubText
import com.clone.love8.util.Avatars
import com.clone.love8.util.Dates
import com.clone.love8.util.LocationHelper
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.roundToInt

private val DEFAULT_CENTER = GeoPoint(21.0285, 105.8542)

@Composable
fun MapScreen(data: AppData, showMessage: (String) -> Unit, open: (SubPageId) -> Unit) {
    val ctx = LocalContext.current
    var hasPermission by remember { mutableStateOf(LocationHelper.hasPermission(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPermission = res.values.any { it } || LocationHelper.hasPermission(ctx)
        if (hasPermission) SyncManager.sendStatusAsync(ctx, force = true)
    }

    Column(Modifier.fillMaxSize().background(LoveCream)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            OsmMap(data, hasPermission)
            if (!hasPermission) {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("📍", fontSize = 36.sp)
                    Text("Cho phép truy cập vị trí", fontWeight = FontWeight.Bold)
                    Text(
                        "Để chia sẻ vị trí và xem khoảng cách giữa hai bạn",
                        textAlign = TextAlign.Center,
                        color = LoveSubText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }) { Text("Cho phép") }
                }
            }
        }
        PartnerInfoPanel(data, showMessage, open)
    }
}

@Composable
private fun PartnerInfoPanel(data: AppData, showMessage: (String) -> Unit, open: (SubPageId) -> Unit) {
    val ctx = LocalContext.current
    val ps = data.partnerStatus
    val dist = LocationHelper.distanceBetween(data.myStatus, ps)
    LoveCard(Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarView(false, data.partner.emoji, data.partnerPhotoStamp, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(data.partner.name.ifBlank { "Người ấy" } + (ps?.mood?.let { " $it" } ?: ""), fontWeight = FontWeight.Bold)
                if (!data.isPaired) {
                    Text("Chưa kết nối với người ấy", color = LoveSubText, style = MaterialTheme.typography.bodySmall)
                } else if (ps == null) {
                    Text("Đang chờ người ấy chia sẻ...", color = LoveSubText, style = MaterialTheme.typography.bodySmall)
                } else {
                    val battery = ps.battery?.let { "🔋 $it%" + if (ps.charging) " ⚡" else "" } ?: "🔋 --"
                    val speed = ps.speed?.let { s -> if (s > 0.5f) " · 🚗 ${(s * 3.6f).roundToInt()} km/h" else " · 🧍 đứng yên" } ?: ""
                    Text(battery + speed, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "📍 " + (dist?.let { LocationHelper.formatDistance(it) } ?: "chưa có vị trí") +
                            " · " + Dates.relative(ps.locationTs ?: ps.ts),
                        style = MaterialTheme.typography.bodySmall,
                        color = LoveSubText,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (data.isPaired) {
                FilledTonalButton(onClick = {
                    SyncManager.scope.launch {
                        SyncManager.sendStatus(ctx.applicationContext, force = true)
                        SyncManager.pull(ctx.applicationContext)
                    }
                    showMessage("Đang cập nhật vị trí...")
                }, modifier = Modifier.weight(1f)) { Text("🔄 Làm mới") }
            } else {
                Button(onClick = { open(SubPageId.PAIRING) }, modifier = Modifier.weight(1f)) { Text("🔗 Kết nối") }
            }
            OutlinedButton(onClick = {
                Repository.update { it.copy(settings = it.settings.copy(shareLocation = !it.settings.shareLocation)) }
                showMessage(if (!data.settings.shareLocation) "Đã bật chia sẻ vị trí" else "Đã tắt chia sẻ vị trí")
            }, modifier = Modifier.weight(1f)) {
                Text(if (data.settings.shareLocation) "📍 Đang chia sẻ" else "🙈 Đã ẩn")
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun OsmMap(data: AppData, hasPermission: Boolean) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(13.0)
            controller.setCenter(DEFAULT_CENTER)
        }
    }
    val meMarker = remember { Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) } }
    val partnerMarker = remember { Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) } }
    var centered by remember { mutableStateOf(false) }
    var liveLocation by remember { mutableStateOf<Location?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    DisposableEffect(hasPermission) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                liveLocation = location
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        if (hasPermission && lm != null) {
            LocationHelper.lastKnown(ctx)?.let { liveLocation = it }
            runCatching {
                lm.getProviders(true).forEach { provider ->
                    if (provider != LocationManager.PASSIVE_PROVIDER) {
                        lm.requestLocationUpdates(provider, 5_000L, 5f, listener, Looper.getMainLooper())
                    }
                }
            }
        }
        onDispose {
            runCatching { lm?.removeUpdates(listener) }
        }
    }

    LaunchedEffect(liveLocation) {
        val loc = liveLocation ?: return@LaunchedEffect
        Repository.update {
            it.copy(
                myStatus = it.myStatus.copy(
                    lat = loc.latitude,
                    lon = loc.longitude,
                    accuracy = if (loc.hasAccuracy()) loc.accuracy else null,
                    speed = if (loc.hasSpeed()) loc.speed else null,
                    locationTs = System.currentTimeMillis(),
                )
            )
        }
    }

    val myLat = data.myStatus.lat
    val myLon = data.myStatus.lon
    val ps = data.partnerStatus
    val pLat = ps?.lat
    val pLon = ps?.lon

    LaunchedEffect(myLat, myLon, pLat, pLon, data.mePhotoStamp, data.partnerPhotoStamp, data.me.emoji, data.partner.emoji) {
        val sizePx = (56 * ctx.resources.displayMetrics.density).toInt()
        if (myLat != null && myLon != null) {
            meMarker.position = GeoPoint(myLat, myLon)
            meMarker.title = data.me.name.ifBlank { "Bạn" }
            meMarker.icon = BitmapDrawable(ctx.resources, Avatars.circle(ctx, true, data.me.emoji, sizePx))
            if (!mapView.overlays.contains(meMarker)) mapView.overlays.add(meMarker)
        }
        if (pLat != null && pLon != null) {
            partnerMarker.position = GeoPoint(pLat, pLon)
            partnerMarker.title = data.partner.name.ifBlank { "Người ấy" }
            partnerMarker.icon = BitmapDrawable(ctx.resources, Avatars.circle(ctx, false, data.partner.emoji, sizePx))
            if (!mapView.overlays.contains(partnerMarker)) mapView.overlays.add(partnerMarker)
        }
        if (!centered) {
            val target = when {
                myLat != null && myLon != null -> GeoPoint(myLat, myLon)
                pLat != null && pLon != null -> GeoPoint(pLat, pLon)
                else -> null
            }
            if (target != null) {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(target)
                centered = true
            }
        }
        mapView.invalidate()
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.TopEnd).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = {
                if (myLat != null && myLon != null) mapView.controller.animateTo(GeoPoint(myLat, myLon))
            }) { Text("🙋 Tôi") }
            FilledTonalButton(onClick = {
                if (pLat != null && pLon != null) mapView.controller.animateTo(GeoPoint(pLat, pLon))
            }) { Text("💖 Người ấy") }
        }
    }
}
