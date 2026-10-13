import SwiftUI

private let quickMessages = [
    "Nhớ em/anh nhiều 💕",
    "Ăn cơm chưa? 🍚",
    "Đang làm gì đó? 👀",
    "Về đến nhà chưa? 🏠",
    "Ngủ ngon nhé 🌙",
    "Yêu nhiều 😘",
]

struct HomeView: View {
    @EnvironmentObject var model: AppModel
    @State private var battery = Battery.Reading(level: nil, charging: false)
    @State private var now = nowMs()
    @State private var text = ""
    private let timer = Timer.publish(every: 30, on: .main, in: .common).autoconnect()

    var body: some View {
        let d = model.data
        let start = Dates.parse(d.startDate) ?? Date()
        ScrollView {
            VStack(spacing: 14) {
                counterCard(d, start: start)
                if !d.isPaired {
                    NavigationLink(destination: PairingView()) {
                        HStack(spacing: 12) {
                            Text("🔗").font(.system(size: 28))
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Kết nối với người ấy").font(.headline).foregroundColor(.loveText)
                                Text("Chia sẻ vị trí, pin, tâm trạng và nuôi thú cưng cùng nhau")
                                    .font(.caption).foregroundColor(.loveSub)
                                    .multilineTextAlignment(.leading)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.loveSub)
                        }
                        .padding(16)
                        .background(RoundedRectangle(cornerRadius: 20).fill(Color(red: 1.0, green: 0.94, blue: 0.79)))
                    }
                    .buttonStyle(.plain)
                }
                statusCard(d)
                missYouCard(d)
                VStack(alignment: .leading, spacing: 10) {
                    CardTitle("Tâm trạng của bạn hôm nay")
                    EmojiGrid(options: moodEmojis, selected: d.myStatus.mood) { model.setMood($0) }
                }
                .loveCard()
                let upcoming = Events.all(d).filter { $0.date >= Dates.startOfDay() }.prefix(3)
                if !upcoming.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        CardTitle("Sắp tới 🎉")
                        ForEach(Array(upcoming)) { ev in EventRow(event: ev) }
                    }
                    .loveCard()
                }
                messagesCard(d)
            }
            .padding(16)
        }
        .background(Gradients.soft.ignoresSafeArea())
        .navigationTitle("Love8")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            battery = Battery.read()
            now = nowMs()
        }
        .onReceive(timer) { _ in
            battery = Battery.read()
            now = nowMs()
        }
    }

    private func counterCard(_ d: AppData, start: Date) -> some View {
        VStack(spacing: 6) {
            HStack(spacing: 10) {
                VStack(spacing: 6) {
                    AvatarView(isMe: true, emoji: d.me.emoji, stamp: d.mePhotoStamp, size: 78)
                    Text(d.meName).font(.headline).foregroundColor(.white).lineLimit(1)
                }
                .frame(width: 100)
                BeatingHeart(size: 34)
                VStack(spacing: 6) {
                    AvatarView(isMe: false, emoji: d.partner.emoji, stamp: d.partnerPhotoStamp, size: 78)
                    Text(d.partnerName).font(.headline).foregroundColor(.white).lineLimit(1)
                }
                .frame(width: 100)
            }
            Text("Đã yêu nhau").font(.subheadline).foregroundColor(.white.opacity(0.9)).padding(.top, 8)
            HStack(alignment: .lastTextBaseline, spacing: 4) {
                Text("\(max(0, Dates.daysTogether(start)))")
                    .font(.system(size: 54, weight: .heavy, design: .rounded))
                    .foregroundColor(.white)
                Text("ngày").font(.title3).foregroundColor(.white)
            }
            Text(Dates.breakdown(start)).font(.subheadline.weight(.medium)).foregroundColor(.white)
            Text("Bắt đầu từ \(Dates.format(start))").font(.caption).foregroundColor(.white.opacity(0.8))
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(Gradients.love)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    private func statusCard(_ d: AppData) -> some View {
        let ps = d.partnerStatus
        let dist = distanceMeters(d.myStatus, ps)
        let updated: String
        if !d.isPaired {
            updated = "chưa kết nối"
        } else if let ps = ps {
            updated = "cập nhật " + Dates.relative(ps.ts, now: now)
        } else {
            updated = "đang chờ người ấy..."
        }
        return HStack {
            personStatus(name: d.meName, battery: battery.level, charging: battery.charging, mood: d.myStatus.mood)
            VStack(spacing: 2) {
                Text("📍").font(.system(size: 22))
                Text(dist.map { formatDistance($0) } ?? "-- km")
                    .font(.subheadline.weight(.bold))
                    .multilineTextAlignment(.center)
                Text(updated).font(.caption2).foregroundColor(.loveSub).multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            personStatus(name: d.partnerName, battery: ps?.battery, charging: ps?.charging ?? false, mood: ps?.mood)
        }
        .loveCard()
    }

    private func personStatus(name: String, battery: Int?, charging: Bool, mood: String?) -> some View {
        let low = (battery ?? 100) <= 20
        let level = battery.map { "\($0)" } ?? "--"
        return VStack(spacing: 4) {
            Text(name).font(.subheadline.weight(.semibold)).lineLimit(1)
            Text("🔋 \(level)%" + (charging ? " ⚡" : ""))
                .font(.subheadline)
                .foregroundColor(low ? .red : .loveSub)
            Text(mood ?? "😶").font(.system(size: 24))
        }
        .frame(width: 92)
    }

    private func missYouCard(_ d: AppData) -> some View {
        let startOfDay = Int(Dates.startOfDay().timeIntervalSince1970 * 1000)
        let missToday = d.messages.filter { $0.sender == d.deviceId && $0.kind == "miss_you" && $0.ts >= startOfDay }.count
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    CardTitle("Nhớ người ấy?")
                    Text("Hôm nay bạn đã nhớ \(missToday) lần 💕").font(.caption).foregroundColor(.loveSub)
                }
                Spacer()
                Button {
                    model.sendMessage(kind: "miss_you", text: "Nhớ bạn!")
                } label: {
                    Text("💗 Nhớ bạn")
                        .font(.headline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(Color.lovePink))
                }
            }
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 8)], alignment: .leading, spacing: 8) {
                ForEach(quickMessages, id: \.self) { q in
                    Button(q) { model.sendMessage(kind: "text", text: q) }
                        .buttonStyle(TagButtonStyle())
                }
            }
            HStack {
                TextField("Nhắn gì đó cho người ấy...", text: $text)
                    .padding(12)
                    .background(Capsule().stroke(Color.lovePinkLight, lineWidth: 1.5))
                Button {
                    let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !t.isEmpty else { return }
                    model.sendMessage(kind: "text", text: t)
                    text = ""
                } label: {
                    Image(systemName: "paperplane.fill").font(.title3).foregroundColor(.lovePink)
                }
            }
        }
        .loveCard()
    }

    private func messagesCard(_ d: AppData) -> some View {
        let recent = Array(d.messages.suffix(15).reversed())
        return VStack(alignment: .leading, spacing: 8) {
            CardTitle("Lời nhắn gần đây 💌")
            if recent.isEmpty {
                Text("Chưa có lời nhắn nào. Hãy gửi một lời yêu thương nhé!")
                    .font(.caption).foregroundColor(.loveSub)
            }
            ForEach(recent) { m in
                let mine = m.sender == d.deviceId
                let msgText: String = m.kind == "miss_you" ? "💗 Nhớ bạn!" : (m.kind == "heart" ? "💖" : m.text)
                HStack {
                    if mine { Spacer(minLength: 40) }
                    VStack(alignment: mine ? .trailing : .leading, spacing: 2) {
                        Text(msgText)
                            .foregroundColor(mine ? .white : .loveText)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(RoundedRectangle(cornerRadius: 18).fill(mine ? Color.lovePink : Color(red: 0.95, green: 0.93, blue: 0.94)))
                        Text(Dates.time(m.ts)).font(.caption2).foregroundColor(.loveSub)
                    }
                    if !mine { Spacer(minLength: 40) }
                }
            }
        }
        .loveCard()
    }
}
