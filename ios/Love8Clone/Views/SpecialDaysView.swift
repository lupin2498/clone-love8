import SwiftUI

private let dayEmojis = ["💝", "🎂", "🎉", "💍", "🌹", "✈️", "🍰", "🎁", "🏖️", "🎄", "🎬", "☕"]

struct SpecialDaysView: View {
    @EnvironmentObject var model: AppModel
    @State private var showAdd = false
    @State private var toDelete: SpecialDay?

    var body: some View {
        let d = model.data
        let start = Dates.parse(d.startDate) ?? Date()
        let events = Events.all(d, milestoneCount: 6)
        ScrollView {
            VStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Ngày đặc biệt 📅").font(.title2.weight(.bold)).foregroundColor(.white)
                    Text("Hôm nay là ngày thứ \(Dates.daysTogether(start)) hai bạn bên nhau").foregroundColor(.white)
                    Text("Kỷ niệm, sinh nhật và những ngày đáng nhớ").font(.caption).foregroundColor(.white.opacity(0.85))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(18)
                .background(Gradients.love)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))

                ForEach(events) { ev in
                    let deleteAction: (() -> Void)? = ev.custom == nil ? nil : { toDelete = ev.custom }
                    EventRow(event: ev, onDelete: deleteAction)
                        .loveCard(padding: 10)
                }
            }
            .padding(16)
            .padding(.bottom, 70)
        }
        .background(Gradients.soft.ignoresSafeArea())
        .overlay(alignment: .bottomTrailing) {
            Button { showAdd = true } label: {
                Image(systemName: "plus")
                    .font(.title2.weight(.bold))
                    .foregroundColor(.white)
                    .frame(width: 58, height: 58)
                    .background(Circle().fill(Color.lovePink))
                    .shadow(color: Color.black.opacity(0.2), radius: 6, y: 3)
            }
            .padding(20)
        }
        .navigationTitle("Ngày lễ")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showAdd) { AddSpecialDaySheet() }
        .alert("Xóa ngày đặc biệt?", isPresented: Binding(get: { toDelete != nil }, set: { if !$0 { toDelete = nil } })) {
            Button("Xóa", role: .destructive) {
                if let sd = toDelete { model.deleteItem(sd.id) }
                toDelete = nil
            }
            Button("Hủy", role: .cancel) { toDelete = nil }
        } message: {
            Text("\"\(toDelete?.title ?? "")\" sẽ bị xóa trên cả hai máy.")
        }
    }
}

struct AddSpecialDaySheet: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var date = Date()
    @State private var emoji = dayEmojis[0]
    @State private var yearly = true
    @State private var remind = true

    var body: some View {
        NavigationStack {
            Form {
                Section("Sự kiện") {
                    TextField("VD: Lần đầu hẹn hò", text: $title)
                    DatePicker("Ngày", selection: $date, displayedComponents: .date)
                        .environment(\.locale, Locale(identifier: "vi_VN"))
                }
                Section("Biểu tượng") {
                    EmojiGrid(options: dayEmojis, selected: emoji) { emoji = $0 }
                        .padding(.vertical, 6)
                }
                Section {
                    Toggle("Lặp lại hằng năm", isOn: $yearly)
                    Toggle("Nhắc nhở", isOn: $remind)
                }
            }
            .tint(.lovePink)
            .navigationTitle("Thêm ngày đặc biệt")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Hủy") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Lưu") {
                        model.addSpecialDay(title: title, date: date, emoji: emoji, yearly: yearly, remind: remind)
                        dismiss()
                    }
                    .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }
}
