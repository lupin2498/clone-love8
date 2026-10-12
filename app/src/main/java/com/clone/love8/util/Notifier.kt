package com.clone.love8.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.clone.love8.MainActivity
import com.clone.love8.R
import com.clone.love8.data.AppData
import com.clone.love8.data.SyncEnvelope

object Notifier {
    const val CH_LOVE = "love_messages"
    const val CH_PET = "pet"
    const val CH_DAYS = "special_days"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_LOVE, "Tin nhắn từ người ấy", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_PET, "Thú cưng", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_DAYS, "Ngày đặc biệt", NotificationManager.IMPORTANCE_DEFAULT),
            )
        )
    }

    fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, channel: String, id: Int, title: String, text: String) {
        if (!canNotify(ctx)) return
        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF5C8A.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // Người dùng chưa cấp quyền thông báo.
        }
    }

    /** Thông báo các sự kiện nhận được từ người ấy khi app không mở. */
    fun notifyIncoming(ctx: Context, events: List<SyncEnvelope>, d: AppData) {
        if (!d.settings.notifyMessages) return
        val partner = d.partner.name.ifBlank { "Người ấy" }
        events.forEach { e ->
            val m = e.message
            when {
                m != null -> {
                    val text = when (m.kind) {
                        "miss_you" -> "💗 $partner đang nhớ bạn!"
                        "heart" -> "💖 $partner gửi bạn một trái tim"
                        else -> m.text
                    }
                    show(ctx, CH_LOVE, m.id.hashCode(), partner, text)
                }
                e.pet != null && d.settings.notifyPet -> {
                    val last = e.pet.log.firstOrNull()?.text
                    if (last != null) show(ctx, CH_PET, 2001, "🐾 ${e.pet.name}", last)
                }
                e.specialDay != null && d.settings.notifySpecialDays ->
                    show(ctx, CH_DAYS, e.specialDay.id.hashCode(), "📅 Ngày đặc biệt mới", "$partner đã thêm \"${e.specialDay.title}\"")
                e.diary != null ->
                    show(ctx, CH_LOVE, e.diary.id.hashCode(), "📔 Nhật ký mới", "$partner vừa viết nhật ký: ${e.diary.text.take(80)}")
            }
        }
    }
}
