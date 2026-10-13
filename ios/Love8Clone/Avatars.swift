import UIKit

/// Ảnh đại diện: ảnh do người dùng chọn hoặc emoji trên nền tròn.
enum Avatars {
    static let meBG = UIColor(red: 1.0, green: 0.84, blue: 0.89, alpha: 1)
    static let partnerBG = UIColor(red: 0.90, green: 0.85, blue: 1.0, alpha: 1)

    static func load(isMe: Bool) -> UIImage? {
        let url = SharedStore.avatarURL(isMe: isMe)
        guard FileManager.default.fileExists(atPath: url.path) else { return nil }
        return UIImage(contentsOfFile: url.path)
    }

    static func save(_ image: UIImage, isMe: Bool) -> Bool {
        let size: CGFloat = 512
        let scale = size / min(image.size.width, image.size.height)
        let w = image.size.width * scale
        let h = image.size.height * scale
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: size, height: size), format: format)
        let cropped = renderer.image { _ in
            image.draw(in: CGRect(x: (size - w) / 2, y: (size - h) / 2, width: w, height: h))
        }
        guard let jpg = cropped.jpegData(compressionQuality: 0.9) else { return false }
        do {
            try jpg.write(to: SharedStore.avatarURL(isMe: isMe), options: .atomic)
            return true
        } catch {
            return false
        }
    }

    static func delete(isMe: Bool) {
        try? FileManager.default.removeItem(at: SharedStore.avatarURL(isMe: isMe))
    }

    /// Ảnh tròn có viền trắng, dùng cho ghim trên bản đồ.
    static func circle(isMe: Bool, emoji: String, size: CGFloat) -> UIImage {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: size, height: size))
        return renderer.image { ctx in
            let rect = CGRect(x: 0, y: 0, width: size, height: size)
            let path = UIBezierPath(ovalIn: rect)
            if let photo = load(isMe: isMe) {
                ctx.cgContext.saveGState()
                path.addClip()
                photo.draw(in: rect)
                ctx.cgContext.restoreGState()
            } else {
                (isMe ? meBG : partnerBG).setFill()
                path.fill()
                let font = UIFont.systemFont(ofSize: size * 0.55)
                let str = NSAttributedString(string: emoji, attributes: [.font: font])
                let ts = str.size()
                str.draw(at: CGPoint(x: (size - ts.width) / 2, y: (size - ts.height) / 2))
            }
            UIColor.white.setStroke()
            let border = UIBezierPath(ovalIn: rect.insetBy(dx: size * 0.03, dy: size * 0.03))
            border.lineWidth = size * 0.06
            border.stroke()
        }
    }
}
