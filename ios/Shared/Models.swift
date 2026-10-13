import Foundation

let defaultServer = "https://ntfy.sh"

func nowMs() -> Int { Int(Date().timeIntervalSince1970 * 1000) }

// Giải mã "dễ tính": thiếu khóa thì dùng giá trị mặc định (tương thích với bản Android).
extension KeyedDecodingContainer {
    func v<T: Decodable>(_ key: Key, _ def: T) -> T {
        (try? decodeIfPresent(T.self, forKey: key)) ?? def
    }

    func o<T: Decodable>(_ key: Key, _ type: T.Type) -> T? {
        (try? decodeIfPresent(T.self, forKey: key)) ?? nil
    }
}

struct Profile: Codable, Equatable {
    var name: String = ""
    var emoji: String = "🙂"
    var birthday: String? = nil
}

extension Profile {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        name = c.v(.name, "")
        emoji = c.v(.emoji, "🙂")
        birthday = c.o(.birthday, String.self)
    }
}

struct SpecialDay: Codable, Equatable, Identifiable {
    var id: String
    var title: String
    var date: String
    var emoji: String = "💝"
    var yearly: Bool = true
    var remind: Bool = true
    var createdBy: String = ""
}

extension SpecialDay {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = c.v(.id, UUID().uuidString)
        title = c.v(.title, "")
        date = c.v(.date, "")
        emoji = c.v(.emoji, "💝")
        yearly = c.v(.yearly, true)
        remind = c.v(.remind, true)
        createdBy = c.v(.createdBy, "")
    }
}

struct PetLog: Codable, Equatable {
    var ts: Int
    var text: String
}

extension PetLog {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        ts = c.v(.ts, 0)
        text = c.v(.text, "")
    }
}

struct PetState: Codable, Equatable {
    var adopted: Bool = false
    var type: String = "cat"
    var name: String = "Mochi"
    var hunger: Double = 80
    var happiness: Double = 80
    var energy: Double = 80
    var cleanliness: Double = 80
    var xp: Int = 0
    var level: Int = 1
    var sleeping: Bool = false
    var accessory: String? = nil
    var ownedAccessories: [String] = []
    var lastTick: Int = 0
    var updatedAt: Int = 0
    var log: [PetLog] = []
}

extension PetState {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        adopted = c.v(.adopted, false)
        type = c.v(.type, "cat")
        name = c.v(.name, "Mochi")
        hunger = c.v(.hunger, 80.0)
        happiness = c.v(.happiness, 80.0)
        energy = c.v(.energy, 80.0)
        cleanliness = c.v(.cleanliness, 80.0)
        xp = c.v(.xp, 0)
        level = c.v(.level, 1)
        sleeping = c.v(.sleeping, false)
        accessory = c.o(.accessory, String.self)
        ownedAccessories = c.v(.ownedAccessories, [String]())
        lastTick = c.v(.lastTick, 0)
        updatedAt = c.v(.updatedAt, 0)
        log = c.v(.log, [PetLog]())
    }
}

struct LoveMessage: Codable, Equatable, Identifiable {
    var id: String
    var sender: String
    var kind: String = "text"
    var text: String = ""
    var ts: Int
}

extension LoveMessage {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = c.v(.id, UUID().uuidString)
        sender = c.v(.sender, "")
        kind = c.v(.kind, "text")
        text = c.v(.text, "")
        ts = c.v(.ts, 0)
    }
}

struct DiaryEntry: Codable, Equatable, Identifiable {
    var id: String
    var author: String
    var mood: String = "😊"
    var text: String
    var ts: Int
}

extension DiaryEntry {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = c.v(.id, UUID().uuidString)
        author = c.v(.author, "")
        mood = c.v(.mood, "😊")
        text = c.v(.text, "")
        ts = c.v(.ts, 0)
    }
}

struct DeviceStatus: Codable, Equatable {
    var battery: Int? = nil
    var charging: Bool = false
    var lat: Double? = nil
    var lon: Double? = nil
    var accuracy: Double? = nil
    var speed: Double? = nil
    var locationTs: Int? = nil
    var mood: String? = nil
    var ts: Int = 0
}

extension DeviceStatus {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        battery = c.o(.battery, Int.self)
        charging = c.v(.charging, false)
        lat = c.o(.lat, Double.self)
        lon = c.o(.lon, Double.self)
        accuracy = c.o(.accuracy, Double.self)
        speed = c.o(.speed, Double.self)
        locationTs = c.o(.locationTs, Int.self)
        mood = c.o(.mood, String.self)
        ts = c.v(.ts, 0)
    }
}

struct Pairing: Codable, Equatable {
    var code: String? = nil
    var server: String = defaultServer
    var since: String? = nil
    var partnerDevice: String? = nil
    var lastStatusSentAt: Int = 0
}

