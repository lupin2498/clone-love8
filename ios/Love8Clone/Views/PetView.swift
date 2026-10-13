import SwiftUI

struct PetView: View {
    @EnvironmentObject var model: AppModel
    @State private var now = nowMs()
    @State private var showEdit = false
    @State private var heart = false
    @State private var bounce = false
    private let timer = Timer.publish(every: 20, on: .main, in: .common).autoconnect()

    var body: some View {
        Group {
            if model.data.pet.adopted {
                petContent
            } else {
                AdoptPetView()
            }
        }
        .background(Gradients.soft.ignoresSafeArea())
        .navigationTitle("Thú cưng")
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(timer) { _ in now = nowMs() }
        .sheet(isPresented: $showEdit) {
            EditPetSheet(pet: model.data.pet)
        }
    }

    private var petContent: some View {
        let pet = PetLogic.tick(model.data.pet, now: now)
        return ScrollView {
            VStack(spacing: 14) {
                stage(pet)
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("Cấp \(pet.level)").font(.headline).foregroundColor(.lovePink)
                        ProgressView(value: Double(pet.xp), total: Double(PetLogic.xpForLevel(pet.level)))
                            .tint(.lovePink)
                        Text("\(pet.xp)/\(PetLogic.xpForLevel(pet.level)) XP").font(.caption).foregroundColor(.loveSub)
                    }
                    StatBar(label: "🍖 No bụng", value: pet.hunger, color: .orange)
                    StatBar(label: "😊 Vui vẻ", value: pet.happiness, color: .lovePink)
                    StatBar(label: "⚡ Năng lượng", value: pet.energy, color: .yellow)
                    StatBar(label: "🫧 Sạch sẽ", value: pet.cleanliness, color: .blue)
                }
                .loveCard()

                VStack(alignment: .leading, spacing: 10) {
                    CardTitle("Chăm sóc bé")
                    HStack {
                        actionButton("🍖", "Cho ăn", .feed)
                        Spacer()
                        actionButton("⚽", "Chơi", .play)
                        Spacer()
                        actionButton("🛁", "Tắm", .bathe)
                        Spacer()
                        actionButton(pet.sleeping ? "☀️" : "🌙", pet.sleeping ? "Đánh thức" : "Đi ngủ", .sleep)
                    }
                }
                .loveCard()

                coinsCard
                shopCard(pet)

                if !pet.log.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        CardTitle("Nhật ký chăm sóc 📝")
                        ForEach(Array(pet.log.prefix(10).enumerated()), id: \.offset) { _, l in
                            HStack(alignment: .top) {
                                Text(l.text).font(.subheadline)
                                Spacer()
                                Text(Dates.relative(l.ts, now: now)).font(.caption2).foregroundColor(.loveSub)
                            }
                        }
                    }
                    .loveCard()
                }
            }
            .padding(16)
        }
    }

    private func stage(_ pet: PetState) -> some View {
        VStack(spacing: 6) {
            HStack(spacing: 6) {
                Text(pet.name).font(.title2.weight(.bold))
                Button { showEdit = true } label: { Text("✏️") }
            }
            Text(PetLogic.mood(pet)).foregroundColor(.loveSub)
            ZStack {
                Ellipse().fill(Color.black.opacity(0.12)).frame(width: 130, height: 18).offset(y: 80)
                VStack(spacing: -18) {
                    if let acc = PetLogic.accessoryEmoji(pet.accessory) {
                        Text(acc).font(.system(size: 40))
                    }
                    Text(PetLogic.emoji(pet.type)).font(.system(size: 110))
                }
                .offset(y: bounce ? (pet.sleeping ? -4 : -14) : 0)
                .onTapGesture {
                    model.petAction(.pat)
                    heart = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) { heart = false }
                }
                if pet.sleeping {
                    Text("💤").font(.system(size: 36)).offset(x: 70, y: -60)
                }
                Text("💕")
                    .font(.system(size: 40))
                    .opacity(heart ? 1 : 0)
                    .offset(y: heart ? -100 : -60)
                    .animation(.easeOut(duration: 0.5), value: heart)
            }
            .frame(height: 200)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.7).repeatForever(autoreverses: true)) { bounce = true }
            }
            Text("Chạm vào bé để vuốt ve 💕").font(.caption).foregroundColor(.loveSub)
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(Gradients.sky)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    private func actionButton(_ emoji: String, _ label: String, _ action: PetLogic.Action) -> some View {
        Button {
            model.show(model.petAction(action))
        } label: {
            VStack(spacing: 4) {
                Text(emoji)
                    .font(.system(size: 28))
                    .frame(width: 62, height: 62)
                    .background(Circle().fill(Color.lovePinkLight))
                Text(label).font(.caption).foregroundColor(.loveText)
            }
        }
        .buttonStyle(.plain)
    }

    private var coinsCard: some View {
        let checkedIn = model.data.settings.lastCheckInDay == Dates.string(Date())
        return HStack(spacing: 10) {
            Text("🪙").font(.system(size: 30))
            VStack(alignment: .leading, spacing: 2) {
                Text("\(model.data.coins) xu").font(.headline)
                Text("Chăm bé và điểm danh mỗi ngày để nhận xu").font(.caption).foregroundColor(.loveSub)
            }
            Spacer()
            Button(checkedIn ? "Đã nhận" : "Điểm danh") { model.dailyCheckIn() }
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Capsule().fill(checkedIn ? Color.gray.opacity(0.4) : Color.lovePink))
                .disabled(checkedIn)
        }
        .loveCard()
    }

    private func shopCard(_ pet: PetState) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            CardTitle("Cửa hàng phụ kiện 🛍️")
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 90), spacing: 10)], spacing: 10) {
                ForEach(PetLogic.accessories) { a in
                    let owned = pet.ownedAccessories.contains(a.id)
                    let wearing = pet.accessory == a.id
                    VStack(spacing: 2) {
                        Text(a.emoji).font(.system(size: 30))
                        Text(a.label).font(.caption).lineLimit(1)
                        Text(wearing ? "Đang đeo" : (owned ? "Đeo" : "🪙 \(a.price)"))
                            .font(.caption2.weight(.bold))
                            .foregroundColor(!owned && model.data.coins < a.price ? .gray : .lovePink)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(8)
                    .background(RoundedRectangle(cornerRadius: 16).fill(wearing ? Color.lovePinkLight : Color(red: 0.98, green: 0.96, blue: 0.965)))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(wearing ? Color.lovePink : Color.clear, lineWidth: 2))
                    .onTapGesture { model.accessory(a.id) }
                }
            }
        }
        .loveCard()
    }
}

