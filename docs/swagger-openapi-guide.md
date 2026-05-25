# Swagger / OpenAPI Guide cho BookingAPI

Tài liệu này hướng dẫn cách **tích hợp và sử dụng Swagger/OpenAPI** cho BookingAPI dựa trên codebase hiện tại.

## Trạng thái hiện tại

Ở thời điểm hiện tại, project **chưa có Swagger/OpenAPI** trong dependency và source code:

- chưa có `springdoc-openapi` trong `pom.xml`
- chưa có cấu hình `OpenAPI`, `SecurityScheme`, `SwaggerConfig`
- chưa có annotation như `@Operation`, `@Tag`, `@Schema`

Vì vậy, tài liệu này tập trung vào:

1. cách thêm Swagger/OpenAPI vào project
2. cách document các controller hiện có
3. cách dùng Swagger UI với JWT Bearer token

---

## 1. Swagger/OpenAPI là gì?

- **OpenAPI**: chuẩn mô tả REST API
- **Swagger UI**: giao diện web để xem và test API từ file OpenAPI

Lợi ích:

- xem danh sách endpoint rõ ràng
- mô tả request/response chuẩn hóa
- test API trực tiếp trên trình duyệt
- dễ chia sẻ contract API cho frontend / QA / team khác

---

## 2. Nên tích hợp bằng thư viện nào?

Với Spring Boot hiện tại, cách phổ biến nhất là dùng **springdoc-openapi**.

### Dependency đề xuất

Nếu sau này bạn muốn bật Swagger cho project, có thể thêm dependency dạng:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.x.x</version>
</dependency>
```

> Phiên bản cụ thể nên chọn theo Spring Boot version trong `pom.xml`.

---

## 3. Cấu hình cơ bản

### 3.1 OpenAPI metadata

Nên tạo một config class để khai báo:

- title
- description
- version
- contact
- server

Ví dụ mục tiêu:

- **BookingAPI OpenAPI**
- mô tả REST API đặt phòng khách sạn
- hỗ trợ JWT Bearer auth

### 3.2 Swagger UI path

Thông thường có thể truy cập:

```text
/swagger-ui.html
```

hoặc:

```text
/swagger-ui/index.html
```

Tùy version springdoc.

---

## 4. Tích hợp JWT Bearer vào Swagger

BookingAPI đang dùng JWT stateless, nên Swagger cần hỗ trợ nhập token thủ công.

### Mục tiêu

Trong Swagger UI, người dùng có thể bấm **Authorize** và dán:

```text
Bearer <jwt-token>
```

### Ý nghĩa

Sau khi authorize:

- các endpoint protected sẽ được gọi kèm header `Authorization`
- các endpoint admin cũng test được ngay

---

## 5. Các annotation nên dùng

### 5.1 Ở controller

- `@Tag` — gom nhóm API theo module
- `@Operation` — mô tả từng endpoint
- `@Parameter` — mô tả path/query/header param
- `@ApiResponses` — mô tả HTTP status

### 5.2 Ở DTO / model

- `@Schema` — mô tả field
- ví dụ: `username`, `password`, `roomId`, `checkInDate`

---

## 6. Mapping controller hiện tại vào Swagger

### 6.1 `AuthController`

File: `src/main/java/com/example/bookingapi/controller/AuthController.java`

Nên document:

- `POST /api/auth/signup`
- `POST /api/auth/signin`
- `POST /api/auth/manager/signin`
- OTP request/confirm endpoints

#### Public / protected

- public
- không cần JWT để gọi

#### Nên ghi rõ

- request body
- response body
- 400 validation
- 401 login fail

---

### 6.2 `HotelController`

File: `src/main/java/com/example/bookingapi/controller/HotelController.java`

Endpoint hiện tại:

- `GET /api/hotels` — public
- `GET /api/hotels/{id}` — public
- `POST /api/hotels` — admin
- `PUT /api/hotels/{id}` — admin
- `DELETE /api/hotels/{id}` — admin

#### Swagger nên thể hiện

- GET: `200 OK`
- POST/PUT/DELETE: `401/403` nếu chưa login hoặc không phải admin

---

### 6.3 `RoomController`

File: `src/main/java/com/example/bookingapi/controller/RoomController.java`

Endpoint:

- `GET /api/hotels/{hotelId}/rooms` — public
- `GET /api/hotels/{hotelId}/rooms/{id}` — public
- `POST /api/hotels/{hotelId}/rooms` — admin
- `PUT /api/hotels/{hotelId}/rooms/{id}` — admin
- `DELETE /api/hotels/{hotelId}/rooms/{id}` — admin

Nên ghi rõ:

- `hotelId` và `id` là path params
- admin-only routes cần JWT Bearer

---

### 6.4 `BookingController`

File: `src/main/java/com/example/bookingapi/controller/BookingController.java`

Endpoint:

- `POST /api/bookings`
- `GET /api/bookings/me`
- `GET /api/bookings/{id}`
- `DELETE /api/bookings/{id}`

Nên thể hiện:

- cần JWT
- có `@CurrentUser`
- booking thuộc user hiện tại

---

### 6.5 `UserController`

File: `src/main/java/com/example/bookingapi/controller/UserController.java`

Endpoint:

- `GET /api/users/me`
- `GET /api/users/{username}`
- `GET /api/users/checkUsernameAvailability`
- `GET /api/users/checkEmailAvailability`

Nên document:

- `/me` cần JWT
- các route còn lại public

---

## 7. Cách mô tả request / response trong Swagger

### Ví dụ request body

Nên mô tả DTO theo field thật trong code:

- `LoginRequest`
- `SignUpRequest`
- `HotelRequest`
- `RoomRequest`
- `BookingRequest`
- các DTO OTP nếu có

### Ví dụ response

Nên hiển thị rõ:

- `ApiResponse`
- `JwtAuthResponse`
- `PagedResponse<T>`
- `UserSummary`
- `UserProfile`

### Lỗi nên mô tả

- `400 Bad Request` — validation fail
- `401 Unauthorized` — chưa login / token sai
- `403 Forbidden` — thiếu quyền admin
- `404 Not Found` — resource không tồn tại

---

## 8. Cấu hình bảo mật cho Swagger UI

Vì BookingAPI là stateless JWT, cần chú ý:

### 8.1 Cho phép Swagger UI public

Nên whitelist các route Swagger trong security config, ví dụ:

- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/swagger-ui.html`

