package com.clone.love8.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.clone.love8.MainActivity
import com.clone.love8.R
import com.clone.love8.data.Repository
import com.clone.love8.util.Avatars
import com.clone.love8.util.Dates
import com.clone.love8.util.LocationHelper
import com.clone.love8.util.PetLogic
import java.time.LocalDate

private fun openAppIntent(ctx: Context): PendingIntent {
    val intent = Intent(ctx, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return PendingIntent.getActivity(ctx, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}

class DaysWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            Repository.init(ctx)
            val d = Repository.data
            val start = Dates.parse(d.startDate) ?: LocalDate.now()
            val days = Dates.daysTogether(start)
            val me = Avatars.circle(ctx, true, d.me.emoji, 128)
            val partner = Avatars.circle(ctx, false, d.partner.emoji, 128)
            for (id in ids) {
                val v = RemoteViews(ctx.packageName, R.layout.widget_days)
                v.setTextViewText(R.id.w_days, days.coerceAtLeast(0).toString())
                v.setTextViewText(R.id.w_names, "${d.me.name.ifBlank { "Bạn" }} ♥ ${d.partner.name.ifBlank { "Người ấy" }}")
                v.setImageViewBitmap(R.id.w_avatar_me, me)
                v.setImageViewBitmap(R.id.w_avatar_partner, partner)
                v.setOnClickPendingIntent(R.id.w_root, openAppIntent(ctx))
                mgr.updateAppWidget(id, v)
            }
        }
    }
}

class PetWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            Repository.init(ctx)
            val pet = PetLogic.tick(Repository.data.pet)
            for (id in ids) {
                val v = RemoteViews(ctx.packageName, R.layout.widget_pet)
                if (pet.adopted) {
                    val acc = PetLogic.accessoryEmoji(pet.accessory)
                    v.setTextViewText(R.id.w_pet, (acc ?: "") + PetLogic.emoji(pet.type))
                    v.setTextViewText(R.id.w_pet_name, "${pet.name} · Lv.${pet.level}")
                    v.setTextViewText(R.id.w_pet_mood, PetLogic.mood(pet))
                } else {
                    v.setTextViewText(R.id.w_pet, "🥚")
                    v.setTextViewText(R.id.w_pet_name, "Chưa có thú cưng")
                    v.setTextViewText(R.id.w_pet_mood, "Mở app để nhận nuôi nhé!")
                }
                v.setProgressBar(R.id.w_pet_hunger, 100, pet.hunger.toInt(), false)
                v.setProgressBar(R.id.w_pet_happy, 100, pet.happiness.toInt(), false)
                v.setOnClickPendingIntent(R.id.w_root, openAppIntent(ctx))
                mgr.updateAppWidget(id, v)
            }
        }
    }
}

class PartnerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            Repository.init(ctx)
            val d = Repository.data
            val st = d.partnerStatus
            val avatar = Avatars.circle(ctx, false, d.partner.emoji, 128)
            val level = st?.battery
            val charging = st?.charging == true
            val battery = when {
                !d.isPaired -> "Chưa kết nối"
                level == null -> "🔋 --"
                charging -> "🔋 $level% ⚡"
                else -> "🔋 $level%"
            }
            val dist = LocationHelper.distanceBetween(d.myStatus, st)
            val distance = if (dist != null) "📍 ${LocationHelper.formatDistance(dist)}" else "📍 --"
            val mood = st?.mood?.let { " $it" } ?: ""
            for (id in ids) {
                val v = RemoteViews(ctx.packageName, R.layout.widget_partner)
                v.setImageViewBitmap(R.id.w_avatar, avatar)
                v.setTextViewText(R.id.w_name, d.partner.name.ifBlank { "Người ấy" } + mood)
                v.setTextViewText(R.id.w_battery, battery)
                v.setTextViewText(R.id.w_distance, distance)
                v.setTextViewText(R.id.w_updated, if (st != null) "Cập nhật ${Dates.relative(st.ts)}" else "")
                v.setOnClickPendingIntent(R.id.w_root, openAppIntent(ctx))
                mgr.updateAppWidget(id, v)
            }
        }
    }
}

object WidgetUpdater {
    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx) ?: return
        fun ids(cls: Class<*>) = mgr.getAppWidgetIds(ComponentName(ctx, cls))
        ids(DaysWidgetProvider::class.java).takeIf { it.isNotEmpty() }?.let { DaysWidgetProvider.render(ctx, mgr, it) }
        ids(PetWidgetProvider::class.java).takeIf { it.isNotEmpty() }?.let { PetWidgetProvider.render(ctx, mgr, it) }
        ids(PartnerWidgetProvider::class.java).takeIf { it.isNotEmpty() }?.let { PartnerWidgetProvider.render(ctx, mgr, it) }
    }
}
