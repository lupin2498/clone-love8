import MapKit
import SwiftUI

final class PersonAnnotation: NSObject, MKAnnotation {
    let isMe: Bool
    @objc dynamic var coordinate: CLLocationCoordinate2D
    var title: String?

    init(isMe: Bool, coordinate: CLLocationCoordinate2D, title: String) {
        self.isMe = isMe
        self.coordinate = coordinate
        self.title = title
    }
}

struct LoveMapView: UIViewRepresentable {
    let me: CLLocationCoordinate2D?
    let partner: CLLocationCoordinate2D?
    let meName: String
    let partnerName: String
    let meEmoji: String
    let partnerEmoji: String
    let stamps: String
    @Binding var focus: Int

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        map.showsCompass = false
        map.pointOfInterestFilter = .excludingAll
        let hanoi = CLLocationCoordinate2D(latitude: 21.0285, longitude: 105.8542)
        map.setRegion(MKCoordinateRegion(center: hanoi, latitudinalMeters: 20_000, longitudinalMeters: 20_000), animated: false)
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        let c = context.coordinator
        c.emojis = [true: meEmoji, false: partnerEmoji]
        if c.stamps != stamps {
            c.stamps = stamps
            c.icons.removeAll()
            for ann in [c.meAnnotation, c.partnerAnnotation].compactMap({ $0 }) {
                if let view = map.view(for: ann) { view.image = c.icon(for: ann.isMe) }
            }
        }
        c.meAnnotation = sync(map, existing: c.meAnnotation, coordinate: me, isMe: true, title: meName)
        c.partnerAnnotation = sync(map, existing: c.partnerAnnotation, coordinate: partner, isMe: false, title: partnerName)

        if !c.centered, let target = me ?? partner {
            c.centered = true
            map.setRegion(MKCoordinateRegion(center: target, latitudinalMeters: 3_000, longitudinalMeters: 3_000), animated: false)
        }
        if focus != c.lastFocus {
            c.lastFocus = focus
            let target: CLLocationCoordinate2D? = focus % 2 == 1 ? me : partner
            if let target = target {
                map.setRegion(MKCoordinateRegion(center: target, latitudinalMeters: 2_000, longitudinalMeters: 2_000), animated: true)
            }
        }
    }

    private func sync(
        _ map: MKMapView, existing: PersonAnnotation?, coordinate: CLLocationCoordinate2D?, isMe: Bool, title: String
    ) -> PersonAnnotation? {
        guard let coordinate = coordinate else {
            if let existing = existing { map.removeAnnotation(existing) }
            return nil
        }
        if let existing = existing {
            existing.coordinate = coordinate
            existing.title = title
            return existing
        }
        let ann = PersonAnnotation(isMe: isMe, coordinate: coordinate, title: title)
        map.addAnnotation(ann)
        return ann
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        var meAnnotation: PersonAnnotation?
        var partnerAnnotation: PersonAnnotation?
        var centered = false
        var lastFocus = 0
        var stamps = ""
        var emojis: [Bool: String] = [:]
        var icons: [Bool: UIImage] = [:]

        func icon(for isMe: Bool) -> UIImage {
            if let cached = icons[isMe] { return cached }
            let img = Avatars.circle(isMe: isMe, emoji: emojis[isMe] ?? "💖", size: 56)
            icons[isMe] = img
            return img
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            guard let ann = annotation as? PersonAnnotation else { return nil }
            let id = ann.isMe ? "me" : "partner"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: id) ?? MKAnnotationView(annotation: ann, reuseIdentifier: id)
            view.annotation = ann
            view.image = icon(for: ann.isMe)
            view.canShowCallout = true
            return view
        }
    }
}

struct MapTabView: View {
    @EnvironmentObject var model: AppModel
    @EnvironmentObject var location: LocationService
    @State private var focus = 0

