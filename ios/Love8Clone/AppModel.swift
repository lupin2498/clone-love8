import BackgroundTasks
import CoreLocation
import Foundation
import SwiftUI
import WidgetKit

/// Trạng thái toàn app + các thao tác của người dùng (giống lớp Actions bên Android).
@MainActor
final class AppModel: ObservableObject {
    static let shared = AppModel()
    static let refreshTaskId = "com.clone.love8.refresh"

    @Published private(set) var data: AppData
    @Published var toast: String?
    let location = LocationService()

    private var pulling = false
    private var loopTask: Task<Void, Never>?
    private var lastWidgetReload = Date.distantPast

    init() {
        var d = SharedStore.load()
        if d.deviceId.isEmpty { d.deviceId = UUID().uuidString }
        data = d
        SharedStore.save(d)
        location.onUpdate = { [weak self] loc in
            Task { @MainActor in self?.updateMyLocation(loc) }
        }
    }

    // MARK: - Lưu trữ

    func update(_ change: (inout AppData) -> Void) {
        var d = data
        change(&d)
        guard d != data else { return }
        data = d
        SharedStore.save(d)
        if Date().timeIntervalSince(lastWidgetReload) > 20 {
            reloadWidgets()
        }
    }

    func reloadWidgets() {
        lastWidgetReload = Date()
        WidgetCenter.shared.reloadAllTimelines()
    }

    func show(_ message: String) {
        toast = message
        let current = message
        Task {
            try? await Task.sleep(nanoseconds: 2_500_000_000)
            if self.toast == current { self.toast = nil }
        }
    }

    private func newId() -> String {
        String(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased().prefix(16))
    }

    // MARK: - Hồ sơ

    func finishOnboarding(me: Profile, partner: Profile, start: Date) {
        update {
            $0.onboarded = true
            $0.me = me
            $0.partner = partner
            $0.startDate = Dates.string(start)
            $0.startDateUpdatedAt = nowMs()
        }
        reloadWidgets()
        Notify.requestPermission()
        Notify.reschedule(data)
    }

    func updateMe(_ p: Profile) {
        update { $0.me = p }
        sendAsync(profileEnvelope(reply: false))
        reloadWidgets()
        Notify.reschedule(data)
    }

    func updatePartner(_ p: Profile) {
        update { $0.partner = p }
        reloadWidgets()
        Notify.reschedule(data)
    }

    func setStartDate(_ date: Date) {
        update {
            $0.startDate = Dates.string(date)
            $0.startDateUpdatedAt = nowMs()
        }
        sendAsync(profileEnvelope(reply: false))
        reloadWidgets()
        Notify.reschedule(data)
    }

    func setPhoto(_ image: UIImage?, isMe: Bool) {
        if let image = image {
            if !Avatars.save(image, isMe: isMe) {
                show("Không lưu được ảnh")
                return
            }
        } else {
            Avatars.delete(isMe: isMe)
        }
        let stamp = nowMs()
        update {
            if isMe { $0.mePhotoStamp = stamp } else { $0.partnerPhotoStamp = stamp }
        }
        reloadWidgets()
    }

    func updateSettings(_ change: (inout AppSettings) -> Void) {
        update { change(&$0.settings) }
        Notify.reschedule(data)
    }

    // MARK: - Lời nhắn & tâm trạng

    func sendMessage(kind: String, text: String) {
        let m = LoveMessage(id: newId(), sender: data.deviceId, kind: kind, text: String(text.prefix(500)), ts: nowMs())
        update { $0.messages = Array(($0.messages + [m]).suffix(200)) }
        sendAsync(SyncEnvelope(type: "message", message: m))
        show(data.isPaired ? "Đã gửi tới \(data.partnerName) 💗" : "Đã lưu. Hãy kết nối với người ấy để gửi tin nhắn!")
    }

    func setMood(_ mood: String) {
        update { $0.myStatus.mood = mood }
        Task { await sendStatus(force: true) }
        show("Đã chia sẻ tâm trạng \(mood)")
    }

    // MARK: - Thú cưng

