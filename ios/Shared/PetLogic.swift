import Foundation

enum PetLogic {
    struct PetType: Identifiable {
        let id: String
        let emoji: String
        let label: String
    }

    struct Accessory: Identifiable {
        let id: String
        let emoji: String
        let label: String
        let price: Int
    }

    enum Action { case feed, play, bathe, sleep, pat }

    struct Outcome {
        let pet: PetState
        let message: String
        let coins: Int
        let ok: Bool
    }

    static let types: [PetType] = [
        PetType(id: "cat", emoji: "🐱", label: "Mèo"),
        PetType(id: "dog", emoji: "🐶", label: "Cún"),
        PetType(id: "bunny", emoji: "🐰", label: "Thỏ"),
        PetType(id: "bear", emoji: "🐻", label: "Gấu"),
        PetType(id: "panda", emoji: "🐼", label: "Gấu trúc"),
        PetType(id: "penguin", emoji: "🐧", label: "Cánh cụt"),
        PetType(id: "fox", emoji: "🦊", label: "Cáo"),
        PetType(id: "hamster", emoji: "🐹", label: "Hamster"),
        PetType(id: "chick", emoji: "🐥", label: "Gà con"),
        PetType(id: "tiger", emoji: "🐯", label: "Hổ con"),
    ]

    static let accessories: [Accessory] = [
        Accessory(id: "flower", emoji: "🌸", label: "Hoa anh đào", price: 40),
        Accessory(id: "bow", emoji: "🎀", label: "Nơ hồng", price: 50),
        Accessory(id: "cap", emoji: "🧢", label: "Mũ lưỡi trai", price: 60),
        Accessory(id: "glasses", emoji: "🕶️", label: "Kính râm", price: 80),
        Accessory(id: "tophat", emoji: "🎩", label: "Mũ chóp", price: 120),
        Accessory(id: "grad", emoji: "🎓", label: "Mũ tốt nghiệp", price: 150),
        Accessory(id: "crown", emoji: "👑", label: "Vương miện", price: 200),
    ]

    static func emoji(_ type: String) -> String {
        types.first { $0.id == type }?.emoji ?? "🐱"
    }

    static func accessoryEmoji(_ id: String?) -> String? {
        guard let id = id else { return nil }
        return accessories.first { $0.id == id }?.emoji
    }

    static func xpForLevel(_ level: Int) -> Int { 50 + level * 50 }

    private static func clamp(_ v: Double) -> Double { min(max(v, 0), 100) }

    /// Áp dụng suy giảm chỉ số theo thời gian (giống bản Android).
    static func tick(_ p: PetState, now: Int = nowMs()) -> PetState {
        if !p.adopted { return p }
        var r = p
        if p.lastTick <= 0 {
            r.lastTick = now
            return r
        }
        let hours = Double(now - p.lastTick) / 3_600_000.0
        if hours <= 0 { return p }
        let energyDelta = p.sleeping ? 15 * hours : -3 * hours
        let newEnergy = clamp(p.energy + energyDelta)
        r.hunger = clamp(p.hunger - 5 * hours)
        r.happiness = clamp(p.happiness - 4 * hours)
        r.energy = newEnergy
        r.cleanliness = clamp(p.cleanliness - 3 * hours)
        r.sleeping = p.sleeping && newEnergy < 100
        r.lastTick = now
        return r
    }

    static func perform(_ p0: PetState, _ action: Action, actor: String, now: Int = nowMs()) -> Outcome {
        let p = tick(p0, now: now)
        if p.sleeping && action != .sleep {
            return Outcome(pet: p, message: "\(p.name) đang ngủ 😴 Hãy để bé nghỉ ngơi nhé!", coins: 0, ok: false)
        }
        var n = p
        let msg: String
        switch action {
        case .feed:
            if p.hunger >= 98 {
                return Outcome(pet: p, message: "\(p.name) no căng bụng rồi 🤭", coins: 0, ok: false)
            }
            n.hunger = clamp(p.hunger + 25)
            n.happiness = clamp(p.happiness + 3)
            n.xp += 5
            msg = "\(actor) đã cho \(p.name) ăn 🍖"
        case .play:
            if p.energy < 10 {
                return Outcome(pet: p, message: "\(p.name) mệt rồi, cho bé ngủ một chút nhé 💤", coins: 0, ok: false)
            }
            n.happiness = clamp(p.happiness + 20)
            n.energy = clamp(p.energy - 10)
            n.hunger = clamp(p.hunger - 5)
            n.xp += 8
            msg = "\(actor) đã chơi cùng \(p.name) ⚽"
        case .bathe:
            n.cleanliness = 100
            n.happiness = clamp(p.happiness + 5)
            n.xp += 4
            msg = "\(actor) đã tắm cho \(p.name) 🛁"
        case .sleep:
            n.sleeping = !p.sleeping
            msg = p.sleeping ? "\(actor) đã đánh thức \(p.name) ☀️" : "\(actor) đã ru \(p.name) ngủ 🌙"
        case .pat:
            n.happiness = clamp(p.happiness + 3)
            n.xp += 1
            msg = "\(actor) đã vuốt ve \(p.name) 💕"
        }
        var coins = action == .pat ? 0 : 2
        while n.xp >= xpForLevel(n.level) {
            n.xp -= xpForLevel(n.level)
            n.level += 1
            coins += 50
        }
        if action != .pat {
            n.log = Array(([PetLog(ts: now, text: msg)] + n.log).prefix(20))
        }
        n.updatedAt = now
        return Outcome(pet: n, message: msg, coins: coins, ok: true)
    }

    static func mood(_ p: PetState) -> String {
        let avg = (p.hunger + p.happiness + p.energy + p.cleanliness) / 4
        if p.sleeping { return "Đang ngủ say 💤" }
        if p.hunger < 25 { return "Đói bụng quá 🥺" }
        if p.cleanliness < 25 { return "Cần được tắm 🫧" }
        if p.energy < 20 { return "Buồn ngủ 😪" }
        if p.happiness < 25 { return "Nhớ hai bạn 😢" }
        if avg > 75 { return "Rất hạnh phúc 🥰" }
        if avg > 50 { return "Vui vẻ 😊" }
        return "Hơi buồn 😔"
    }

    /// Thời điểm (ms) bé sẽ đói (no bụng < 25) nếu không ai chăm.
    static func hungryAt(_ p: PetState, now: Int = nowMs()) -> Int? {
        guard p.adopted else { return nil }
        let t = tick(p, now: now)
        if t.hunger <= 25 { return nil }
        let hours = (t.hunger - 25) / 5
        return now + Int(hours * 3_600_000)
    }
}