### 8.2 JWT token scheme

Nên khai báo security scheme kiểu HTTP bearer để Swagger hiểu định dạng token.

---

## 9. Quy ước document cho team

Khi annotate endpoint, nên thống nhất:

1. luôn ghi rõ public / protected / admin
2. luôn có example request
3. luôn có mô tả response status chính
4. luôn document các field bắt buộc
5. luôn mô tả `Authorization: Bearer <token>` cho endpoint cần login

---

## 10. Checklist triển khai Swagger sau này

### Bước 1
Thêm dependency `springdoc-openapi`.

### Bước 2
Tạo config OpenAPI + security scheme Bearer.

### Bước 3
Whitelist Swagger routes trong `SecurityConfig`.

### Bước 4
Annotate các controller hiện có bằng `@Tag`, `@Operation`, `@ApiResponses`.

### Bước 5
Annotate DTO bằng `@Schema`.

### Bước 6
Mở Swagger UI và test luồng:

- signup
- signin
- authorize JWT
- gọi endpoint protected/admin

---

## 11. Gợi ý cấu trúc tài liệu hóa endpoint

Một endpoint nên có các phần:

- Method + path
- Mô tả ngắn
- Auth requirement
- Request body / params
- Response 200 / 201
- Error 400 / 401 / 403 / 404
- Example JSON

---

## 12. Kết luận

BookingAPI hiện chưa tích hợp Swagger/OpenAPI, nhưng việc thêm Swagger là rất phù hợp vì:

- hệ thống có nhiều endpoint auth, booking, hotel, room
- có JWT stateless
- có public / protected / admin route rõ ràng
- cần document request/response cho frontend và QA

Tài liệu này là hướng dẫn triển khai để sau này có thể thêm Swagger vào project một cách nhất quán với kiến trúc hiện tại.

