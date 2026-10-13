import SwiftUI
import UIKit
import WidgetKit

struct LoveEntry: TimelineEntry {
    let date: Date
    let data: AppData
}

struct LoveProvider: TimelineProvider {
    func placeholder(in context: Context) -> LoveEntry {
        var d = AppData()
        d.onboarded = true
        d.me = Profile(name: "Bạn", emoji: "😊")
        d.partner = Profile(name: "Người ấy", emoji: "🥰")
        d.startDate = Dates.string(Dates.addDays(Date(), -99))
        d.pet.adopted = true
        return LoveEntry(date: Date(), data: d)
    }

    func getSnapshot(in context: Context, completion: @escaping (LoveEntry) -> Void) {
        let d = SharedStore.load()
        completion(d.onboarded ? LoveEntry(date: Date(), data: d) : placeholder(in: context))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<LoveEntry>) -> Void) {
        let now = Date()
        let d = SharedStore.load()
        let midnight = Dates.addDays(now, 1)
        let halfHour = now.addingTimeInterval(30 * 60)
        completion(Timeline(entries: [LoveEntry(date: now, data: d)], policy: .after(min(midnight, halfHour))))
    }
}

private let pink = Color(red: 1.0, green: 0.36, blue: 0.54)
private let pinkTop = Color(red: 1.0, green: 0.62, blue: 0.73)
private let textDark = Color(red: 0.35, green: 0.06, blue: 0.15)
private let textSub = Color(red: 0.48, green: 0.33, blue: 0.38)

extension View {
    @ViewBuilder
    func widgetBackground<B: View>(_ background: B) -> some View {
        if #available(iOS 17.0, *) {
            containerBackground(for: .widget) { background }
        } else {
            self.background(background)
        }
    }
}

private struct WidgetAvatar: View {
    let isMe: Bool
    let emoji: String
    let size: CGFloat

    var body: some View {
        let url = SharedStore.avatarURL(isMe: isMe)
        ZStack {
            Circle().fill(Color.white.opacity(0.85))
            if let img = UIImage(contentsOfFile: url.path) {
                Image(uiImage: img).resizable().scaledToFill()
            } else {
                Text(emoji).font(.system(size: size * 0.5))
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color.white, lineWidth: 2))
    }
}

struct DaysWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: LoveEntry

    var body: some View {
        let d = entry.data
        let start = Dates.parse(d.startDate) ?? entry.date
        let days = max(0, Dates.daysTogether(start, entry.date))
        VStack(spacing: 4) {
            if !d.onboarded {
                Text("💗").font(.system(size: 34))
                Text("Mở app để bắt đầu").font(.caption).foregroundColor(.white)
            } else {
                HStack(spacing: 6) {
                    WidgetAvatar(isMe: true, emoji: d.me.emoji, size: family == .systemSmall ? 36 : 44)
                    Text("❤").foregroundColor(.white)
                    WidgetAvatar(isMe: false, emoji: d.partner.emoji, size: family == .systemSmall ? 36 : 44)
                }
                Text("Đã yêu nhau").font(.caption2).foregroundColor(.white.opacity(0.9))
                HStack(alignment: .lastTextBaseline, spacing: 3) {
                    Text("\(days)")
                        .font(.system(size: family == .systemSmall ? 30 : 36, weight: .heavy, design: .rounded))
                        .foregroundColor(.white)
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                    Text("ngày").font(.caption).foregroundColor(.white)
                }
                if family != .systemSmall {
                    Text("\(d.meName) ♥ \(d.partnerName)").font(.caption).foregroundColor(.white).lineLimit(1)
                    Text(Dates.breakdown(start, entry.date)).font(.caption2).foregroundColor(.white.opacity(0.85))
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .widgetBackground(LinearGradient(colors: [pinkTop, pink], startPoint: .topLeading, endPoint: .bottomTrailing))
    }
}

struct PetWidgetView: View {
    let entry: LoveEntry

    var body: some View {
        let pet = PetLogic.tick(entry.data.pet, now: Int(entry.date.timeIntervalSince1970 * 1000))
        HStack(spacing: 10) {
            VStack(spacing: -10) {
                if pet.adopted, let acc = PetLogic.accessoryEmoji(pet.accessory) {
                    Text(acc).font(.system(size: 22))
                }
                Text(pet.adopted ? PetLogic.emoji(pet.type) : "🥚").font(.system(size: 50))
            }
            VStack(alignment: .leading, spacing: 3) {
                Text(pet.adopted ? "\(pet.name) · Lv.\(pet.level)" : "Chưa có thú cưng")
                    .font(.headline).foregroundColor(textDark).lineLimit(1)
                Text(pet.adopted ? PetLogic.mood(pet) : "Mở app để nhận nuôi nhé!")
                    .font(.caption).foregroundColor(textSub).lineLimit(1)
                if pet.adopted {
                    bar("🍖", pet.hunger, .orange)
                    bar("😊", pet.happiness, pink)
                }
            }
        }
        .padding(.horizontal, 4)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .widgetBackground(LinearGradient(colors: [Color.white, Color(red: 1.0, green: 0.89, blue: 0.93)], startPoint: .top, endPoint: .bottom))
    }

    private func bar(_ label: String, _ value: Double, _ color: Color) -> some View {
        HStack(spacing: 4) {
            Text(label).font(.caption2)
            ProgressView(value: min(max(value, 0), 100), total: 100).tint(color)
        }
    }
}

struct DaysWidget: Widget {
    let kind = "Love8DaysWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: LoveProvider()) { entry in
            DaysWidgetView(entry: entry)
        }
        .configurationDisplayName("Ngày yêu nhau")
        .description("Đếm số ngày hai bạn đã bên nhau")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct PetWidget: Widget {
    let kind = "Love8PetWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: LoveProvider()) { entry in
            PetWidgetView(entry: entry)
        }
        .configurationDisplayName("Thú cưng")
        .description("Thú cưng chung của hai bạn")
        .supportedFamilies([.systemMedium])
    }
}

@main
struct Love8Widgets: WidgetBundle {
    var body: some Widget {
        DaysWidget()
        PetWidget()
    }
}
