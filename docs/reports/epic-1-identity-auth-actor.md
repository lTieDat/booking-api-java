# Báo cáo Epic 1 — Identity / Auth / Actor

## 1. Mục tiêu
Hoàn thiện lớp định danh và xác thực cho BookingAPI để các module sau có thể dựa vào:

- user login / signup
- manager login
- OTP cho verify email và reset password
- actor-aware JWT cho auditing và phân quyền

---

## 2. Các hạng mục đã triển khai

### 2.1 User authentication hiện có
- Giữ nguyên luồng `POST /api/auth/signup`
- Giữ nguyên luồng `POST /api/auth/signin`
- User mới được tạo với trạng thái `isVerified = false`
- JWT tiếp tục hoạt động cho user hiện tại

### 2.2 Manager authentication
- Thêm `Manager` entity và `ManagerRepository`
- Thêm seed manager mặc định trong migration v4
- Thêm endpoint `POST /api/auth/manager/signin`
- Cho phép hệ thống load cả user và manager từ cùng một `UserDetailsService`

### 2.3 OTP flows
- Thêm `OTPToken` entity và `OTPTokenRepository`
- Thêm OTP request cho:
  - verify email
  - reset password
- Thêm OTP confirm cho:
  - verify email
  - reset password
- OTP token có expiry và trạng thái `isUsed`

### 2.4 Actor-aware JWT
- Thêm `ActorType`
- JWT có claim `actorType`
- Filter đọc `actorType` để load đúng principal
- `UserPrincipal` được mở rộng để hỗ trợ cả `USER` và `MANAGER`

### 2.5 Auditing / security support
- `AuditingConfig` tiếp tục dùng principal từ `SecurityContext`
- Epic 1 đặt nền cho các module sau như booking, payment, refund, history

---

## 3. File chính đã thay đổi

### Model / enum
- `src/main/java/com/example/bookingapi/features/auth/model/Manager.java`
- `src/main/java/com/example/bookingapi/features/auth/model/OTPToken.java`
- `src/main/java/com/example/bookingapi/features/user/model/User.java`
- `src/main/java/com/example/bookingapi/features/auth/model/enums/ActorType.java`
- `src/main/java/com/example/bookingapi/features/auth/model/enums/OtpPurpose.java`

### Repository
- `src/main/java/com/example/bookingapi/features/auth/repository/ManagerRepository.java`
- `src/main/java/com/example/bookingapi/features/auth/repository/OTPTokenRepository.java`

### Security
- `src/main/java/com/example/bookingapi/common/security/UserPrincipal.java`
- `src/main/java/com/example/bookingapi/common/security/CustomUserDetailsService.java`
- `src/main/java/com/example/bookingapi/common/security/JwtTokenProvider.java`
- `src/main/java/com/example/bookingapi/common/security/JwtAuthenticationFilter.java`
- `src/main/java/com/example/bookingapi/features/auth/service/impl/CustomUserDetailsServiceImpl.java`

### Auth / API
- `src/main/java/com/example/bookingapi/features/auth/service/AuthService.java`
- `src/main/java/com/example/bookingapi/features/auth/service/impl/AuthServiceImpl.java`
- `src/main/java/com/example/bookingapi/features/auth/controller/AuthController.java`
- `src/main/java/com/example/bookingapi/common/exception/GlobalExceptionHandler.java`

### DTO
- `src/main/java/com/example/bookingapi/features/auth/dto/request/OtpRequest.java`
- `src/main/java/com/example/bookingapi/features/auth/dto/request/OtpVerifyRequest.java`
- `src/main/java/com/example/bookingapi/features/auth/dto/request/PasswordResetConfirmRequest.java`
- `src/main/java/com/example/bookingapi/features/auth/dto/response/OtpTokenResponse.java`

### Migration
- `src/main/resources/db/migration/V4__identity_auth_actor.sql`

---

## 4. Trạng thái kiểm thử

### Đã kiểm tra
- Chạy `./mvnw test`
- Kết quả: **BUILD SUCCESS**
- Flyway chạy thành công tới migration v4
- Spring Boot context khởi động thành công

### Chưa có
- Chưa có test riêng cho từng endpoint auth / OTP
- Chưa có integration test cho manager signin và verify/reset OTP

---

## 5. Ghi chú / hạn chế

- OTP hiện trả trực tiếp qua API response để tiện test/dev, chưa tích hợp email/SMS gateway thật
- Chưa có job nền tự động dọn OTP hết hạn
- Manager signin đang dùng chung JWT flow với user, chỉ khác `actorType`
- Manager signup chưa mở công khai; hiện có seed admin mặc định để test local
- Auditing hiện vẫn ưu tiên actor từ `SecurityContext`, phù hợp cho bước tiếp theo của hệ thống

---

## 6. Hướng xử lý tiếp theo

1. Viết integration test cho auth / OTP / manager signin
2. Bổ sung email/SMS service nếu cần gửi OTP thật
3. Làm Epic 2: `Location`, `HotelGroup`, `Hotel`, `HotelManager`
4. Tiếp tục hoàn thiện audit/history cho các module nghiệp vụ sau

---

## 7. Kết luận
Epic 1 đã hoàn thành phần nền tảng cho định danh và xác thực của BookingAPI. Hệ thống hiện có thể phân biệt user / manager qua JWT, hỗ trợ OTP verify email và reset password, đồng thời giữ nguyên luồng user login/signup hiện có để không phá vỡ các phần đã làm trước đó.

