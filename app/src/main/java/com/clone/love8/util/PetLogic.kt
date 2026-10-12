package com.clone.love8.util

import com.clone.love8.data.PetLog
import com.clone.love8.data.PetState

object PetLogic {
    data class PetType(val id: String, val emoji: String, val label: String)
    data class Accessory(val id: String, val emoji: String, val label: String, val price: Int)

    enum class Action { FEED, PLAY, BATHE, SLEEP, PAT }

    data class Outcome(val pet: PetState, val message: String, val coins: Int, val ok: Boolean)

    val types = listOf(
        PetType("cat", "🐱", "Mèo"),
        PetType("dog", "🐶", "Cún"),
        PetType("bunny", "🐰", "Thỏ"),
        PetType("bear", "🐻", "Gấu"),
        PetType("panda", "🐼", "Gấu trúc"),
        PetType("penguin", "🐧", "Cánh cụt"),
        PetType("fox", "🦊", "Cáo"),
        PetType("hamster", "🐹", "Hamster"),
        PetType("chick", "🐥", "Gà con"),
        PetType("tiger", "🐯", "Hổ con"),
    )

    val accessories = listOf(
        Accessory("flower", "🌸", "Hoa anh đào", 40),
        Accessory("bow", "🎀", "Nơ hồng", 50),
        Accessory("cap", "🧢", "Mũ lưỡi trai", 60),
        Accessory("glasses", "🕶️", "Kính râm", 80),
        Accessory("tophat", "🎩", "Mũ chóp", 120),
        Accessory("grad", "🎓", "Mũ tốt nghiệp", 150),
        Accessory("crown", "👑", "Vương miện", 200),
    )

    fun emoji(type: String): String = types.firstOrNull { it.id == type }?.emoji ?: "🐱"

    fun accessoryEmoji(id: String?): String? = accessories.firstOrNull { it.id == id }?.emoji

    fun xpForLevel(level: Int): Int = 50 + level * 50

    private fun clamp(v: Float) = v.coerceIn(0f, 100f)

    /** Áp dụng suy giảm chỉ số theo thời gian thực. */
    fun tick(p: PetState, now: Long = System.currentTimeMillis()): PetState {
        if (!p.adopted) return p
        if (p.lastTick <= 0L) return p.copy(lastTick = now)
        val hours = (now - p.lastTick) / 3_600_000f
        if (hours <= 0f) return p
        val energyDelta = if (p.sleeping) 15f * hours else -3f * hours
        val newEnergy = clamp(p.energy + energyDelta)
        return p.copy(
            hunger = clamp(p.hunger - 5f * hours),
            happiness = clamp(p.happiness - 4f * hours),
            energy = newEnergy,
            cleanliness = clamp(p.cleanliness - 3f * hours),
            sleeping = p.sleeping && newEnergy < 100f,
            lastTick = now,
        )
    }

    fun perform(p0: PetState, action: Action, actor: String, now: Long = System.currentTimeMillis()): Outcome {
        val p = tick(p0, now)
        if (p.sleeping && action != Action.SLEEP) {
            return Outcome(p, "${p.name} đang ngủ 😴 Hãy để bé nghỉ ngơi nhé!", 0, false)
        }
        val (changed, msg) = when (action) {
            Action.FEED -> {
                if (p.hunger >= 98f) return Outcome(p, "${p.name} no căng bụng rồi 🤭", 0, false)
                p.copy(hunger = clamp(p.hunger + 25f), happiness = clamp(p.happiness + 3f), xp = p.xp + 5) to
                    "$actor đã cho ${p.name} ăn 🍖"
            }
            Action.PLAY -> {
                if (p.energy < 10f) return Outcome(p, "${p.name} mệt rồi, cho bé ngủ một chút nhé 💤", 0, false)
                p.copy(
                    happiness = clamp(p.happiness + 20f),
                    energy = clamp(p.energy - 10f),
                    hunger = clamp(p.hunger - 5f),
                    xp = p.xp + 8,
                ) to "$actor đã chơi cùng ${p.name} ⚽"
            }
            Action.BATHE -> p.copy(cleanliness = 100f, happiness = clamp(p.happiness + 5f), xp = p.xp + 4) to
                "$actor đã tắm cho ${p.name} 🛁"
            Action.SLEEP -> p.copy(sleeping = !p.sleeping) to
                (if (p.sleeping) "$actor đã đánh thức ${p.name} ☀️" else "$actor đã ru ${p.name} ngủ 🌙")
            Action.PAT -> p.copy(happiness = clamp(p.happiness + 3f), xp = p.xp + 1) to
                "$actor đã vuốt ve ${p.name} 💕"
        }
        var leveled = changed
        var coins = if (action == Action.PAT) 0 else 2
        while (leveled.xp >= xpForLevel(leveled.level)) {
            leveled = leveled.copy(xp = leveled.xp - xpForLevel(leveled.level), level = leveled.level + 1)
            coins += 50
        }
        val logged = if (action == Action.PAT) leveled.log else (listOf(PetLog(now, msg)) + leveled.log).take(20)
        return Outcome(leveled.copy(updatedAt = now, log = logged), msg, coins, true)
    }

    fun mood(p: PetState): String {
        val avg = (p.hunger + p.happiness + p.energy + p.cleanliness) / 4f
        return when {
            p.sleeping -> "Đang ngủ say 💤"
            p.hunger < 25f -> "Đói bụng quá 🥺"
            p.cleanliness < 25f -> "Cần được tắm 🫧"
            p.energy < 20f -> "Buồn ngủ 😪"
            p.happiness < 25f -> "Nhớ hai bạn 😢"
            avg > 75f -> "Rất hạnh phúc 🥰"
            avg > 50f -> "Vui vẻ 😊"
            else -> "Hơi buồn 😔"
        }
    }

    fun needsCare(p: PetState): Boolean =
        p.adopted && (p.hunger < 25f || p.happiness < 25f || p.cleanliness < 20f)
}
