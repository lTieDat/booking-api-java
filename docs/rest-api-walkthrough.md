# Hướng dẫn Walkthrough REST API BookingAPI

Tài liệu này giải thích cách API của BookingAPI hoạt động theo đúng codebase hiện tại, từ lúc client gửi request cho đến khi Spring Security, controller và service xử lý xong response.

Mục tiêu là giúp bạn nhìn được **toàn bộ luồng REST API** chứ không chỉ nhớ từng endpoint rời rạc.

---

## 1. BookingAPI đang theo mô hình gì?

BookingAPI là một **Spring Boot REST API** theo kiểu layered architecture:

- **Controller** nhận HTTP request và trả HTTP response
- **Service** xử lý nghiệp vụ
- **Repository** làm việc với database
- **Security** xử lý JWT và phân quyền
- **Model / Entity** ánh xạ dữ liệu trong database

### Sơ đồ đơn giản

```text
Client -> Controller -> Service -> Repository -> Database
               |
               v
           Security / Validation / Exception handling
```

Trong repo này, các phần chính là:

- `AuthController` cho signup/signin
- `HotelController` cho khách sạn
- `RoomController` cho phòng
- `BookingController` cho đặt phòng
- `UserController` cho profile người dùng
- `SecurityConfig` + `JwtAuthenticationFilter` cho JWT stateless authentication

---

## 2. Luồng xử lý request trong thực tế

Khi client gọi API, request sẽ đi qua các bước sau:

1. Spring nhận request HTTP
2. `JwtAuthenticationFilter` kiểm tra header `Authorization`
3. Nếu có token hợp lệ, user được đưa vào `SecurityContext`
4. Spring Security kiểm tra endpoint có cần đăng nhập hay không
5. Nếu controller có `@PreAuthorize("hasRole('ADMIN')")`, quyền admin sẽ được kiểm tra thêm
6. Controller gọi service tương ứng
7. Service xử lý business logic và truy vấn DB
8. Response được trả về cho client

### Nếu không có token thì sao?

- endpoint public vẫn truy cập được
- endpoint protected sẽ trả `401 Unauthorized`

### Nếu có token nhưng không đủ quyền thì sao?

- Spring trả `403 Forbidden`

---

## 3. Authentication: đăng ký và đăng nhập

### 3.1 Đăng ký user

Route:

```http
POST /api/auth/signup
```

Controller:

- `AuthController.registerUser()`
- gọi `AuthServiceImpl.signup()`

Ví dụ request:

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Nguyen",
    "username": "alice",
    "email": "alice@example.com",
    "password": "secret123"
  }'
```

Giải thích payload:

- `name`: tên hiển thị
- `username`: tên đăng nhập
- `email`: email duy nhất
- `password`: mật khẩu thô, sẽ được mã hóa bằng BCrypt trước khi lưu

Response thành công:

```json
{
  "success": true,
  "message": "User registered successfully"
}
```

### 3.2 Đăng nhập để lấy JWT

Route:

```http
POST /api/auth/signin
```

Controller:

- `AuthController.authenticateUser()`
- gọi `AuthServiceImpl.signin()`

Ví dụ request:

```bash
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "alice",
    "password": "secret123"
  }'
```

Giải thích:

- `usernameOrEmail`: có thể là username hoặc email
- `password`: mật khẩu đã đăng ký trước đó

Response thành công:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer"
}
```

### JWT được tạo như thế nào?

Trong `JwtTokenProvider`:

- `subject` của token là `userId`
- token có thời hạn theo `app.jwtExpirationInMs`
- token được ký bằng `app.jwtSecret`

Ý nghĩa thực tế:

- server không lưu session
- client chỉ cần giữ token
- các request sau phải gửi token qua header `Authorization`

---

## 4. Cách dùng token sau khi login

Sau khi nhận được token, client gửi kèm mọi request cần xác thực:

```http
Authorization: Bearer <accessToken>
```

