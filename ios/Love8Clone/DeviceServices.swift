import CoreLocation
import UIKit

enum Battery {
    struct Reading {
        let level: Int?
        let charging: Bool
    }

    @MainActor
    static func read() -> Reading {
        let device = UIDevice.current
        device.isBatteryMonitoringEnabled = true
        let raw = device.batteryLevel
        let level: Int? = raw < 0 ? nil : Int((raw * 100).rounded())
        let charging = device.batteryState == .charging || device.batteryState == .full
        return Reading(level: level, charging: charging)
    }
}

/// Dịch vụ vị trí: theo dõi vị trí khi app mở, giữ lại vị trí gần nhất.
final class LocationService: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    @Published var authorization: CLAuthorizationStatus = .notDetermined
    @Published var lastLocation: CLLocation?
    var onUpdate: ((CLLocation) -> Void)?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.distanceFilter = 20
        authorization = manager.authorizationStatus
        lastLocation = manager.location
    }

    var isAuthorized: Bool {
        authorization == .authorizedWhenInUse || authorization == .authorizedAlways
    }

    func requestPermission() {
        manager.requestWhenInUseAuthorization()
    }

    func requestAlways() {
        manager.requestAlwaysAuthorization()
    }

    func start() {
        guard isAuthorized else { return }
        manager.startUpdatingLocation()
    }

    func stop() {
        manager.stopUpdatingLocation()
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorization = manager.authorizationStatus
        if isAuthorized {
            manager.startUpdatingLocation()
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.last else { return }
        lastLocation = loc
        onUpdate?(loc)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {}
}

func distanceMeters(_ a: DeviceStatus?, _ b: DeviceStatus?) -> Double? {
    guard let aLat = a?.lat, let aLon = a?.lon, let bLat = b?.lat, let bLon = b?.lon else { return nil }
    return CLLocation(latitude: aLat, longitude: aLon).distance(from: CLLocation(latitude: bLat, longitude: bLon))
}

func formatDistance(_ m: Double) -> String {
    if m < 50 { return "Đang ở cạnh nhau 🥰" }
    if m < 1000 { return "\(Int(m.rounded())) m" }
    if m < 100_000 { return String(format: "%.1f km", m / 1000) }
    return "\(Int((m / 1000).rounded())) km"
}
