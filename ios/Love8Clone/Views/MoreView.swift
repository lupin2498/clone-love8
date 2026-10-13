import PhotosUI
import SwiftUI
import UIKit

struct MoreView: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        let d = model.data
        let start = Dates.parse(d.startDate) ?? Date()
        ScrollView {
            VStack(spacing: 14) {
                HStack(spacing: 6) {
                    AvatarView(isMe: true, emoji: d.me.emoji, stamp: d.mePhotoStamp, size: 56)
                    BeatingHeart(size: 22)
                    AvatarView(isMe: false, emoji: d.partner.emoji, stamp: d.partnerPhotoStamp, size: 56)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(d.meName) & \(d.partnerName)").font(.headline)
                        Text("\(Dates.daysTogether(start)) ngày bên nhau").foregroundColor(.loveSub)
                        Text(d.partnerConnected ? "🟢 Đã kết nối" : (d.isPaired ? "🟡 Đang chờ người ấy" : "⚪ Chưa kết nối"))
                            .font(.caption)
                    }
                    .padding(.leading, 6)
                    Spacer()
                }
                .loveCard()

                NavigationLink(destination: PremiumView()) {
                    HStack(spacing: 12) {
                        Text("👑").font(.system(size: 30))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Love8 Premium").font(.headline).foregroundColor(.white)
                            Text("Đã mở khóa toàn bộ tính năng").font(.caption).foregroundColor(.white.opacity(0.9))
                        }
                        Spacer()
                        Image(systemName: "chevron.right").foregroundColor(.white)
                    }
                    .padding(18)
                    .background(Gradients.premium)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                }
                .buttonStyle(.plain)

                VStack(spacing: 0) {
                    menuRow("🔗", "Kết nối người ấy", d.isPaired ? "Mã: đã thiết lập" : "Ghép đôi hai điện thoại", PairingView())
                    Divider()
                    menuRow("📔", "Nhật ký tình yêu", "\(d.diary.count) trang nhật ký", DiaryView())
                    Divider()
                    menuRow("⚙️", "Cài đặt", "Hồ sơ, ngày bắt đầu, thông báo, quyền riêng tư", SettingsView())
                    Divider()
                    menuRow("ℹ️", "Giới thiệu", "Phiên bản & hướng dẫn widget", AboutView())
                }
                .loveCard(padding: 4)
            }
            .padding(16)
        }
        .background(Gradients.soft.ignoresSafeArea())
        .navigationTitle("Thêm")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func menuRow<Dest: View>(_ emoji: String, _ title: String, _ subtitle: String, _ dest: Dest) -> some View {
        NavigationLink(destination: dest) {
            HStack(spacing: 12) {
                Text(emoji)
                    .font(.system(size: 20))
                    .frame(width: 42, height: 42)
                    .background(Circle().fill(Color.lovePinkLight))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.subheadline.weight(.semibold)).foregroundColor(.loveText)
                    Text(subtitle).font(.caption).foregroundColor(.loveSub).multilineTextAlignment(.leading)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundColor(.loveSub)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Nhật ký

struct DiaryView: View {
    @EnvironmentObject var model: AppModel
    @State private var showAdd = false
    @State private var toDelete: DiaryEntry?

    var body: some View {
        let d = model.data
        ScrollView {
            VStack(spacing: 12) {
                if d.diary.isEmpty {
                    VStack(spacing: 6) {
                        Text("📖").font(.system(size: 64))
                        Text("Chưa có trang nhật ký nào").font(.headline)
                        Text("Ghi lại những khoảnh khắc đáng nhớ của hai bạn")
                            .foregroundColor(.loveSub).multilineTextAlignment(.center)
                    }
                    .padding(.top, 60)
                }
                ForEach(d.diary) { e in
                    let mine = e.author == d.deviceId
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 10) {
                            Text(e.mood).font(.system(size: 30))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(mine ? d.meName : d.partnerName).font(.headline)
                                Text(Dates.time(e.ts)).font(.caption2).foregroundColor(.loveSub)
                            }
                            Spacer()
                            if mine {
                                Button { toDelete = e } label: { Image(systemName: "trash").foregroundColor(.loveSub) }
                            }
                        }
                        Text(e.text)
                    }
                    .loveCard()
                }
            }
            .padding(16)
        }
        .background(Gradients.soft.ignoresSafeArea())
        .navigationTitle("Nhật ký tình yêu 📔")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showAdd = true } label: { Image(systemName: "square.and.pencil") }
            }
        }
        .sheet(isPresented: $showAdd) { AddDiarySheet() }
        .alert("Xóa trang nhật ký?", isPresented: Binding(get: { toDelete != nil }, set: { if !$0 { toDelete = nil } })) {
            Button("Xóa", role: .destructive) {
                if let e = toDelete { model.deleteItem(e.id) }
                toDelete = nil
            }
            Button("Hủy", role: .cancel) { toDelete = nil }
        } message: {
            Text("Trang nhật ký sẽ bị xóa trên cả hai máy.")
        }
    }
}

