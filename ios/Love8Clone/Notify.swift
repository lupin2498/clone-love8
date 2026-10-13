import Foundation
import UserNotifications

/// Thông báo cục bộ: tin nhắn của người ấy, nhắc ngày đặc biệt, thú cưng đói.
enum Notify {
    static func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    static func now(id: String, title: String, body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let req = UNNotificationRequest(identifier: id, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(req, withCompletionHandler: nil)
    }

    static func incoming(_ events: [SyncEnvelope], data d: AppData) {
        guard d.settings.notifyMessages else { return }
        let partner = d.partnerName
        for e in events {
            if let m = e.message {
                let text: String
                switch m.kind {
                case "miss_you": text = "💗 \(partner) đang nhớ bạn!"
                case "heart": text = "💖 \(partner) gửi bạn một trái tim"
                default: text = m.text
                }
                now(id: "msg-\(m.id)", title: partner, body: text)
            } else if let pet = e.pet, d.settings.notifyPet, let last = pet.log.first?.text {
                now(id: "pet-\(pet.updatedAt)", title: "🐾 \(pet.name)", body: last)
            } else if let sd = e.specialDay, d.settings.notifySpecialDays {
                now(id: "sd-\(sd.id)", title: "📅 Ngày đặc biệt mới", body: "\(partner) đã thêm \"\(sd.title)\"")
            } else if let de = e.diary {
                now(id: "diary-\(de.id)", title: "📔 Nhật ký mới", body: "\(partner) vừa viết nhật ký: \(de.text.prefix(80))")
            }
        }
    }

    /// Lên lịch nhắc ngày đặc biệt (D-7, D-1, hôm nay lúc 9:00) và nhắc thú cưng đói.
    static func reschedule(_ d: AppData) {
        let center = UNUserNotificationCenter.current()
        center.getPendingNotificationRequests { pending in
            let old = pending.map { $0.identifier }.filter { $0.hasPrefix("day-") || $0.hasPrefix("pethungry") }
            center.removePendingNotificationRequests(withIdentifiers: old)

            if d.onboarded && d.settings.notifySpecialDays {
                let today = Dates.startOfDay()
                var count = 0
                for ev in Events.all(d, today: today, milestoneCount: 3) where ev.remind {
                    for (offset, label) in [(7, "Còn 7 ngày nữa là"), (1, "Ngày mai là"), (0, "Hôm nay là")] {
                        guard count < 45 else { break }
                        let fireDay = Dates.addDays(ev.date, -offset)
                        guard let fire = Dates.cal.date(bySettingHour: 9, minute: 0, second: 0, of: fireDay),
                              fire > Date() else { continue }
                        let content = UNMutableNotificationContent()
                        content.title = "📅 Ngày đặc biệt"
                        content.body = "\(label) \(ev.title) \(ev.emoji)"
                        content.sound = .default
                        let comps = Dates.cal.dateComponents([.year, .month, .day, .hour, .minute], from: fire)
                        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
                        center.add(UNNotificationRequest(identifier: "day-\(ev.id)-\(offset)", content: content, trigger: trigger))
                        count += 1
                    }
                }
            }

            if d.settings.notifyPet, let at = PetLogic.hungryAt(d.pet) {
                let seconds = max(60, Double(at - nowMs()) / 1000)
                let content = UNMutableNotificationContent()
                content.title = "🐾 \(d.pet.name) đói rồi!"
                content.body = "\(d.pet.name) đang nhớ hai bạn 🥺 Vào cho bé ăn nhé!"
                content.sound = .default
                let trigger = UNTimeIntervalNotificationTrigger(timeInterval: seconds, repeats: false)
                center.add(UNNotificationRequest(identifier: "pethungry", content: content, trigger: trigger))
            }
        }
    }
}
