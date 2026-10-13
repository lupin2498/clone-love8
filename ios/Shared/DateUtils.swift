import Foundation

enum Dates {
    static let cal: Calendar = {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone.current
        return c
    }()

    private static let iso: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone.current
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    private static let display: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "vi_VN")
        f.timeZone = TimeZone.current
        f.dateFormat = "dd/MM/yyyy"
        return f
    }()

    private static let timeFmt: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "vi_VN")
        f.dateFormat = "HH:mm dd/MM"
        return f
    }()

    static func parse(_ s: String?) -> Date? {
        guard let s = s, !s.isEmpty else { return nil }
        return iso.date(from: s)
    }

    static func string(_ d: Date) -> String { iso.string(from: d) }

    static func format(_ d: Date) -> String { display.string(from: d) }

    static func time(_ ms: Int) -> String {
        timeFmt.string(from: Date(timeIntervalSince1970: TimeInterval(ms) / 1000))
    }

    static func startOfDay(_ d: Date = Date()) -> Date { cal.startOfDay(for: d) }

    static func daysBetween(_ a: Date, _ b: Date) -> Int {
        cal.dateComponents([.day], from: startOfDay(a), to: startOfDay(b)).day ?? 0
    }

    /// Ngày bắt đầu được tính là ngày thứ 1.
    static func daysTogether(_ start: Date, _ today: Date = Date()) -> Int {
        daysBetween(start, today) + 1
    }

    static func breakdown(_ start: Date, _ today: Date = Date()) -> String {
        if startOfDay(today) < startOfDay(start) { return "Sắp bắt đầu" }
        let c = cal.dateComponents([.year, .month, .day], from: startOfDay(start), to: startOfDay(today))
        var parts: [String] = []
        if let y = c.year, y > 0 { parts.append("\(y) năm") }
        if let m = c.month, m > 0 { parts.append("\(m) tháng") }
        parts.append("\(c.day ?? 0) ngày")
        return parts.joined(separator: " ")
    }

    static func addDays(_ d: Date, _ n: Int) -> Date {
        cal.date(byAdding: .day, value: n, to: startOfDay(d)) ?? d
    }

    static func addYears(_ d: Date, _ n: Int) -> Date {
        cal.date(byAdding: .year, value: n, to: startOfDay(d)) ?? d
    }

    static func nextOccurrence(_ date: Date, yearly: Bool, today: Date = Date()) -> Date {
        let t = startOfDay(today)
        if !yearly { return startOfDay(date) }
        let md = cal.dateComponents([.month, .day], from: date)
        let year = cal.component(.year, from: t)
        func make(_ y: Int) -> Date {
            var c = DateComponents()
            c.year = y
            c.month = md.month
            c.day = md.day
            return cal.date(from: c) ?? t
        }
        let d = make(year)
        return d < t ? make(year + 1) : d
    }

    static func ddayLabel(_ target: Date, _ today: Date = Date()) -> String {
        let diff = daysBetween(today, target)
        if diff == 0 { return "Hôm nay" }
        if diff > 0 { return "D-\(diff)" }
        return "D+\(-diff)"
    }

    static func relative(_ ts: Int, now: Int = nowMs()) -> String {
        if ts <= 0 { return "chưa có dữ liệu" }
        let s = (now - ts) / 1000
        if s < 60 { return "vừa xong" }
        if s < 3600 { return "\(s / 60) phút trước" }
        if s < 86400 { return "\(s / 3600) giờ trước" }
        return "\(s / 86400) ngày trước"
    }
}

struct UpcomingEvent: Identifiable, Equatable {
    var id: String
    var emoji: String
    var title: String
    var date: Date
    var remind: Bool = true
    var custom: SpecialDay? = nil
}

enum Events {
    private static let dayMarks: [Int] = (1...10).map { $0 * 100 } + (3...40).map { $0 * 500 }

    static func all(_ d: AppData, today: Date = Date(), milestoneCount: Int = 4) -> [UpcomingEvent] {
        let t = Dates.startOfDay(today)
        let start = Dates.parse(d.startDate) ?? t
        var out: [UpcomingEvent] = []

        let marks = dayMarks.map { ($0, Dates.addDays(start, $0 - 1)) }.filter { $0.1 >= t }.prefix(milestoneCount)
        for (m, date) in marks {
            out.append(UpcomingEvent(id: "m\(m)", emoji: "💯", title: "\(m) ngày yêu nhau", date: date))
        }
        let years = (1...60).map { ($0, Dates.addYears(start, $0)) }.filter { $0.1 >= t }.prefix(2)
        for (y, date) in years {
            out.append(UpcomingEvent(id: "y\(y)", emoji: "🎉", title: "Kỷ niệm \(y) năm yêu nhau", date: date))
        }
        if let b = Dates.parse(d.me.birthday) {
            out.append(UpcomingEvent(id: "bme", emoji: "🎂", title: "Sinh nhật \(d.meName)", date: Dates.nextOccurrence(b, yearly: true, today: t)))
        }
        if let b = Dates.parse(d.partner.birthday) {
            out.append(UpcomingEvent(id: "bpartner", emoji: "🎂", title: "Sinh nhật \(d.partnerName)", date: Dates.nextOccurrence(b, yearly: true, today: t)))
        }
        for sd in d.specialDays {
            guard let date = Dates.parse(sd.date) else { continue }
            out.append(UpcomingEvent(
                id: "c\(sd.id)", emoji: sd.emoji, title: sd.title,
                date: Dates.nextOccurrence(date, yearly: sd.yearly, today: t),
                remind: sd.remind, custom: sd
            ))
        }
        let upcoming = out.filter { $0.date >= t }.sorted { $0.date < $1.date }
        let past = out.filter { $0.date < t }.sorted { $0.date > $1.date }
        return upcoming + past
    }
}