struct AddDiarySheet: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var mood = moodEmojis[0]
    @State private var text = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Tâm trạng") {
                    EmojiGrid(options: moodEmojis, selected: mood) { mood = $0 }
                        .padding(.vertical, 6)
                }
                Section("Hôm nay của hai bạn thế nào?") {
                    TextEditor(text: $text).frame(minHeight: 160)
                }
            }
            .navigationTitle("Viết nhật ký")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Hủy") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Lưu") {
                        model.addDiary(mood: mood, text: text)
                        dismiss()
                    }
                    .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

// MARK: - Kết nối

struct PairingView: View {
    @EnvironmentObject var model: AppModel
    @State private var input = ""
    @State private var server = ""
    @State private var confirmUnpair = false

    var body: some View {
        let d = model.data
        ScrollView {
            VStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("🔗 Ghép đôi hai điện thoại").font(.title3.weight(.bold)).foregroundColor(.white)
                    Text("Sau khi kết nối, hai bạn sẽ thấy pin, vị trí, tâm trạng của nhau, gửi lời nhắn, cùng nuôi thú cưng và chia sẻ ngày đặc biệt. Kết nối được cả iPhone với Android.")
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
                .background(Gradients.love)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))

                if let code = d.pairing.code {
                    connectedCard(d, code: code)
                } else {
                    VStack(alignment: .leading, spacing: 10) {
                        CardTitle("Cách 1 · Tạo mã mới")
                        Text("Tạo mã rồi gửi cho người ấy nhập vào app của họ.").font(.caption).foregroundColor(.loveSub)
                        Button("✨ Tạo mã kết nối") { model.createPairing() }
                            .buttonStyle(PrimaryButtonStyle())
                    }
                    .loveCard()
                    VStack(alignment: .leading, spacing: 10) {
                        CardTitle("Cách 2 · Nhập mã của người ấy")
                        TextField("Mã 8 ký tự (VD: ABCD-2345)", text: $input)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                            .padding(12)
                            .background(RoundedRectangle(cornerRadius: 12).stroke(Color.lovePinkLight, lineWidth: 1.5))
                        Button("💞 Kết nối") { model.joinPairing(input) }
                            .buttonStyle(PrimaryButtonStyle())
                    }
                    .loveCard()
                }

                VStack(alignment: .leading, spacing: 8) {
                    CardTitle("Máy chủ đồng bộ")
                    Text("Mặc định dùng ntfy.sh miễn phí. Có thể tự host ntfy và nhập địa chỉ tại đây (cả hai máy phải dùng cùng máy chủ).")
                        .font(.caption).foregroundColor(.loveSub)
                    TextField("https://ntfy.sh", text: $server)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .padding(12)
                        .background(RoundedRectangle(cornerRadius: 12).stroke(Color.lovePinkLight, lineWidth: 1.5))
                    HStack {
                        Button("Kiểm tra") {
                            let s = server
                            Task { @MainActor in
                                let ok = await Ntfy.ping(s)
                                model.show(ok ? "Máy chủ hoạt động ✅" : "Không kết nối được máy chủ ❌")
                            }
                        }
                        Spacer()
                        Button("Lưu") { model.setServer(server) }
                    }
                    .foregroundColor(.lovePink)
                }
                .loveCard()