    func adoptPet(type: String, name: String) {
        let now = nowMs()
        let petName = name.isEmpty ? "Mochi" : name
        var pet = PetState()
        pet.adopted = true
        pet.type = type
        pet.name = petName
        pet.lastTick = now
        pet.updatedAt = now
        pet.log = [PetLog(ts: now, text: "\(data.meName) đã nhận nuôi \(petName) 🎉")]
        update { $0.pet = pet }
        syncPet(pet)
        show("Chào mừng \(petName) về nhà! 🎉")
    }

    @discardableResult
    func petAction(_ action: PetLogic.Action) -> String {
        let res = PetLogic.perform(data.pet, action, actor: data.meName)
        if res.ok {
            update {
                $0.pet = res.pet
                $0.coins += res.coins
            }
            if action != .pat { syncPet(res.pet) }
        }
        return res.message
    }

    func renamePet(name: String, type: String) {
        let now = nowMs()
        var pet = PetLogic.tick(data.pet, now: now)
        pet.name = name.isEmpty ? "Mochi" : name
        pet.type = type
        pet.updatedAt = now
        update { $0.pet = pet }
        syncPet(pet)
    }

    func accessory(_ id: String) {
        guard let acc = PetLogic.accessories.first(where: { $0.id == id }) else { return }
        let now = nowMs()
        var pet = PetLogic.tick(data.pet, now: now)
        if pet.accessory == id {
            pet.accessory = nil
            show("Đã tháo \(acc.label)")
        } else if pet.ownedAccessories.contains(id) {
            pet.accessory = id
            show("\(pet.name) đã đeo \(acc.label) \(acc.emoji)")
        } else if data.coins < acc.price {
            show("Không đủ xu 🪙 (cần \(acc.price))")
            return
        } else {
            pet.accessory = id
            pet.ownedAccessories.append(id)
            pet.happiness = min(100, pet.happiness + 10)
            update { $0.coins -= acc.price }
            show("Đã mua \(acc.label) \(acc.emoji) cho \(pet.name)!")
        }
        pet.updatedAt = now
        update { $0.pet = pet }
        syncPet(pet)
    }

    func dailyCheckIn() {
        let today = Dates.string(Date())
        guard data.settings.lastCheckInDay != today else { return }
        update {
            $0.coins += 20
            $0.settings.lastCheckInDay = today
        }
        show("Điểm danh thành công +20 xu 🪙")
    }

    private func syncPet(_ pet: PetState) {
        var p = pet
        p.log = Array(p.log.prefix(8))
        sendAsync(SyncEnvelope(type: "pet", pet: p))
        reloadWidgets()
        Notify.reschedule(data)
    }

    // MARK: - Ngày đặc biệt & nhật ký

    func addSpecialDay(title: String, date: Date, emoji: String, yearly: Bool, remind: Bool) {
        let sd = SpecialDay(
            id: newId(), title: String(title.trimmingCharacters(in: .whitespaces).prefix(60)),
            date: Dates.string(date), emoji: emoji, yearly: yearly, remind: remind, createdBy: data.deviceId
        )
        update { $0.specialDays.append(sd) }
        sendAsync(SyncEnvelope(type: "special_day", specialDay: sd))
        Notify.reschedule(data)
        show("Đã thêm \"\(sd.title)\" \(emoji)")
    }

    func deleteItem(_ id: String) {
        update {
            $0.specialDays.removeAll { $0.id == id }
            $0.diary.removeAll { $0.id == id }
            $0.deletedIds = Array(($0.deletedIds + [id]).suffix(300))
        }
        sendAsync(SyncEnvelope(type: "delete", deleteId: id))
        Notify.reschedule(data)
    }

    func addDiary(mood: String, text: String) {
        let e = DiaryEntry(
            id: newId(), author: data.deviceId, mood: mood,
            text: String(text.trimmingCharacters(in: .whitespacesAndNewlines).prefix(1000)), ts: nowMs()
        )
        update { $0.diary.insert(e, at: 0) }
        sendAsync(SyncEnvelope(type: "diary", diary: e))
    }

    // MARK: - Kết nối

    func createPairing() {
        connect(LoveCrypto.newCode())
        show("Đã tạo mã. Hãy gửi cho người ấy nhé!")
    }

