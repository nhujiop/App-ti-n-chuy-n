# TIỆN CHUYẾN – ANDROID APK BETA

**Bộ này là source Android có quy trình tự build `.apk` bằng GitHub Actions. Chưa phải file APK đã biên dịch.**

- Phiên bản: `0.1.0-beta`, một ứng dụng Android WebView với vỏ native.
- Website đích: `https://tien-chuyen-online.onrender.com/` (URL của người dùng đã triển khai).
- Tài khoản, chuyến và đơn vẫn nằm trong backend PostgreSQL hiện tại của website, không chép database vào điện thoại.
- Có: đăng nhập/đặt xe theo vai trò của website hiện tại; giao diện trên Android; mở Google Maps, link chuyển khoản/ngân hàng bằng ứng dụng bên ngoài; xin quyền GPS khi trang web yêu cầu; trang báo lỗi và nút tải lại.
- **Chưa có**: thuật toán Grab tự phân cuốc, GPS theo dõi nền, Google Maps SDK + chi phí/khóa API, ví bảo đảm 350k, ký quỹ thực, webhook ngân hàng, chống gian lận, ứng dụng tài xế native chạy nền. Những tính năng đó cần cập nhật backend riêng và sẽ không tự sinh ra chỉ bằng APK.

## A. TẠO FILE APK TRÊN GITHUB, KHÔNG CẦN CÀI ANDROID STUDIO

1. Mở `github.com` trên MacBook, nhấn **+** ở góc phải → **New repository**. Đặt tên **tien-chuyen-android**, chọn **Private**, nhấn **Create repository**. Tạo repo **riêng** để không ảnh hưởng repo `tien-chuyen-online` đang chạy trên Render.
2. Giải nén file ZIP này trên Desktop.
3. Trong repo Android mới, nhấn đường link **uploading an existing file** (nếu repo đang trống); nếu đã có file thì tab **Code** → **Add file** → **Upload files**.
4. Từ Finder **kéo thả cả hai thư mục `android-app` và `.github`** vào trang upload. Đừng upload nguyên ZIP; đừng upload file nhạy cảm hay thư mục build. Nếu Finder không thấy `.github`, nhấn `Command + Shift + .` để hiện file/thư mục ẩn.
5. Đảm bảo repo sau khi upload có hai đường dẫn `android-app/app/build.gradle` và `.github/workflows/build-apk.yml`, nằm ngay ngoài cùng repository. Bấm **Commit changes**.
6. Chọn tab **Actions** của repo Android → chọn **Build Tien Chuyen Android APK** ở menu trái. Nếu chưa thấy, tải lại trang; kiểm tra thư mục `.github/workflows/`. Nếu GitHub yêu cầu bật Actions cho repo, làm theo thông báo.
7. Nhấn **Run workflow** → chọn `main` → nút **Run workflow**. Workflow cũng có thể tự chạy ngay sau lần upload.
8. Mở dòng chạy mới nhất. Khi hiển thị dấu tích xanh, cuộn xuống phần **Artifacts**, nhấn **TienChuyen-Android-Beta-APK**. GitHub sẽ tải một ZIP chứa file `app-debug.apk`.
9. Giải nén ZIP đó. Gửi `app-debug.apk` sang điện thoại Android (cáp USB, Google Drive hoặc nhắn file qua nền tảng phù hợp).
10. Trên điện thoại mở `app-debug.apk`, cho phép **Install unknown apps / Cài ứng dụng không rõ nguồn gốc** đối với ứng dụng quản lý file nếu Android hỏi, sau đó cài. Chỉ dùng APK do chính workflow của repo mình build, không dùng link APK lạ.

### Nếu workflow báo lỗi

Mở Actions → build bị đỏ → nhấn bước `Compile beta APK`, chụp phần cuối log, **che thông tin riêng** rồi gửi. Nếu gặp thiếu SDK, kiểm tra bước `Install Android SDK platform`.

