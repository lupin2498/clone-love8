package com.clone.love8.data

import android.content.Context
import com.clone.love8.sync.Crypto
import com.clone.love8.sync.SyncManager
import com.clone.love8.util.PetLogic
import com.clone.love8.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Các thao tác của người dùng: cập nhật dữ liệu cục bộ và đồng bộ sang người ấy. */
object Actions {
    private fun newId() = UUID.randomUUID().toString().replace("-", "").take(16)

    private fun refreshWidgets() {
        runCatching { WidgetUpdater.updateAll(Repository.appContext) }
    }

    fun finishOnboarding(me: Profile, partner: Profile, start: LocalDate) {
        Repository.update {
            it.copy(
                onboarded = true,
                me = me,
                partner = partner,
                startDate = start.toString(),
                startDateUpdatedAt = System.currentTimeMillis(),
            )
        }
        refreshWidgets()
    }

    fun updateMe(profile: Profile) {
        Repository.update { it.copy(me = profile) }
        SyncManager.sendProfileAsync()
        refreshWidgets()
    }

    fun updatePartner(profile: Profile) {
        Repository.update { it.copy(partner = profile) }
        refreshWidgets()
    }

    fun setStartDate(date: LocalDate) {
        Repository.update { it.copy(startDate = date.toString(), startDateUpdatedAt = System.currentTimeMillis()) }
        SyncManager.sendProfileAsync()
        refreshWidgets()
    }

    fun photoChanged(isMe: Boolean) {
        val now = System.currentTimeMillis()
        Repository.update { if (isMe) it.copy(mePhotoStamp = now) else it.copy(partnerPhotoStamp = now) }
        refreshWidgets()
    }

    fun sendMessage(kind: String, text: String) {
        val d = Repository.data
        val m = LoveMessage(id = newId(), sender = d.deviceId, kind = kind, text = text.take(500), ts = System.currentTimeMillis())
        Repository.update { it.copy(messages = (it.messages + m).takeLast(200)) }
        SyncManager.sendAsync(SyncEnvelope(type = "message", message = m))
    }

    fun setMood(ctx: Context, mood: String) {
        Repository.update { it.copy(myStatus = it.myStatus.copy(mood = mood)) }
        SyncManager.sendStatusAsync(ctx, force = true)
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        Repository.update { it.copy(settings = transform(it.settings)) }
    }

    // ---------- Thú cưng ----------

    fun adoptPet(type: String, name: String) {
        val now = System.currentTimeMillis()
        val d = Repository.data
        val pet = PetState(
            adopted = true,
            type = type,
            name = name.ifBlank { "Mochi" },
            lastTick = now,
            updatedAt = now,
            log = listOf(PetLog(now, "${d.me.name.ifBlank { "Bạn" }} đã nhận nuôi ${name.ifBlank { "Mochi" }} 🎉")),
        )
        Repository.update { it.copy(pet = pet) }
        syncPet(pet)
        refreshWidgets()
    }

    fun petAction(action: PetLogic.Action): String {
        val d = Repository.data
        val res = PetLogic.perform(d.pet, action, d.me.name.ifBlank { "Bạn" })
        if (res.ok) {
            Repository.update { it.copy(pet = res.pet, coins = it.coins + res.coins) }
            if (action != PetLogic.Action.PAT) syncPet(res.pet)
            refreshWidgets()
        }
        return res.message
    }

    fun renamePet(name: String, type: String) {
        val now = System.currentTimeMillis()
        val pet = PetLogic.tick(Repository.data.pet, now).copy(name = name.ifBlank { "Mochi" }, type = type, updatedAt = now)
        Repository.update { it.copy(pet = pet) }
        syncPet(pet)
        refreshWidgets()
    }