                VStack(alignment: .leading, spacing: 6) {
                    CardTitle("🔒 Quyền riêng tư")
                    Text("Mọi dữ liệu gửi đi đều được mã hóa đầu-cuối (AES-256-GCM) bằng khóa sinh ra từ mã kết nối. Máy chủ chỉ chuyển tiếp dữ liệu đã mã hóa và tự xóa sau tối đa 12 giờ. Chỉ chia sẻ mã cho người ấy.")
                        .font(.caption).foregroundColor(.loveSub)
                }
                .loveCard()
            }
            .padding(16)
        }
        .background(Gradients.soft.ignoresSafeArea())
        .navigationTitle("Kết nối người ấy")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { server = model.data.pairing.server }
        .alert("Ngắt kết nối?", isPresented: $confirmUnpair) {
            Button("Ngắt kết nối", role: .destructive) { model.unpair() }
            Button("Hủy", role: .cancel) {}
        } message: {
            Text("Hai máy sẽ không còn đồng bộ với nhau. Bạn có thể kết nối lại bằng mã mới.")
        }
    }

    private func connectedCard(_ d: AppData, code: String) -> some View {
        let pretty = LoveCrypto.pretty(code)
        return VStack(alignment: .leading, spacing: 10) {
            Text("Mã kết nối của hai bạn").foregroundColor(.loveSub)
            Text(pretty)
                .font(.system(size: 36, weight: .heavy, design: .monospaced))
                .foregroundColor(.lovePink)
            Text(d.partnerConnected ? "🟢 Đã kết nối với \(d.partnerName)" : "🟡 Đang chờ người ấy nhập mã...")
                .font(.subheadline.weight(.semibold))
            HStack(spacing: 8) {
                Button("📋 Sao chép") {
                    UIPasteboard.general.string = pretty
                    model.show("Đã sao chép mã")
                }
                .buttonStyle(TagButtonStyle())
                ShareLink(item: "💗 Kết nối với mình trên Love8 Clone nhé! Mã kết nối: \(pretty)") {
                    Text("📤 Chia sẻ")
                }
                .buttonStyle(TagButtonStyle())
                Button("🔄 Đồng bộ") {
                    Task { @MainActor in
                        await model.sendStatus(force: true)
                        let got = await model.pull()
                        model.show(got.isEmpty ? "Đã đồng bộ" : "Đã nhận \(got.count) cập nhật mới")
                    }
                }
                .buttonStyle(TagButtonStyle())
            }
            Button("Ngắt kết nối") { confirmUnpair = true }
                .foregroundColor(.red)
                .padding(.top, 4)
        }
        .loveCard()
    }
}

// MARK: - Cài đặt

struct SettingsView: View {
    @EnvironmentObject var model: AppModel
    @EnvironmentObject var location: LocationService
    @State private var meName = ""
    @State private var partnerName = ""
    @State private var meItem: PhotosPickerItem?
    @State private var partnerItem: PhotosPickerItem?
    @State private var confirmReset = false