    @discardableResult
    func joinPairing(_ raw: String) -> Bool {
        let code = LoveCrypto.normalize(raw)
        guard code.count == 8 else {
            show("Mã không hợp lệ")
            return false
        }
        connect(code)
        show("Đang kết nối...")
        return true
    }

    private func connect(_ code: String) {
        update {
            $0.pairing.code = code
            $0.pairing.since = nil
            $0.pairing.partnerDevice = nil
            $0.pairing.lastStatusSentAt = 0
            $0.partnerStatus = nil
        }
        Task {
            _ = await send(profileEnvelope(reply: true))
            await pull()
            await sendStatus(force: true)
        }
    }

    func setServer(_ server: String) {
        var s = server.trimmingCharacters(in: .whitespacesAndNewlines)
        while s.hasSuffix("/") { s.removeLast() }
        let value = s.isEmpty ? defaultServer : s
        update {
            $0.pairing.server = value
            $0.pairing.since = nil
        }
        show("Đã lưu máy chủ")
    }

    func unpair() {
        update {
            $0.pairing = Pairing(server: $0.pairing.server)
            $0.partnerStatus = nil
        }
        reloadWidgets()
    }

    func resetAll() {
        Avatars.delete(isMe: true)
        Avatars.delete(isMe: false)
        var fresh = AppData()
        fresh.deviceId = UUID().uuidString
        data = fresh
        SharedStore.save(fresh)
        reloadWidgets()
    }

    // MARK: - Đồng bộ

    func profileEnvelope(reply: Bool) -> SyncEnvelope {
        SyncEnvelope(
            type: "profile",
            profile: ProfilePayload(
                name: data.me.name, emoji: data.me.emoji, birthday: data.me.birthday,
                startDate: data.startDate, startDateUpdatedAt: data.startDateUpdatedAt, reply: reply
            )
        )
    }

    func send(_ env: SyncEnvelope) async -> Bool {
        guard let code = data.pairing.code else { return false }
        var e = env
        e.from = data.deviceId
        e.ts = nowMs()
        guard let json = try? JSONEncoder().encode(e), let body = LoveCrypto.encrypt(code, json) else { return false }
        return await Ntfy.publish(server: data.pairing.server, topic: LoveCrypto.topic(code), body: body)
    }

    func sendAsync(_ env: SyncEnvelope) {
        guard data.isPaired else { return }
        Task { _ = await send(env) }
    }

    /// Lấy tin mới từ máy chủ, hợp nhất và trả về các sự kiện của người ấy.
    @discardableResult
    func pull() async -> [SyncEnvelope] {
        guard !pulling, let code = data.pairing.code else { return [] }
        pulling = true
        defer { pulling = false }
        let server = data.pairing.server
        let since = data.pairing.since ?? "all"
        guard let msgs = await Ntfy.poll(server: server, topic: LoveCrypto.topic(code), since: since),
              let lastId = msgs.last?.id else { return [] }
        let me = data.deviceId
        let decoder = JSONDecoder()
        let events: [SyncEnvelope] = msgs.compactMap { m in
            guard let plain = LoveCrypto.decrypt(code, m.message),
                  let e = try? decoder.decode(SyncEnvelope.self, from: plain),
                  !e.from.isEmpty, e.from != me else { return nil }
            return e
        }
        guard data.pairing.code == code else { return [] }
        update { d in
            d.pairing.since = lastId
            for e in events { Merge.apply(&d, e) }
        }
        if events.contains(where: { $0.profile?.reply == true }) {
            _ = await send(profileEnvelope(reply: false))
            if data.pet.adopted {
                var p = data.pet
                p.log = Array(p.log.prefix(8))
                _ = await send(SyncEnvelope(type: "pet", pet: p))
            }
            for sd in data.specialDays.filter({ $0.createdBy == data.deviceId }).prefix(10) {
                _ = await send(SyncEnvelope(type: "special_day", specialDay: sd))
            }
            await sendStatus(force: true)
        }
        if !events.isEmpty {
            reloadWidgets()
            if events.contains(where: { $0.pet != nil || $0.specialDay != nil || $0.deleteId != nil || $0.profile != nil }) {
                Notify.reschedule(data)
            }
        }
        return events
    }

