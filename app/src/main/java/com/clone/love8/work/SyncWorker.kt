package com.clone.love8.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.clone.love8.Love8App
import com.clone.love8.data.Repository
import com.clone.love8.sync.SyncManager
import com.clone.love8.util.Dates
import com.clone.love8.util.Events
import com.clone.love8.util.Notifier
import com.clone.love8.util.PetLogic
import com.clone.love8.widget.WidgetUpdater
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Chạy nền định kỳ: nhận tin của người ấy, gửi trạng thái, nhắc ngày đặc biệt, chăm thú cưng. */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        Repository.init(ctx)
        val events = runCatching { SyncManager.pull(ctx) }.getOrDefault(emptyList())
        if (events.isNotEmpty() && !Love8App.foreground) {
            Notifier.notifyIncoming(ctx, events, Repository.data)
        }
        runCatching { SyncManager.sendStatus(ctx) }
        runCatching { checkSpecialDays(ctx) }
        runCatching { checkPet(ctx) }
        runCatching { WidgetUpdater.updateAll(ctx) }
        return Result.success()
    }

    private fun checkSpecialDays(ctx: Context) {
        val d = Repository.data
        if (!d.onboarded || !d.settings.notifySpecialDays) return
        val today = LocalDate.now()
        if (d.settings.lastReminderDay == today.toString()) return
        if (LocalTime.now().hour < 8) return
        Events.all(d, today, milestoneCount = 2).filter { it.remind }.forEach { ev ->
            val left = Dates.daysUntil(ev.date, today)
            val text = when (left) {
                0L -> "Hôm nay là ${ev.title} ${ev.emoji}"
                1L -> "Ngày mai là ${ev.title} ${ev.emoji}"
                7L -> "Còn 7 ngày nữa là ${ev.title} ${ev.emoji}"
                else -> null
            }
            if (text != null) Notifier.show(ctx, Notifier.CH_DAYS, ev.key.hashCode(), "📅 Ngày đặc biệt", text)
        }
        Repository.update { it.copy(settings = it.settings.copy(lastReminderDay = today.toString())) }
    }

    private fun checkPet(ctx: Context) {
        val d = Repository.data
        if (!d.settings.notifyPet || !d.pet.adopted) return
        val now = System.currentTimeMillis()
        if (now - d.settings.lastPetNotifyAt < 4 * 3_600_000L) return
        val pet = PetLogic.tick(d.pet, now)
        if (!PetLogic.needsCare(pet)) return
        Notifier.show(ctx, Notifier.CH_PET, 2002, "🐾 ${pet.name} cần hai bạn!", "${pet.name}: ${PetLogic.mood(pet)} Vào chăm sóc bé nhé!")
        Repository.update { it.copy(settings = it.settings.copy(lastPetNotifyAt = now)) }
    }

    companion object {
        private const val NAME = "love8_sync"

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