    var body: some View {
        let d = model.data
        Form {
            profileSection(title: "Hồ sơ của bạn", isMe: true, profile: d.me, stamp: d.mePhotoStamp, name: $meName, item: $meItem)
            profileSection(title: "Người ấy", isMe: false, profile: d.partner, stamp: d.partnerPhotoStamp, name: $partnerName, item: $partnerItem)

            Section("Ngày bắt đầu yêu 💞") {
                DatePicker(
                    "Ngày bắt đầu",
                    selection: Binding(
                        get: { Dates.parse(model.data.startDate) ?? Date() },
                        set: { model.setStartDate($0) }
                    ),
                    in: ...Date(),
                    displayedComponents: .date
                )
                .environment(\.locale, Locale(identifier: "vi_VN"))
            }

            Section("Chia sẻ & thông báo") {
                Toggle("📍 Chia sẻ vị trí", isOn: settingBinding(\.shareLocation))
                Toggle("🔋 Chia sẻ mức pin", isOn: settingBinding(\.shareBattery))
                Toggle("💌 Thông báo lời nhắn", isOn: settingBinding(\.notifyMessages))
                Toggle("🐾 Thông báo thú cưng", isOn: settingBinding(\.notifyPet))
                Toggle("📅 Nhắc ngày đặc biệt", isOn: settingBinding(\.notifySpecialDays))
            }

            Section("Quyền ứng dụng") {
                Button("🔔 Cho phép thông báo") { Notify.requestPermission() }
                Button("📍 Cho phép vị trí") { location.requestPermission() }
                Button("🛰️ Vị trí nền (luôn cho phép)") { location.requestAlways() }
                Button("⚙️ Mở cài đặt ứng dụng") {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }
            }

            Section("Dữ liệu") {
                Button("🗑️ Xóa toàn bộ dữ liệu", role: .destructive) { confirmReset = true }
            }
        }
        .tint(.lovePink)
        .navigationTitle("Cài đặt")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            meName = model.data.me.name
            partnerName = model.data.partner.name
        }
        .onChange(of: meItem) { item in loadPhoto(item, isMe: true) }
        .onChange(of: partnerItem) { item in loadPhoto(item, isMe: false) }
        .alert("Xóa toàn bộ dữ liệu?", isPresented: $confirmReset) {
            Button("Xóa", role: .destructive) { model.resetAll() }
            Button("Hủy", role: .cancel) {}
        } message: {
            Text("Mọi dữ liệu trên máy này (hồ sơ, nhật ký, thú cưng, ngày đặc biệt) sẽ bị xóa và không thể khôi phục.")
        }
    }

    private func settingBinding(_ key: WritableKeyPath<AppSettings, Bool>) -> Binding<Bool> {
        Binding(
            get: { model.data.settings[keyPath: key] },
            set: { value in model.updateSettings { $0[keyPath: key] = value } }
        )
    }

    private func loadPhoto(_ item: PhotosPickerItem?, isMe: Bool) {
        guard let item = item else { return }
        Task { @MainActor in
            if let raw = try? await item.loadTransferable(type: Data.self), let img = UIImage(data: raw) {
                model.setPhoto(img, isMe: isMe)
                model.show("Đã cập nhật ảnh đại diện")
            } else {
                model.show("Không đọc được ảnh")
            }
        }
    }

    private func saveProfile(isMe: Bool, _ change: (inout Profile) -> Void) {
        var p = isMe ? model.data.me : model.data.partner
        change(&p)
        if isMe { model.updateMe(p) } else { model.updatePartner(p) }
    }

    @ViewBuilder
    private func profileSection(
        title: String, isMe: Bool, profile: Profile, stamp: Int, name: Binding<String>, item: Binding<PhotosPickerItem?>
    ) -> some View {
        Section(title) {
            HStack(spacing: 14) {
                AvatarView(isMe: isMe, emoji: profile.emoji, stamp: stamp, size: 64)
                VStack(alignment: .leading, spacing: 8) {
                    PhotosPicker(selection: item, matching: .images) {
                        Text("📷 Chọn ảnh từ thư viện")
                    }
                    Button("😊 Dùng biểu tượng") { model.setPhoto(nil, isMe: isMe) }
                }
            }
            .buttonStyle(.borderless)
            HStack {
                TextField("Tên", text: name)
                Button("Lưu") {
                    let n = name.wrappedValue.trimmingCharacters(in: .whitespaces)
                    saveProfile(isMe: isMe) { $0.name = n }
                    model.show("Đã lưu tên")
                }
                .buttonStyle(.borderless)
            }
            EmojiGrid(options: avatarEmojis, selected: profile.emoji) { e in
                saveProfile(isMe: isMe) { $0.emoji = e }
            }
            .padding(.vertical, 6)
            if let b = Dates.parse(profile.birthday) {
                DatePicker(
                    "🎂 Sinh nhật",
                    selection: Binding(
                        get: { b },
                        set: { v in saveProfile(isMe: isMe) { $0.birthday = Dates.string(v) } }
                    ),
                    displayedComponents: .date
                )
                .environment(\.locale, Locale(identifier: "vi_VN"))
            } else {
                Button("🎂 Thêm ngày sinh") {
                    saveProfile(isMe: isMe) { $0.birthday = "2000-01-01" }
                }
                .buttonStyle(.borderless)
            }
        }
    }
}

