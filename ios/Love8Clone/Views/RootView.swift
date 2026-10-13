import SwiftUI

struct RootView: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        ZStack(alignment: .bottom) {
            if model.data.onboarded {
                MainTabView()
            } else {
                OnboardingView()
            }
            if let toast = model.toast {
                ToastView(text: toast)
                    .padding(.bottom, 90)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onTapGesture { model.toast = nil }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: model.toast)
    }
}

struct MainTabView: View {
    @EnvironmentObject var model: AppModel

    var body: some View {
        TabView {
            NavigationStack { HomeView() }
                .tabItem { Label("Tình yêu", systemImage: "heart.fill") }
            NavigationStack { MapTabView() }
                .tabItem { Label("Vị trí", systemImage: "location.fill") }
            NavigationStack { PetView() }
                .tabItem { Label("Thú cưng", systemImage: "pawprint.fill") }
            NavigationStack { SpecialDaysView() }
                .tabItem { Label("Ngày lễ", systemImage: "calendar") }
            NavigationStack { MoreView() }
                .tabItem { Label("Thêm", systemImage: "line.3.horizontal") }
        }
        .tint(.lovePink)
        .onAppear { Notify.requestPermission() }
    }
}

struct OnboardingView: View {
    @EnvironmentObject var model: AppModel
    @State private var step = 0
    @State private var myName = ""
    @State private var myEmoji = "😊"
    @State private var partnerName = ""
    @State private var partnerEmoji = "🥰"
    @State private var startDate = Date()

    var body: some View {
        ZStack {
            Gradients.soft.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 18) {
                    HStack(spacing: 6) {
                        ForEach(0..<4, id: \.self) { i in
                            Capsule()
                                .fill(i <= step ? Color.lovePink : Color(red: 0.95, green: 0.79, blue: 0.84))
                                .frame(width: i == step ? 22 : 8, height: 8)
                        }
                    }
                    .padding(.top, 12)
                    content
                }
                .padding(20)
            }
        }
        .animation(.easeInOut, value: step)
    }

    @ViewBuilder
    private var content: some View {
        switch step {
        case 0:
            VStack(spacing: 18) {
                Spacer(minLength: 40)
                ZStack {
                    Circle().fill(Gradients.love).frame(width: 150, height: 150)
                    BeatingHeart(size: 72)
                }
                Text("Love8 Clone")
                    .font(.largeTitle.weight(.heavy))
                    .foregroundColor(.lovePink)
                Text("Ứng dụng dành cho các cặp đôi\nĐếm ngày yêu · Chia sẻ vị trí · Nuôi thú cưng chung · Ngày đặc biệt")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.loveSub)
                Spacer(minLength: 20)
                Button("Bắt đầu 💕") { step = 1 }
                    .buttonStyle(PrimaryButtonStyle())
            }
        case 1:
            personStep(title: "Bạn là ai?", name: $myName, emoji: $myEmoji, placeholder: "Tên của bạn") { step = 2 }
        case 2:
            VStack(spacing: 10) {
                personStep(title: "Người ấy của bạn", name: $partnerName, emoji: $partnerEmoji, placeholder: "Tên người ấy") { step = 3 }
                Button("Quay lại") { step = 1 }.foregroundColor(.loveSub)
            }
        default:
            VStack(spacing: 16) {
                Text("Hai bạn bắt đầu yêu nhau từ ngày nào?")
                    .font(.title2.weight(.bold))
                    .multilineTextAlignment(.center)
                DatePicker("Ngày bắt đầu", selection: $startDate, in: ...Date(), displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .tint(.lovePink)
                    .environment(\.locale, Locale(identifier: "vi_VN"))
                    .padding(8)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                Button("Hoàn tất 🎉") {
                    model.finishOnboarding(
                        me: Profile(name: myName.trimmingCharacters(in: .whitespaces), emoji: myEmoji),
                        partner: Profile(name: partnerName.trimmingCharacters(in: .whitespaces), emoji: partnerEmoji),
                        start: startDate
                    )
                }
                .buttonStyle(PrimaryButtonStyle())
                Button("Quay lại") { step = 2 }.foregroundColor(.loveSub)
            }
        }
    }

    private func personStep(
        title: String, name: Binding<String>, emoji: Binding<String>, placeholder: String, next: @escaping () -> Void
    ) -> some View {
        let valid = !name.wrappedValue.trimmingCharacters(in: .whitespaces).isEmpty
        return VStack(spacing: 16) {
            Text(title).font(.title2.weight(.bold))
            Text(emoji.wrappedValue)
                .font(.system(size: 56))
                .frame(width: 110, height: 110)
                .background(Circle().fill(Color.white))
            TextField(placeholder, text: name)
                .padding(14)
                .background(RoundedRectangle(cornerRadius: 14).fill(Color.white))
            Text("Chọn biểu tượng").foregroundColor(.loveSub)
            EmojiGrid(options: avatarEmojis, selected: emoji.wrappedValue) { emoji.wrappedValue = $0 }
            Button("Tiếp tục", action: next)
                .buttonStyle(PrimaryButtonStyle(enabled: valid))
                .disabled(!valid)
        }
    }
}