extension Pairing {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        code = c.o(.code, String.self)
        server = c.v(.server, defaultServer)
        since = c.o(.since, String.self)
        partnerDevice = c.o(.partnerDevice, String.self)
        lastStatusSentAt = c.v(.lastStatusSentAt, 0)
    }
}

struct AppSettings: Codable, Equatable {
    var shareLocation: Bool = true
    var shareBattery: Bool = true
    var notifyMessages: Bool = true
    var notifyPet: Bool = true
    var notifySpecialDays: Bool = true
    var lastCheckInDay: String? = nil
}

extension AppSettings {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        shareLocation = c.v(.shareLocation, true)
        shareBattery = c.v(.shareBattery, true)
        notifyMessages = c.v(.notifyMessages, true)
        notifyPet = c.v(.notifyPet, true)
        notifySpecialDays = c.v(.notifySpecialDays, true)
        lastCheckInDay = c.o(.lastCheckInDay, String.self)
    }
}

struct AppData: Codable, Equatable {
    var deviceId: String = ""
    var onboarded: Bool = false
    var me: Profile = Profile()
    var partner: Profile = Profile(name: "Người ấy", emoji: "💖")
    var mePhotoStamp: Int = 0
    var partnerPhotoStamp: Int = 0
    var startDate: String = Dates.string(Date())
    var startDateUpdatedAt: Int = 0
    var specialDays: [SpecialDay] = []
    var deletedIds: [String] = []
    var pet: PetState = PetState()
    var coins: Int = 100
    var messages: [LoveMessage] = []
    var diary: [DiaryEntry] = []
    var myStatus: DeviceStatus = DeviceStatus()
    var partnerStatus: DeviceStatus? = nil
    var pairing: Pairing = Pairing()
    var settings: AppSettings = AppSettings()

    var isPaired: Bool { pairing.code != nil }
    var partnerConnected: Bool { pairing.code != nil && pairing.partnerDevice != nil }
    var meName: String { me.name.isEmpty ? "Bạn" : me.name }
    var partnerName: String { partner.name.isEmpty ? "Người ấy" : partner.name }
}

extension AppData {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        deviceId = c.v(.deviceId, "")
        onboarded = c.v(.onboarded, false)
        me = c.v(.me, Profile())
        partner = c.v(.partner, Profile(name: "Người ấy", emoji: "💖"))
        mePhotoStamp = c.v(.mePhotoStamp, 0)
        partnerPhotoStamp = c.v(.partnerPhotoStamp, 0)
        startDate = c.v(.startDate, Dates.string(Date()))
        startDateUpdatedAt = c.v(.startDateUpdatedAt, 0)
        specialDays = c.v(.specialDays, [SpecialDay]())
        deletedIds = c.v(.deletedIds, [String]())
        pet = c.v(.pet, PetState())
        coins = c.v(.coins, 100)
        messages = c.v(.messages, [LoveMessage]())
        diary = c.v(.diary, [DiaryEntry]())
        myStatus = c.v(.myStatus, DeviceStatus())
        partnerStatus = c.o(.partnerStatus, DeviceStatus.self)
        pairing = c.v(.pairing, Pairing())
        settings = c.v(.settings, AppSettings())
    }
}

struct ProfilePayload: Codable, Equatable {
    var name: String
    var emoji: String
    var birthday: String? = nil
    var startDate: String? = nil
    var startDateUpdatedAt: Int = 0
    var reply: Bool = false
}

extension ProfilePayload {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        name = c.v(.name, "")
        emoji = c.v(.emoji, "")
        birthday = c.o(.birthday, String.self)
        startDate = c.o(.startDate, String.self)
        startDateUpdatedAt = c.v(.startDateUpdatedAt, 0)
        reply = c.v(.reply, false)
    }
}

/// Gói tin đồng bộ, cùng định dạng JSON với bản Android.
struct SyncEnvelope: Codable, Equatable {
    var type: String
    var from: String = ""
    var ts: Int = 0
    var profile: ProfilePayload? = nil
    var status: DeviceStatus? = nil
    var message: LoveMessage? = nil
    var pet: PetState? = nil
    var specialDay: SpecialDay? = nil
    var diary: DiaryEntry? = nil
    var deleteId: String? = nil
}

extension SyncEnvelope {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        type = c.v(.type, "")
        from = c.v(.from, "")
        ts = c.v(.ts, 0)
        profile = c.o(.profile, ProfilePayload.self)
        status = c.o(.status, DeviceStatus.self)
        message = c.o(.message, LoveMessage.self)
        pet = c.o(.pet, PetState.self)
        specialDay = c.o(.specialDay, SpecialDay.self)
        diary = c.o(.diary, DiaryEntry.self)
        deleteId = c.o(.deleteId, String.self)
    }
}
