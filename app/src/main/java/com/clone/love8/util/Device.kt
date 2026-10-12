package com.clone.love8.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.clone.love8.data.DeviceStatus
import kotlin.math.roundToInt

object BatteryHelper {
    data class Battery(val level: Int, val charging: Boolean)

    fun read(ctx: Context): Battery {
        val bm = ctx.getSystemService(BatteryManager::class.java)
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val charging = bm?.isCharging ?: false
        return Battery(level, charging)
    }
}

object LocationHelper {
    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasBackgroundPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun lastKnown(ctx: Context): Location? {
        if (!hasPermission(ctx)) return null
        val lm = ctx.getSystemService(LocationManager::class.java) ?: return null
        return try {
            lm.getProviders(true).mapNotNull { provider -> lm.getLastKnownLocation(provider) }.maxByOrNull { it.time }
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun distanceMeters(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Float {
        val out = FloatArray(1)
        Location.distanceBetween(aLat, aLon, bLat, bLon, out)
        return out[0]
    }

    fun distanceBetween(a: DeviceStatus?, b: DeviceStatus?): Float? {
        val sa = a ?: return null
        val sb = b ?: return null
        val aLat = sa.lat ?: return null
        val aLon = sa.lon ?: return null
        val bLat = sb.lat ?: return null
        val bLon = sb.lon ?: return null
        return distanceMeters(aLat, aLon, bLat, bLon)
    }

    fun formatDistance(m: Float): String = when {
        m < 50f -> "Đang ở cạnh nhau 🥰"
        m < 1000f -> "${m.roundToInt()} m"
        m < 100_000f -> String.format("%.1f km", m / 1000f)
        else -> "${(m / 1000f).roundToInt()} km"
    }
}

object StatusCollector {
    /** Thu thập trạng thái hiện tại của máy (pin, vị trí) để gửi cho người ấy. */
    fun collect(ctx: Context, previous: DeviceStatus, shareBattery: Boolean, shareLocation: Boolean): DeviceStatus {
        val now = System.currentTimeMillis()
        val battery = if (shareBattery) BatteryHelper.read(ctx) else null
        var lat: Double? = null
        var lon: Double? = null
        var acc: Float? = null
        var speed: Float? = null
        var locTs: Long? = null
        if (shareLocation) {
            val loc = LocationHelper.lastKnown(ctx)
            val prevTs = previous.locationTs ?: 0L
            if (loc != null && loc.time >= prevTs) {
                lat = loc.latitude
                lon = loc.longitude
                acc = if (loc.hasAccuracy()) loc.accuracy else null
                speed = if (loc.hasSpeed()) loc.speed else null
                locTs = loc.time
            } else if (previous.lat != null) {
                lat = previous.lat
                lon = previous.lon
                acc = previous.accuracy
                speed = previous.speed
                locTs = previous.locationTs
            }
        }
        return DeviceStatus(
            battery = battery?.level?.takeIf { it >= 0 },
            charging = battery?.charging ?: false,
            lat = lat,
            lon = lon,
            accuracy = acc,
            speed = speed,
            locationTs = locTs,
            mood = previous.mood,
            ts = now,
        )
    }
}