// MARK: - Premium & giới thiệu

private let premiumFeatures: [(String, String)] = [
    ("📍", "Chia sẻ vị trí thời gian thực, tốc độ & thời gian dừng"),
    ("🔋", "Xem mức pin và trạng thái sạc của người ấy"),
    ("🐾", "Nuôi thú cưng chung, mọi loài & phụ kiện"),
    ("📅", "Ngày đặc biệt không giới hạn, nhắc nhở tự động"),
    ("🧩", "Widget màn hình chính: ngày yêu, thú cưng"),
    ("📔", "Nhật ký tình yêu đồng bộ hai máy"),
    ("💌", "Lời nhắn & \"Nhớ bạn\" không giới hạn"),
    ("🔒", "Mã hóa đầu-cuối mọi dữ liệu"),
]

struct PremiumView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                VStack(spacing: 6) {
                    Text("👑").font(.system(size: 56))
                    Text("Premium đã mở khóa").font(.title2.weight(.heavy)).foregroundColor(.white)
                    Text("Bản clone: mọi tính năng đều miễn phí").foregroundColor(.white.opacity(0.9))
                }
                .frame(maxWidth: .infinity)
                .padding(24)
                .background(Gradients.premium)
                .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
                ForEach(premiumFeatures.indices, id: \.self) { i in
                    let item = premiumFeatures[i]
                    HStack(spacing: 12) {
                        Text(item.0).font(.system(size: 24))
                        Text(item.1)
                        Spacer()
                        Text("✅")
                    }
                    .loveCard(padding: 14)
                }
            }
            .padding(16)
        }
        .background(Gradients.soft.ignoresSafeArea())
        .navigationTitle("Love8 Premium")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct AboutView: View {
    private var version: String {
        let v = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
        let b = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "1"
        return "\(v) (\(b))"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                VStack(spacing: 4) {
                    Text("💗").font(.system(size: 64))
                    Text("Love8 Clone").font(.title2.weight(.heavy))
                    Text("Phiên bản \(version)").foregroundColor(.loveSub)
                }
                VStack(alignment: .leading, spacing: 4) {
                    CardTitle("Thêm widget ra màn hình chính")
                    Text("Nhấn giữ màn hình chính → nút + → tìm \"Love8 Clone\" → chọn widget Ngày yêu nhau hoặc Thú cưng.")
                        .foregroundColor(.loveSub)
                }
                .loveCard()
                VStack(alignment: .leading, spacing: 4) {
                    CardTitle("Tính năng")
                    Text("• Đếm ngày yêu nhau & mốc kỷ niệm\n• Chia sẻ vị trí, pin, tâm trạng\n• Gửi \"Nhớ bạn\" và lời nhắn\n• Thú cưng chung: cho ăn, chơi, tắm, ngủ, phụ kiện\n• Ngày đặc biệt & nhắc nhở\n• Nhật ký tình yêu\n• Widget màn hình chính\n• Kết nối được với bản Android")
                        .foregroundColor(.loveSub)
                }
                .loveCard()
                Text("Ứng dụng mô phỏng lại tính năng của \"Love8 - App for Couples\" cho mục đích học tập, không liên kết với nhà phát triển gốc.")
                    .font(.caption)
                    .foregroundColor(.loveSub)
                    .multilineTextAlignment(.center)
                    .padding(8)
            }
            .padding(16)
        }
        .background(Gradients.soft.ignoresSafeArea())
        .navigationTitle("Giới thiệu")
        .navigationBarTitleDisplayMode(.inline)
    }
}
