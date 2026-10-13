# Love8 Clone 💗

Bản clone của ứng dụng **Love8 – App for Couples** (Google Play: `ltd.love8.couples.relationship`): bản Android viết bằng Kotlin + Jetpack Compose (thư mục `app/`), bản iOS viết bằng SwiftUI (thư mục `ios/`).

## Tính năng

| Tab | Chức năng |
| --- | --- |
| 💗 Tình yêu | Đếm số ngày yêu nhau (năm/tháng/ngày), ảnh đại diện hai người, pin & tâm trạng của cả hai, khoảng cách, nút **"Nhớ bạn"**, lời nhắn nhanh, mốc kỷ niệm sắp tới |
| 📍 Vị trí | Bản đồ OpenStreetMap, vị trí của bạn và người ấy, tốc độ, pin, thời gian cập nhật, bật/tắt chia sẻ vị trí |
| 🐾 Thú cưng | Nhận nuôi thú cưng chung (10 loài), cho ăn / chơi / tắm / ngủ, cấp độ & XP, xu, điểm danh hằng ngày, cửa hàng phụ kiện, thông báo khi bé đói |
| 📅 Ngày lễ | Mốc 100/200/…/1000 ngày, kỷ niệm năm, sinh nhật, ngày đặc biệt tùy chỉnh (lặp hằng năm), nhắc nhở D-7 / D-1 / hôm nay |
| ☰ Thêm | Nhật ký tình yêu, kết nối người ấy, cài đặt hồ sơ/ảnh/ngày bắt đầu/quyền, Premium (đã mở khóa) |

Widget màn hình chính: **Ngày yêu nhau**, **Thú cưng**, **Người ấy** (pin, khoảng cách, tâm trạng).

## Kết nối hai điện thoại

1. Máy A: *Thêm → Kết nối người ấy → Tạo mã kết nối*, gửi mã 8 ký tự cho người ấy.
2. Máy B: nhập mã → *Kết nối*.

Dữ liệu (pin, vị trí, lời nhắn, thú cưng, ngày đặc biệt, nhật ký) được đồng bộ qua máy chủ [ntfy](https://ntfy.sh) và **mã hóa đầu-cuối AES-256-GCM** bằng khóa sinh từ mã kết nối. Có thể đổi sang máy chủ ntfy tự host trong màn hình kết nối.

## Tải bộ cài

- **Android (APK):** build bằng `.github/workflows/build-apk.yml`, đăng ở **Releases** (tag `v1.0.x`).
- **iOS (IPA):** build bằng `.github/workflows/build-ios.yml`, đăng ở **Releases** (tag `ios-v1.0.x`).

### Cài IPA lên iPhone

IPA chưa được ký bằng chứng chỉ Apple, cần cài qua công cụ sideload:

- **Sideloadly** (Windows/macOS): cắm iPhone → kéo file `.ipa` vào → đăng nhập Apple ID → Start. Sau đó vào *Cài đặt → Cài đặt chung → Quản lý VPN & thiết bị* để tin cậy nhà phát triển.
- **AltStore**: mở AltStore trên iPhone → *My Apps* → `+` → chọn file `.ipa`.
- **TrollStore** (máy hỗ trợ): mở file `.ipa` bằng TrollStore.

Với Apple ID miễn phí, app cần được ký lại sau mỗi 7 ngày (AltStore/Sideloadly tự làm khi bật refresh).

Bản iOS dùng chung giao thức đồng bộ với bản Android: iPhone và Android có thể kết nối với nhau bằng cùng một mã.

## Build thủ công

### iOS

Yêu cầu macOS + Xcode 15 trở lên và [XcodeGen](https://github.com/yonaskolb/XcodeGen):

```bash
cd ios
xcodegen generate
open Love8Clone.xcodeproj   # chọn Team để ký và chạy trên máy thật
```

### Android

Yêu cầu JDK 17 và Android SDK (compileSdk 35):

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

APK được ký bằng `app/love8clone.jks` (mật khẩu `love8clone`). Có thể thay khóa riêng bằng các biến môi trường
`SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

> Dự án mô phỏng lại tính năng của Love8 cho mục đích học tập, không liên kết với nhà phát triển gốc.
