import CryptoKit
import Foundation

/// Mã hóa đầu-cuối giống hệt bản Android: AES-256-GCM, khóa = SHA-256("love8clone-key:" + mã).
enum LoveCrypto {
    static let prefix = "L8C1:"
    private static let alphabet = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")

    static func newCode() -> String {
        String((0..<8).map { _ in alphabet.randomElement() ?? "A" })
    }

    static func normalize(_ code: String) -> String {
        String(code.uppercased().filter { $0.isLetter || $0.isNumber })
    }

    static func pretty(_ code: String) -> String {
        code.count == 8 ? "\(code.prefix(4))-\(code.suffix(4))" : code
    }

    private static func sha(_ s: String) -> Data {
        Data(SHA256.hash(data: Data(s.utf8)))
    }

    static func topic(_ code: String) -> String {
        let bytes = [UInt8](sha("love8clone-topic:" + normalize(code)))
        return "l8c_" + bytes.prefix(12).map { String(format: "%02x", $0) }.joined()
    }

    private static func key(_ code: String) -> SymmetricKey {
        SymmetricKey(data: sha("love8clone-key:" + normalize(code)))
    }

    static func encrypt(_ code: String, _ plain: Data) -> String? {
        guard let box = try? AES.GCM.seal(plain, using: key(code)), let combined = box.combined else { return nil }
        return prefix + combined.base64EncodedString()
    }

    static func decrypt(_ code: String, _ payload: String) -> Data? {
        guard payload.hasPrefix(prefix),
              let raw = Data(base64Encoded: String(payload.dropFirst(prefix.count))),
              raw.count > 28,
              let box = try? AES.GCM.SealedBox(combined: raw) else { return nil }
        return try? AES.GCM.open(box, using: key(code))
    }
}

struct NtfyMessage {
    let id: String
    let time: Int
    let message: String
}

/// Client tối giản cho máy chủ ntfy.
enum Ntfy {
    private static func base(_ server: String) -> String {
        var s = server.trimmingCharacters(in: .whitespacesAndNewlines)
        while s.hasSuffix("/") { s.removeLast() }
        return s
    }

    static func publish(server: String, topic: String, body: String) async -> Bool {
        guard let url = URL(string: "\(base(server))/\(topic)") else { return false }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.httpBody = Data(body.utf8)
        req.timeoutInterval = 15
        req.setValue("text/plain; charset=utf-8", forHTTPHeaderField: "Content-Type")
        do {
            let (_, resp) = try await URLSession.shared.data(for: req)
            let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
            return (200..<300).contains(code)
        } catch {
            return false
        }
    }

    static func poll(server: String, topic: String, since: String) async -> [NtfyMessage]? {
        guard var comps = URLComponents(string: "\(base(server))/\(topic)/json") else { return nil }
        comps.queryItems = [URLQueryItem(name: "poll", value: "1"), URLQueryItem(name: "since", value: since)]
        guard let url = comps.url else { return nil }
        var req = URLRequest(url: url)
        req.timeoutInterval = 20
        do {
            let (data, resp) = try await URLSession.shared.data(for: req)
            let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
            guard (200..<300).contains(code) else { return nil }
            let text = String(decoding: data, as: UTF8.self)
            var out: [NtfyMessage] = []
            for line in text.split(separator: "\n") {
                guard let obj = try? JSONSerialization.jsonObject(with: Data(line.utf8)) as? [String: Any],
                      (obj["event"] as? String) == "message",
                      let id = obj["id"] as? String else { continue }
                let time = (obj["time"] as? NSNumber)?.intValue ?? 0
                out.append(NtfyMessage(id: id, time: time, message: (obj["message"] as? String) ?? ""))
            }
            return out
        } catch {
            return nil
        }
    }

    static func ping(_ server: String) async -> Bool {
        guard let url = URL(string: "\(base(server))/v1/health") else { return false }
        var req = URLRequest(url: url)
        req.timeoutInterval = 10
        do {
            let (_, resp) = try await URLSession.shared.data(for: req)
            let code = (resp as? HTTPURLResponse)?.statusCode ?? 0
            return (200..<300).contains(code)
        } catch {
            return false
        }
    }
}

/// Quy tắc hợp nhất dữ liệu nhận từ người ấy (giống bản Android).
enum Merge {
    static func apply(_ d: inout AppData, _ e: SyncEnvelope) {
        d.pairing.partnerDevice = e.from
        if let p = e.profile {
            if !p.name.isEmpty { d.partner.name = p.name }
            if !p.emoji.isEmpty { d.partner.emoji = p.emoji }
            if let b = p.birthday { d.partner.birthday = b }
            if let s = p.startDate, p.startDateUpdatedAt > d.startDateUpdatedAt {
                d.startDate = s
                d.startDateUpdatedAt = p.startDateUpdatedAt
            }
        }
        if let s = e.status, s.ts >= (d.partnerStatus?.ts ?? 0) {
            d.partnerStatus = s
        }
        if let m = e.message, !d.messages.contains(where: { $0.id == m.id }) {
            d.messages = Array((d.messages + [m]).sorted { $0.ts < $1.ts }.suffix(200))
        }
        if let pet = e.pet, pet.updatedAt > d.pet.updatedAt {
            d.pet = pet
        }
        if let sd = e.specialDay, !d.deletedIds.contains(sd.id) {
            d.specialDays = d.specialDays.filter { $0.id != sd.id } + [sd]
        }
        if let de = e.diary, !d.deletedIds.contains(de.id), !d.diary.contains(where: { $0.id == de.id }) {
            d.diary = (d.diary + [de]).sorted { $0.ts > $1.ts }
        }
        if let del = e.deleteId {
            d.specialDays.removeAll { $0.id == del }
            d.diary.removeAll { $0.id == del }
            d.deletedIds = Array((d.deletedIds + [del]).suffix(300))
        }
    }
}