    /** Mua hoặc đeo / tháo phụ kiện. Trả về thông báo cho người dùng. */
    fun accessory(id: String): String {
        val acc = PetLogic.accessories.firstOrNull { it.id == id } ?: return ""
        val d = Repository.data
        val now = System.currentTimeMillis()
        val pet = PetLogic.tick(d.pet, now)
        return when {
            pet.accessory == id -> {
                val p = pet.copy(accessory = null, updatedAt = now)
                Repository.update { it.copy(pet = p) }
                syncPet(p)
                "Đã tháo ${acc.label}"
            }
            id in pet.ownedAccessories -> {
                val p = pet.copy(accessory = id, updatedAt = now)
                Repository.update { it.copy(pet = p) }
                syncPet(p)
                "${pet.name} đã đeo ${acc.label} ${acc.emoji}"
            }
            d.coins < acc.price -> "Không đủ xu 🪙 (cần ${acc.price})"
            else -> {
                val p = pet.copy(
                    accessory = id,
                    ownedAccessories = pet.ownedAccessories + id,
                    happiness = (pet.happiness + 10f).coerceAtMost(100f),
                    updatedAt = now,
                )
                Repository.update { it.copy(pet = p, coins = it.coins - acc.price) }
                syncPet(p)
                "Đã mua ${acc.label} ${acc.emoji} cho ${pet.name}!"
            }
        }.also { refreshWidgets() }
    }

    fun dailyCheckIn(): Boolean {
        val today = LocalDate.now().toString()
        if (Repository.data.settings.lastCheckInDay == today) return false
        Repository.update { it.copy(coins = it.coins + 20, settings = it.settings.copy(lastCheckInDay = today)) }
        return true
    }

    private fun syncPet(pet: PetState) {
        SyncManager.sendAsync(SyncEnvelope(type = "pet", pet = pet.copy(log = pet.log.take(8))))
    }

    // ---------- Ngày đặc biệt ----------

    fun addSpecialDay(title: String, date: LocalDate, emoji: String, yearly: Boolean, remind: Boolean) {
        val d = Repository.data
        val sd = SpecialDay(
            id = newId(),
            title = title.trim().take(60),
            date = date.toString(),
            emoji = emoji,
            yearly = yearly,
            remind = remind,
            createdBy = d.deviceId,
        )
        Repository.update { it.copy(specialDays = it.specialDays + sd) }
        SyncManager.sendAsync(SyncEnvelope(type = "special_day", specialDay = sd))
    }

    fun deleteItem(id: String) {
        Repository.update {
            it.copy(
                specialDays = it.specialDays.filterNot { s -> s.id == id },
                diary = it.diary.filterNot { e -> e.id == id },
                deletedIds = (it.deletedIds + id).takeLast(300),
            )
        }
        SyncManager.sendAsync(SyncEnvelope(type = "delete", deleteId = id))
    }

    // ---------- Nhật ký ----------

    fun addDiary(mood: String, text: String) {
        val d = Repository.data
        val e = DiaryEntry(id = newId(), author = d.deviceId, mood = mood, text = text.trim().take(1000), ts = System.currentTimeMillis())
        Repository.update { it.copy(diary = (listOf(e) + it.diary)) }
        SyncManager.sendAsync(SyncEnvelope(type = "diary", diary = e))
    }

    // ---------- Kết nối ----------

    fun createPairing(ctx: Context): String {
        val code = Crypto.newCode()
        connect(ctx, code)
        return code
    }

    fun joinPairing(ctx: Context, raw: String): Boolean {
        val code = Crypto.normalize(raw)
        if (code.length != 8) return false
        connect(ctx, code)
        return true
    }

    private fun connect(ctx: Context, code: String) {
        Repository.update {
            it.copy(pairing = it.pairing.copy(code = code, since = null, partnerDevice = null, lastStatusSentAt = 0L), partnerStatus = null)
        }
        val app = ctx.applicationContext
        SyncManager.scope.launch {
            SyncManager.send(SyncManager.profileEnvelope(Repository.data, reply = true))
            SyncManager.pull(app)
            SyncManager.sendStatus(app, force = true)
        }
    }

    fun setServer(server: String) {
        val s = server.trim().trimEnd('/').ifBlank { DEFAULT_SERVER }
        Repository.update { it.copy(pairing = it.pairing.copy(server = s, since = null)) }
    }

    fun unpair() {
        Repository.update { it.copy(pairing = Pairing(server = it.pairing.server), partnerStatus = null) }
        refreshWidgets()
    }
}