    private func updateMyLocation(_ loc: CLLocation) {
        update {
            $0.myStatus.lat = loc.coordinate.latitude
            $0.myStatus.lon = loc.coordinate.longitude
            $0.myStatus.accuracy = loc.horizontalAccuracy >= 0 ? loc.horizontalAccuracy : nil
            $0.myStatus.speed = loc.speed >= 0 ? loc.speed : nil
            $0.myStatus.locationTs = nowMs()
        }
    }

    /// Gửi pin / vị trí nếu thay đổi đáng kể hoặc đã lâu chưa gửi.
    func sendStatus(force: Bool = false) async {
        let prev = data.myStatus
        var st = DeviceStatus()
        if data.settings.shareBattery {
            let b = Battery.read()
            st.battery = b.level
            st.charging = b.charging
        }
        if data.settings.shareLocation {
            if let loc = location.lastLocation, Int(loc.timestamp.timeIntervalSince1970 * 1000) >= (prev.locationTs ?? 0) {
                st.lat = loc.coordinate.latitude
                st.lon = loc.coordinate.longitude
                st.accuracy = loc.horizontalAccuracy >= 0 ? loc.horizontalAccuracy : nil
                st.speed = loc.speed >= 0 ? loc.speed : nil
                st.locationTs = Int(loc.timestamp.timeIntervalSince1970 * 1000)
            } else {
                st.lat = prev.lat
                st.lon = prev.lon
                st.accuracy = prev.accuracy
                st.speed = prev.speed
                st.locationTs = prev.locationTs
            }
        }
        st.mood = prev.mood
        st.ts = nowMs()
        update { $0.myStatus = st }
        guard data.isPaired else { return }
        let now = nowMs()
        let moved = (distanceMeters(prev, st) ?? 0) > 150
        let battChanged = abs((prev.battery ?? -100) - (st.battery ?? -100)) >= 5 || prev.charging != st.charging
        let due = now - data.pairing.lastStatusSentAt >= 10 * 60_000
        guard force || due || moved || battChanged else { return }
        if await send(SyncEnvelope(type: "status", status: st)) {
            update { $0.pairing.lastStatusSentAt = now }
        }
    }

    // MARK: - Vòng đồng bộ & chạy nền

    func becameActive() {
        location.start()
        loopTask?.cancel()
        loopTask = Task { [weak self] in
            await self?.sendStatus(force: true)
            var round = 0
            while !Task.isCancelled {
                guard let self = self else { return }
                let events = await self.pull()
                for e in events { self.toastFor(e) }
                round += 1
                if round % 12 == 0 { await self.sendStatus() }
                try? await Task.sleep(nanoseconds: 8_000_000_000)
            }
        }
        Notify.reschedule(data)
    }

    func enteredBackground() {
        loopTask?.cancel()
        loopTask = nil
        location.stop()
        reloadWidgets()
        Notify.reschedule(data)
        scheduleBackgroundRefresh()
    }

    private func toastFor(_ e: SyncEnvelope) {
        let partner = data.partnerName
        if let m = e.message {
            switch m.kind {
            case "miss_you": show("💗 \(partner) đang nhớ bạn!")
            case "heart": show("💖 \(partner) gửi bạn một trái tim")
            default: show("💬 \(partner): \(m.text)")
            }
        } else if let pet = e.pet, let last = pet.log.first?.text {
            show(last)
        } else if let sd = e.specialDay {
            show("📅 \(partner) đã thêm \"\(sd.title)\"")
        } else if e.diary != nil {
            show("📔 \(partner) vừa viết nhật ký mới")
        }
    }

    func scheduleBackgroundRefresh() {
        let req = BGAppRefreshTaskRequest(identifier: AppModel.refreshTaskId)
        req.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(req)
    }

    /// Chạy khi iOS đánh thức app ở chế độ nền.
    func backgroundRefresh() async {
        scheduleBackgroundRefresh()
        let events = await pull()
        await sendStatus()
        Notify.incoming(events, data: data)
        Notify.reschedule(data)
        reloadWidgets()
    }
}
