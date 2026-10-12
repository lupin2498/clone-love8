package com.clone.love8.sync

import android.content.Context
import com.clone.love8.data.AppData
import com.clone.love8.data.ProfilePayload
import com.clone.love8.data.Repository
import com.clone.love8.data.SyncEnvelope
import com.clone.love8.util.LocationHelper
import com.clone.love8.util.StatusCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.abs

/** Đồng bộ dữ liệu giữa hai người qua kênh ntfy đã mã hóa. */
object SyncManager {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val _incoming = MutableSharedFlow<SyncEnvelope>(extraBufferCapacity = 32)
    val incoming: SharedFlow<SyncEnvelope> = _incoming.asSharedFlow()

    private const val STATUS_MIN_INTERVAL = 10 * 60_000L

    suspend fun send(env: SyncEnvelope): Boolean = withContext(Dispatchers.IO) {
        val d = Repository.data
        val code = d.pairing.code ?: return@withContext false
        val plain = json.encodeToString(SyncEnvelope.serializer(), env.copy(from = d.deviceId, ts = System.currentTimeMillis()))
        NtfyClient.publish(d.pairing.server, Crypto.topic(code), Crypto.encrypt(code, plain))
    }

    fun sendAsync(env: SyncEnvelope) {
        if (!Repository.data.isPaired) return
        scope.launch { send(env) }
    }

    fun profileEnvelope(d: AppData, reply: Boolean): SyncEnvelope = SyncEnvelope(
        type = "profile",
        profile = ProfilePayload(
            name = d.me.name,
            emoji = d.me.emoji,
            birthday = d.me.birthday,
            startDate = d.startDate,
            startDateUpdatedAt = d.startDateUpdatedAt,
            reply = reply,
        ),
    )

    fun sendProfileAsync(reply: Boolean = false) = sendAsync(profileEnvelope(Repository.data, reply))

    /** Lấy tin mới từ máy chủ, hợp nhất vào dữ liệu cục bộ và trả về các sự kiện của người ấy. */
    suspend fun pull(ctx: Context): List<SyncEnvelope> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val d = Repository.data
            val code = d.pairing.code ?: return@withContext emptyList<SyncEnvelope>()
            val msgs = NtfyClient.poll(d.pairing.server, Crypto.topic(code), d.pairing.since ?: "all")
                ?: return@withContext emptyList<SyncEnvelope>()
            if (msgs.isEmpty()) return@withContext emptyList<SyncEnvelope>()
            val events = msgs.mapNotNull { m ->
                val plain = Crypto.decrypt(code, m.message) ?: return@mapNotNull null
                runCatching { json.decodeFromString(SyncEnvelope.serializer(), plain) }.getOrNull()
            }.filter { it.from.isNotBlank() && it.from != d.deviceId }
            val lastId = msgs.last().id
            Repository.update { cur ->
                if (cur.pairing.code != code) {
                    cur
                } else {
                    events.fold(cur.copy(pairing = cur.pairing.copy(since = lastId))) { acc, e -> Merge.event(acc, e) }
                }
            }
            if (events.any { it.profile?.reply == true }) {
                val now = Repository.data
                send(profileEnvelope(now, reply = false))
                if (now.pet.adopted) send(SyncEnvelope(type = "pet", pet = now.pet.copy(log = now.pet.log.take(8))))
                now.specialDays.filter { it.createdBy == now.deviceId }.take(10).forEach {
                    send(SyncEnvelope(type = "special_day", specialDay = it))
                }
                sendStatus(ctx, force = true)
            }
            events.forEach { _incoming.tryEmit(it) }
            events
        }
    }

    /** Gửi trạng thái pin / vị trí nếu có thay đổi đáng kể hoặc đã lâu chưa gửi. */
    suspend fun sendStatus(ctx: Context, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val d = Repository.data
        val st = StatusCollector.collect(ctx, d.myStatus, d.settings.shareBattery, d.settings.shareLocation)
        val prev = d.myStatus
        Repository.update { it.copy(myStatus = st) }
        if (!d.isPaired) return@withContext false
        val now = System.currentTimeMillis()
        val moved = (LocationHelper.distanceBetween(prev, st) ?: 0f) > 150f
        val battChanged = abs((prev.battery ?: -100) - (st.battery ?: -100)) >= 5 || prev.charging != st.charging
        val due = now - d.pairing.lastStatusSentAt >= STATUS_MIN_INTERVAL
        if (!force && !due && !moved && !battChanged) return@withContext false
        val ok = send(SyncEnvelope(type = "status", status = st))
        if (ok) Repository.update { it.copy(pairing = it.pairing.copy(lastStatusSentAt = now)) }
        ok
    }

    fun sendStatusAsync(ctx: Context, force: Boolean = false) {
        scope.launch { sendStatus(ctx.applicationContext, force) }
    }
}

/** Quy tắc hợp nhất dữ liệu nhận từ người ấy. */
object Merge {
    fun event(d: AppData, e: SyncEnvelope): AppData {
        var r = d.copy(pairing = d.pairing.copy(partnerDevice = e.from))
        val p = e.profile
        if (p != null) {
            r = r.copy(
                partner = r.partner.copy(
                    name = p.name.ifBlank { r.partner.name },
                    emoji = p.emoji.ifBlank { r.partner.emoji },
                    birthday = p.birthday ?: r.partner.birthday,
                )
            )
            if (p.startDate != null && p.startDateUpdatedAt > r.startDateUpdatedAt) {
                r = r.copy(startDate = p.startDate, startDateUpdatedAt = p.startDateUpdatedAt)
            }
        }
        val s = e.status
        if (s != null && s.ts >= (r.partnerStatus?.ts ?: 0L)) {
            r = r.copy(partnerStatus = s)
        }
        val m = e.message
        if (m != null && r.messages.none { it.id == m.id }) {
            r = r.copy(messages = (r.messages + m).sortedBy { it.ts }.takeLast(200))
        }
        val pet = e.pet
        if (pet != null && pet.updatedAt > r.pet.updatedAt) {
            r = r.copy(pet = pet)
        }
        val sd = e.specialDay
        if (sd != null && sd.id !in r.deletedIds) {
            r = r.copy(specialDays = r.specialDays.filterNot { it.id == sd.id } + sd)
        }
        val de = e.diary
        if (de != null && de.id !in r.deletedIds && r.diary.none { it.id == de.id }) {
            r = r.copy(diary = (r.diary + de).sortedByDescending { it.ts })
        }
        val del = e.deleteId
        if (del != null) {
            r = r.copy(
                specialDays = r.specialDays.filterNot { it.id == del },
                diary = r.diary.filterNot { it.id == del },
                deletedIds = (r.deletedIds + del).takeLast(300),
            )
        }
        return r
    }
}