### Chi phí

Không cần trả phí Google Play hay mua tên miền để thử bản APK debug này. GitHub Actions trong repo Private có hạn mức sử dụng miễn phí theo tài khoản; **có thể phát sinh phí nếu vượt hạn mức hoặc tài khoản đã cấu hình thanh toán**, nên xem Billing/Actions trước khi build hàng loạt. Render Free vẫn ngủ khi không truy cập và PostgreSQL Free chỉ để dùng thử.

## B. BUILD TRÊN MACBOOK VỚI ANDROID STUDIO (TÙY CHỌN)

Cài Android Studio từ trang chính thức. Mở thư mục `android-app` (không phải toàn bộ ZIP), để IDE đồng bộ Gradle và SDK 35, chọn **Build → Build Bundle(s) / APK(s) → Build APK(s)**. File sẽ có trong `app/build/outputs/apk/debug/app-debug.apk`. Android Studio có thể yêu cầu tạo Gradle wrapper hoặc thiết lập Gradle vì bộ này cố ý không đóng gói thư viện bên thứ ba.

## C. CẤU TRÚC PROJECT

```
.github/workflows/build-apk.yml   GitHub Actions build apk
android-app/settings.gradle
android-app/build.gradle
android-app/gradle.properties
android-app/app/build.gradle
android-app/app/src/main/AndroidManifest.xml
android-app/app/src/main/java/vn/tienchuyen/app/MainActivity.java
android-app/app/src/main/res/...  Styles + icon
```

## D. BẢO MẬT & CẬP NHẬT

- APK `debug` được ký tự động bởi debug key ở máy runner; những lần build khác nhau có thể dùng key khác nhau. **Có thể phải gỡ bản cũ trước khi cài bản mới**. Chưa dùng để phát hành công khai hoặc lên Google Play.
- Khi phát hành cần app ký bằng **release keystore cố định**, giữ an toàn ở GitHub Secrets/keystore quản trị, tạo AAB, chính sách quyền riêng tư và đáp ứng yêu cầu Google Play.
- Chỉ có quyền `INTERNET` và `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION`; GPS chỉ hỏi khi website thực sự dùng `navigator.geolocation`. Không chạy GPS ngầm.
- Tài khoản và mật khẩu không có trong source APK. Không đưa `ADMIN_PASSWORD`, `DATABASE_URL` hoặc khóa ngân hàng/Google Maps vào repo hay APK.
- App chỉ tải nội dung bên trong `tien-chuyen-online.onrender.com`; liên kết ngoài được mở bởi trình duyệt hoặc ứng dụng chuyên dụng. Chỉ dựa vào WebView không có nghĩa là đã tích hợp cổng thanh toán.
- Nếu website Render ngừng hoạt động, APK cũng không sử dụng được các luồng online. Nút **Thử lại** sẽ giúp tải lại trang.

## E. KẾ HOẠCH ĐỂ THÀNH ỨNG DỤNG KIỂU GRAB THẬT

1. API tạo yêu cầu, báo giá, ghép tài xế theo GPS và giờ, đảm bảo xử lý tranh ghế.
2. Chế độ tài xế online/offline, nhận/từ chối cuốc; app native Android có foreground service để theo dõi chuyến khi hợp lệ và được cấp quyền.
3. GPS, Google Maps/Routes API (cần API key và kiểm soát ngân sách), ETA và OTP khi lên xe.
4. Ledger giao dịch bảo đảm (chế độ giả lập trước), không giữ/quyết toán tiền thật khi chưa rõ thỏa thuận và tư cách pháp lý.
5. Kết nối cổng thanh toán có webhook chứng thực, quy trình thanh toán tiền mặt/chuyển khoản tài xế, xác minh hủy chuyến/tranh chấp.
6. Backend mới deploy lên Render/DB có sao lưu; kiểm thử an ninh và chống gian lận trước khi sử dụng tiền thật.