struct AdoptPetView: View {
    @EnvironmentObject var model: AppModel
    @State private var type = PetLogic.types[0].id
    @State private var name = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text("🥚").font(.system(size: 80))
                Text("Nhận nuôi thú cưng chung").font(.title2.weight(.bold)).multilineTextAlignment(.center)
                Text("Hai bạn cùng chăm sóc một bé thú cưng: cho ăn, chơi đùa, tắm rửa. Bé sẽ nhớ và gửi thông báo nếu bị bỏ rơi lâu!")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.loveSub)
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 76), spacing: 10)], spacing: 10) {
                    ForEach(PetLogic.types) { t in
                        let sel = t.id == type
                        VStack(spacing: 2) {
                            Text(t.emoji).font(.system(size: 34))
                            Text(t.label).font(.caption)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(8)
                        .background(RoundedRectangle(cornerRadius: 18).fill(sel ? Color.lovePinkLight : Color.white))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(sel ? Color.lovePink : Color(red: 0.94, green: 0.84, blue: 0.87), lineWidth: sel ? 2 : 1))
                        .onTapGesture { type = t.id }
                    }
                }
                TextField("Đặt tên cho bé", text: $name)
                    .padding(14)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Color.white))
                Button("🐾 Nhận nuôi") {
                    model.adoptPet(type: type, name: name.trimmingCharacters(in: .whitespaces))
                }
                .buttonStyle(PrimaryButtonStyle())
            }
            .padding(20)
        }
    }
}

struct EditPetSheet: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var type: String

    init(pet: PetState) {
        _name = State(initialValue: pet.name)
        _type = State(initialValue: pet.type)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Tên") {
                    TextField("Tên", text: $name)
                }
                Section("Loài") {
                    EmojiGrid(options: PetLogic.types.map { $0.emoji }, selected: PetLogic.emoji(type)) { e in
                        if let t = PetLogic.types.first(where: { $0.emoji == e }) { type = t.id }
                    }
                    .padding(.vertical, 6)
                }
            }
            .navigationTitle("Đổi tên / loài")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Hủy") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Lưu") {
                        model.renamePet(name: name.trimmingCharacters(in: .whitespaces), type: type)
                        dismiss()
                    }
                }
            }
        }
    }
}