Ví dụ:

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <accessToken>"
```

Nếu token hợp lệ, `JwtAuthenticationFilter` sẽ:

- đọc token từ header
- validate chữ ký và expiration
- lấy `userId` từ token
- load user từ database
- set user vào `SecurityContext`

---

## 5. Public endpoints

Theo `SecurityConfig`, các endpoint sau không cần đăng nhập:

- `POST /api/auth/signup`
- `POST /api/auth/signin`
- `GET /api/hotels/**`

### Ví dụ: xem danh sách khách sạn

Route:

```http
GET /api/hotels?page=0&size=10
```

Ví dụ:

```bash
curl http://localhost:8080/api/hotels?page=0&size=10
```

Giải thích:

  - `page`: số trang, mặc định lấy từ `AppConstants`
- `size`: số phần tử mỗi trang
- đây là API public nên không cần token

### Ví dụ: xem chi tiết một khách sạn

```bash
curl http://localhost:8080/api/hotels/1
```

---

## 6. Protected endpoints cho user đã đăng nhập

Một số endpoint bắt buộc phải có JWT hợp lệ:

- `POST /api/bookings`
- `GET /api/bookings/me`
- `GET /api/bookings/{id}`
- `DELETE /api/bookings/{id}`
- `GET /api/users/me`

### 6.1 Tạo booking

Route:

```http
POST /api/bookings
```

Controller:

- `BookingController.createBooking()`
- nhận `@CurrentUser UserPrincipal currentUser`

Ví dụ request:

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 01JZ7Q3M2AZR9V6DS6E8Q8XK7A" \
  -d '{
    "rooms": [
      {
        "roomTypeId": "41000000-0000-0000-0000-000000000001",
        "quantity": 1
      }
    ],
    "checkInDate": "2026-06-10T14:00:00",
    "checkOutDate": "2026-06-13T12:00:00",
    "discountCode": "SUMMER10",
    "guest": {
      "firstName": "Jane",
      "lastName": "Doe",
      "identifyCardNo": "ID123456",
      "phoneNumber": "0901234567",
      "email": "jane@example.com"
    }
  }'
```

Giải thích payload:

- `rooms[]`: danh sách loại phòng và số lượng muốn đặt
- `checkInDate`: ngày giờ nhận phòng
- `checkOutDate`: ngày giờ trả phòng
- `guest`: thông tin khách lưu trú chính
- `discountCode`: mã giảm giá tuỳ chọn

Điểm quan trọng:

- client không cần tự gửi `userId`
- hệ thống lấy user hiện tại từ JWT thông qua `@CurrentUser`
- service sẽ gắn booking với đúng người đang đăng nhập

### 6.2 Xem booking của tôi

```bash
curl http://localhost:8080/api/bookings/me \
  -H "Authorization: Bearer <accessToken>"
```

Giải thích:

- API này chỉ trả booking của chính user đang đăng nhập
- đây là mẫu điển hình cho việc dùng `@CurrentUser`

### 6.3 Xem chi tiết hoặc huỷ booking

Ví dụ:

```bash
curl http://localhost:8080/api/bookings/12 \
  -H "Authorization: Bearer <accessToken>"
```

```bash
curl -X DELETE http://localhost:8080/api/bookings/12 \
  -H "Authorization: Bearer <accessToken>"
```

Ý nghĩa bảo mật:

- service kiểm tra booking đó có thuộc về user hiện tại không
- nếu không phải owner, request sẽ bị từ chối

---

## 7. Admin endpoints

Các API ghi dữ liệu khách sạn/phòng chỉ dành cho admin:

### Hotel

- `POST /api/hotels`
- `PUT /api/hotels/{id}`
- `DELETE /api/hotels/{id}`

### Room

- `POST /api/hotels/{hotelId}/rooms`
- `PUT /api/hotels/{hotelId}/rooms/{id}`
- `DELETE /api/hotels/{hotelId}/rooms/{id}`

### Vì sao cần `@PreAuthorize`?

Trong `HotelController` và `RoomController`, các method ghi dữ liệu đều có:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Điều này có nghĩa:

- token hợp lệ thôi chưa đủ
- user phải có role `ROLE_ADMIN`

### Ví dụ thêm hotel bằng admin token

```bash
curl -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer <adminToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Grand Palace Hotel",
    "description": "5-star hotel in city center",
    "address": "123 Main St",
    "city": "Ho Chi Minh City",
    "country": "Vietnam"
  }'
```

Giải thích:

- nếu token là user thường -> `403 Forbidden`
- nếu token của admin -> request được xử lý bình thường

---

## 8. Xem thông tin user hiện tại

Route:

```http
GET /api/users/me
```

Controller:

- `UserController.getCurrentUser()`
- nhận `@CurrentUser UserPrincipal currentUser`

Ví dụ:

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <accessToken>"
```

Giải thích:

- đây là pattern rất hay trong REST API
- controller không tự parse token
- Spring inject sẵn user hiện tại vào tham số method

---

## 9. Validation và lỗi thường gặp

### 9.1 Validation lỗi input

Các request body như `LoginRequest`, `SignUpRequest`, `HotelRequest`, `RoomRequest`, `BookingRequest` đều dùng validation annotations.

Ví dụ `SignUpRequest` yêu cầu:

- `name` không được blank, độ dài 4–40
- `username` không được blank, độ dài 3–15
- `email` phải đúng định dạng email
- `password` không được blank, độ dài 6–20

Nếu gửi dữ liệu sai, API trả `400 Bad Request`.

### 9.2 Chưa đăng nhập

Nếu gọi endpoint protected mà không có token:

- Spring Security trả `401 Unauthorized`

### 9.3 Token sai hoặc hết hạn

Nếu token bị sửa, sai chữ ký, hoặc hết hạn:

- `JwtAuthenticationFilter` không set authentication
- request sẽ bị từ chối

### 9.4 Không đủ quyền

Nếu user thường gọi endpoint admin:

- API trả `403 Forbidden`

### 9.5 Không tồn tại resource

Ví dụ gọi booking hoặc hotel không tồn tại:

- service ném exception
- `GlobalExceptionHandler` chuẩn hóa response lỗi

---

## 10. Map từ code sang API

| Thành phần | Vai trò |
|---|---|
| `SecurityConfig` | cấu hình route public/protected, bật stateless JWT |
| `JwtAuthenticationFilter` | đọc và xác thực JWT từ request |
| `JwtTokenProvider` | tạo token, validate token, lấy user id |
| `JwtAuthenticationEntryPoint` | trả `401 Unauthorized` khi chưa xác thực |
| `AuthController` | signup/signin |
| `AuthServiceImpl` | nghiệp vụ đăng ký, đăng nhập, tạo JWT |
| `CustomUserDetailsServiceImpl` | load user từ DB theo username/email hoặc id |
| `UserPrincipal` | đại diện user trong Spring Security |
| `CurrentUser` | inject user hiện tại vào controller |
| `HotelController` | CRUD hotel |
| `RoomController` | CRUD room |
| `BookingController` | tạo/xem/huỷ booking |
| `UserController` | xem profile và user hiện tại |

---

## 11. Kịch bản walkthrough đầy đủ

Nếu bạn muốn thử repo theo đúng luồng thực tế, hãy làm như sau:

### Bước 1: đăng ký user

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Nguyen",
    "username": "alice",
    "email": "alice@example.com",
    "password": "secret123"
  }'
```

### Bước 2: đăng nhập

```bash
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "alice",
    "password": "secret123"
  }'
```

### Bước 3: copy `accessToken`

### Bước 4: gọi API cần đăng nhập

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <accessToken>"
```

### Bước 5: gọi endpoint public

```bash
curl http://localhost:8080/api/hotels?page=0&size=10
```

### Bước 6: nếu có admin token, thử thêm hotel

```bash
curl -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer <adminToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Grand Palace Hotel",
    "description": "5-star hotel in city center",
    "address": "123 Main St",
    "city": "Ho Chi Minh City",
    "country": "Vietnam"
  }'
```

---

## 12. Tài liệu liên quan

- JWT guide: [`docs/jwt-security-guide.md`](jwt-security-guide.md)
- README tổng quan: [`README.md`](../README.md)
- Migration guide: [`docs/migration-guide.md`](migration-guide.md)

---

## 13. Kết luận

Walkthrough này cho thấy BookingAPI đang triển khai REST API theo cách rất “thực chiến”:

- public route cho tra cứu dữ liệu
- JWT cho xác thực stateless
- `@CurrentUser` để lấy user hiện tại rất gọn
- `@PreAuthorize` để phân quyền admin
- service layer giữ logic nghiệp vụ thay vì nhồi hết vào controller

Nếu bạn đọc source code song song với tài liệu này, bạn sẽ thấy mỗi endpoint của repo đều nối với một phần rất rõ ràng trong luồng xử lý.