    var body: some View {
        let d = model.data
        let me = coordinate(d.myStatus)
        let partner = coordinate(d.partnerStatus)
        ZStack(alignment: .bottom) {
            LoveMapView(
                me: me, partner: partner,
                meName: d.meName, partnerName: d.partnerName,
                meEmoji: d.me.emoji, partnerEmoji: d.partner.emoji,
                stamps: "\(d.mePhotoStamp)-\(d.partnerPhotoStamp)-\(d.me.emoji)-\(d.partner.emoji)",
                focus: $focus
            )
            .ignoresSafeArea(edges: .top)

            VStack(spacing: 10) {
                HStack {
                    Spacer()
                    VStack(spacing: 8) {
                        Button("🙋 Tôi") { focus = focus % 2 == 1 ? focus + 2 : focus + 1 }
                            .buttonStyle(TagButtonStyle())
                        Button("💖 Người ấy") { focus = focus % 2 == 0 ? focus + 2 : focus + 1 }
                            .buttonStyle(TagButtonStyle())
                    }
                    .padding(.trailing, 12)
                }
                Spacer()
                if !location.isAuthorized {
                    VStack(spacing: 8) {
                        Text("📍").font(.system(size: 36))
                        Text("Cho phép truy cập vị trí").font(.headline)
                        Text("Để chia sẻ vị trí và xem khoảng cách giữa hai bạn")
                            .font(.caption).foregroundColor(.loveSub).multilineTextAlignment(.center)
                        Button("Cho phép") { location.requestPermission() }
                            .buttonStyle(PrimaryButtonStyle())
                    }
                    .loveCard(padding: 20)
                    .padding(.horizontal, 24)
                    Spacer()
                }
                partnerPanel(d)
                    .padding(12)
            }
            .padding(.top, 8)
        }
        .navigationBarHidden(true)
        .onAppear { location.start() }
    }

    private func coordinate(_ s: DeviceStatus?) -> CLLocationCoordinate2D? {
        guard let lat = s?.lat, let lon = s?.lon else { return nil }
        return CLLocationCoordinate2D(latitude: lat, longitude: lon)
    }

    private func partnerPanel(_ d: AppData) -> some View {
        let ps = d.partnerStatus
        let dist = distanceMeters(d.myStatus, ps)
        return VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                AvatarView(isMe: false, emoji: d.partner.emoji, stamp: d.partnerPhotoStamp, size: 52)
                VStack(alignment: .leading, spacing: 2) {
                    Text(d.partnerName + (ps?.mood.map { " \($0)" } ?? "")).font(.headline)
                    if !d.isPaired {
                        Text("Chưa kết nối với người ấy").font(.caption).foregroundColor(.loveSub)
                    } else if let ps = ps {
                        let battery = ps.battery.map { "🔋 \($0)%" + (ps.charging ? " ⚡" : "") } ?? "🔋 --"
                        let speed: String = ps.speed.map { $0 > 0.5 ? " · 🚗 \(Int(($0 * 3.6).rounded())) km/h" : " · 🧍 đứng yên" } ?? ""
                        Text(battery + speed).font(.caption)
                        Text("📍 " + (dist.map { formatDistance($0) } ?? "chưa có vị trí") + " · " + Dates.relative(ps.locationTs ?? ps.ts))
                            .font(.caption).foregroundColor(.loveSub)
                    } else {
                        Text("Đang chờ người ấy chia sẻ...").font(.caption).foregroundColor(.loveSub)
                    }
                }
                Spacer()
            }
            HStack(spacing: 8) {
                if d.isPaired {
                    Button("🔄 Làm mới") {
                        Task { @MainActor in
                            await model.sendStatus(force: true)
                            await model.pull()
                        }
                        model.show("Đang cập nhật vị trí...")
                    }
                    .buttonStyle(TagButtonStyle())
                } else {
                    NavigationLink("🔗 Kết nối") { PairingView() }
                        .buttonStyle(TagButtonStyle())
                }
                Button(d.settings.shareLocation ? "📍 Đang chia sẻ" : "🙈 Đã ẩn") {
                    let newValue = !d.settings.shareLocation
                    model.updateSettings { $0.shareLocation = newValue }
                    model.show(newValue ? "Đã bật chia sẻ vị trí" : "Đã tắt chia sẻ vị trí")
                }
                .buttonStyle(TagButtonStyle())
            }
        }
        .loveCard()
    }
}
