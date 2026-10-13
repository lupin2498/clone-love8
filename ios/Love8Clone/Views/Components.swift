import SwiftUI
import UIKit

extension Color {
    static let lovePink = Color(red: 1.0, green: 0.36, blue: 0.54)
    static let lovePinkLight = Color(red: 1.0, green: 0.84, blue: 0.89)
    static let loveCream = Color(red: 1.0, green: 0.965, blue: 0.973)
    static let loveSub = Color(red: 0.54, green: 0.39, blue: 0.44)
    static let loveText = Color(red: 0.24, green: 0.10, blue: 0.15)
    static let lovePurpleLight = Color(red: 0.90, green: 0.85, blue: 1.0)
}

enum Gradients {
    static let love = LinearGradient(
        colors: [Color(red: 1.0, green: 0.62, blue: 0.73), .lovePink],
        startPoint: .topLeading, endPoint: .bottomTrailing
    )
    static let soft = LinearGradient(
        colors: [Color(red: 1.0, green: 0.89, blue: 0.93), .loveCream],
        startPoint: .top, endPoint: .bottom
    )
    static let sky = LinearGradient(
        colors: [Color(red: 0.87, green: 0.92, blue: 1.0), Color(red: 1.0, green: 0.91, blue: 0.95)],
        startPoint: .top, endPoint: .bottom
    )
    static let premium = LinearGradient(
        colors: [Color(red: 1.0, green: 0.76, blue: 0.44), Color(red: 1.0, green: 0.37, blue: 0.43)],
        startPoint: .topLeading, endPoint: .bottomTrailing
    )
}

struct LoveCardModifier: ViewModifier {
    var padding: CGFloat

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .shadow(color: Color.black.opacity(0.06), radius: 6, x: 0, y: 2)
    }
}

extension View {
    func loveCard(padding: CGFloat = 16) -> some View {
        modifier(LoveCardModifier(padding: padding))
    }
}

let avatarEmojis = ["🙂", "😊", "😎", "🥰", "😺", "🐶", "🐰", "🐻", "🦊", "🐼", "🌸", "🌻", "⭐", "💖", "👦", "👧", "👨", "👩", "🧑", "👸", "🤴", "🐧"]
let moodEmojis = ["😊", "🥰", "😍", "😘", "😴", "😢", "😡", "🤒", "😎", "🤩", "🥺", "😋"]

/// Bộ nhớ đệm ảnh đại diện theo "stamp" để không đọc đĩa mỗi lần vẽ lại.
enum AvatarCache {
    private static var cache: [Bool: (Int, UIImage?)] = [:]

    static func image(isMe: Bool, stamp: Int) -> UIImage? {
        if let hit = cache[isMe], hit.0 == stamp { return hit.1 }
        let img = Avatars.load(isMe: isMe)
        cache[isMe] = (stamp, img)
        return img
    }
}

struct AvatarView: View {
    let isMe: Bool
    let emoji: String
    let stamp: Int
    var size: CGFloat = 72

    var body: some View {
        ZStack {
            Circle().fill(isMe ? Color.lovePinkLight : Color.lovePurpleLight)
            if let img = AvatarCache.image(isMe: isMe, stamp: stamp) {
                Image(uiImage: img)
                    .resizable()
                    .scaledToFill()
            } else {
                Text(emoji).font(.system(size: size * 0.5))
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color.white, lineWidth: 3))
    }
}

struct EmojiGrid: View {
    let options: [String]
    let selected: String?
    let onSelect: (String) -> Void

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 44), spacing: 8)], spacing: 8) {
            ForEach(options, id: \.self) { e in
                let isSel = e == selected
                Text(e)
                    .font(.system(size: 22))
                    .frame(width: 44, height: 44)
                    .background(Circle().fill(isSel ? Color.lovePink.opacity(0.22) : Color.white))
                    .overlay(Circle().stroke(isSel ? Color.lovePink : Color(red: 0.94, green: 0.84, blue: 0.87), lineWidth: isSel ? 2 : 1))
                    .onTapGesture { onSelect(e) }
            }
        }
    }
}

struct StatBar: View {
    let label: String
    let value: Double
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label).font(.subheadline.weight(.medium))
                Spacer()
                Text("\(Int(value.rounded()))%").font(.caption).foregroundColor(.loveSub)
            }
            ProgressView(value: min(max(value, 0), 100), total: 100)
                .tint(color)
        }
        .padding(.vertical, 3)
    }
}

struct BeatingHeart: View {
    var size: CGFloat = 30
    var emoji: String = "💗"
    @State private var beat = false

    var body: some View {
        Text(emoji)
            .font(.system(size: size))
            .scaleEffect(beat ? 1.25 : 1.0)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.65).repeatForever(autoreverses: true)) {
                    beat = true
                }
            }
    }
}

struct CardTitle: View {
    let text: String

    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text).font(.headline).foregroundColor(.loveText)
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    var enabled: Bool = true

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Capsule().fill(enabled ? Color.lovePink : Color.gray.opacity(0.4)))
            .opacity(configuration.isPressed ? 0.8 : 1)
    }
}

struct TagButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline)
            .foregroundColor(.loveText)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Capsule().fill(Color.lovePinkLight.opacity(configuration.isPressed ? 1 : 0.6)))
    }
}

struct EventRow: View {
    let event: UpcomingEvent
    var onDelete: (() -> Void)? = nil

    var body: some View {
        let diff = Dates.daysBetween(Date(), event.date)
        HStack(spacing: 12) {
            Text(event.emoji)
                .font(.system(size: 22))
                .frame(width: 44, height: 44)
                .background(Circle().fill(Color.lovePinkLight))
            VStack(alignment: .leading, spacing: 2) {
                Text(event.title).font(.subheadline.weight(.semibold)).lineLimit(2)
                Text(Dates.format(event.date)).font(.caption).foregroundColor(.loveSub)
            }
            Spacer()
            Text(Dates.ddayLabel(event.date))
                .font(.subheadline.weight(.bold))
                .foregroundColor(diff == 0 ? .white : .lovePink)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(Capsule().fill(diff == 0 ? Color.lovePink : Color.lovePinkLight))
            if let onDelete = onDelete {
                Button(action: onDelete) {
                    Image(systemName: "trash").foregroundColor(.loveSub)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.vertical, 4)
    }
}

struct ToastView: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.subheadline.weight(.medium))
            .foregroundColor(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .background(Capsule().fill(Color.black.opacity(0.78)))
            .padding(.horizontal, 24)
    }
}
