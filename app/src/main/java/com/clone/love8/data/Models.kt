package com.clone.love8.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

const val DEFAULT_SERVER = "https://ntfy.sh"

@Serializable
data class Profile(
    val name: String = "",
    val emoji: String = "🙂",
    val birthday: String? = null,
)

@Serializable
data class SpecialDay(
    val id: String,
    val title: String,
    val date: String,
    val emoji: String = "💝",
    val yearly: Boolean = true,
    val remind: Boolean = true,
    val createdBy: String = "",
)

@Serializable
data class PetLog(val ts: Long, val text: String)

@Serializable
data class PetState(
    val adopted: Boolean = false,
    val type: String = "cat",
    val name: String = "Mochi",
    val hunger: Float = 80f,
    val happiness: Float = 80f,
    val energy: Float = 80f,
    val cleanliness: Float = 80f,
    val xp: Int = 0,
    val level: Int = 1,
    val sleeping: Boolean = false,
    val accessory: String? = null,
    val ownedAccessories: List<String> = emptyList(),
    val lastTick: Long = 0L,
    val updatedAt: Long = 0L,
    val log: List<PetLog> = emptyList(),
)

@Serializable
data class LoveMessage(
    val id: String,
    val sender: String,
    val kind: String = "text",
    val text: String = "",
    val ts: Long,
)

@Serializable
data class DiaryEntry(
    val id: String,
    val author: String,
    val mood: String = "😊",
    val text: String,
    val ts: Long,
)

@Serializable
data class DeviceStatus(
    val battery: Int? = null,
    val charging: Boolean = false,
    val lat: Double? = null,
    val lon: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val locationTs: Long? = null,
    val mood: String? = null,
    val ts: Long = 0L,
)

@Serializable
data class Pairing(
    val code: String? = null,
    val server: String = DEFAULT_SERVER,
    val since: String? = null,
    val partnerDevice: String? = null,
    val lastStatusSentAt: Long = 0L,
)

@Serializable
data class Settings(
    val shareLocation: Boolean = true,
    val shareBattery: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyPet: Boolean = true,
    val notifySpecialDays: Boolean = true,
    val lastReminderDay: String? = null,
    val lastPetNotifyAt: Long = 0L,
    val lastCheckInDay: String? = null,
)

@Serializable
data class AppData(
    val deviceId: String = "",
    val onboarded: Boolean = false,
    val me: Profile = Profile(),
    val partner: Profile = Profile(name = "Người ấy", emoji = "💖"),
    val mePhotoStamp: Long = 0L,
    val partnerPhotoStamp: Long = 0L,
    val startDate: String = LocalDate.now().toString(),
    val startDateUpdatedAt: Long = 0L,
    val specialDays: List<SpecialDay> = emptyList(),
    val deletedIds: List<String> = emptyList(),
    val pet: PetState = PetState(),
    val coins: Int = 100,
    val messages: List<LoveMessage> = emptyList(),
    val diary: List<DiaryEntry> = emptyList(),
    val myStatus: DeviceStatus = DeviceStatus(),
    val partnerStatus: DeviceStatus? = null,
    val pairing: Pairing = Pairing(),
    val settings: Settings = Settings(),
) {
    val isPaired: Boolean get() = pairing.code != null
    val partnerConnected: Boolean get() = pairing.code != null && pairing.partnerDevice != null
}

@Serializable
data class ProfilePayload(
    val name: String,
    val emoji: String,
    val birthday: String? = null,
    val startDate: String? = null,
    val startDateUpdatedAt: Long = 0L,
    val reply: Boolean = false,
)

/** Gói tin đồng bộ giữa hai thiết bị (được mã hóa đầu-cuối trước khi gửi). */
@Serializable
data class SyncEnvelope(
    val type: String,
    val from: String = "",
    val ts: Long = System.currentTimeMillis(),
    val profile: ProfilePayload? = null,
    val status: DeviceStatus? = null,
    val message: LoveMessage? = null,
    val pet: PetState? = null,
    val specialDay: SpecialDay? = null,
    val diary: DiaryEntry? = null,
    val deleteId: String? = null,
)
