import Foundation

/// Lưu dữ liệu vào App Group (để widget đọc được); nếu không có App Group thì dùng thư mục của app.
enum SharedStore {
    static let appGroup = "group.com.clone.love8"

    static var directory: URL {
        if let g = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroup) {
            return g
        }
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    static var dataURL: URL { directory.appendingPathComponent("love8_data.json") }

    static func avatarURL(isMe: Bool) -> URL {
        directory.appendingPathComponent(isMe ? "avatar_me.jpg" : "avatar_partner.jpg")
    }

    static func load() -> AppData {
        guard let raw = try? Data(contentsOf: dataURL),
              let data = try? JSONDecoder().decode(AppData.self, from: raw) else {
            return AppData()
        }
        return data
    }

    static func save(_ data: AppData) {
        guard let raw = try? JSONEncoder().encode(data) else { return }
        try? raw.write(to: dataURL, options: .atomic)
    }
}
