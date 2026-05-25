# Hướng dẫn JPA & Database Integration cho BookingAPI

Tài liệu này giải thích cách **JPA**, **Flyway**, **auditing** và **quan hệ entity** đang phối hợp trong BookingAPI. Mục tiêu là giúp bạn hiểu:

- vì sao repo dùng `ddl-auto=validate`
- schema trong database được tạo như thế nào
- entity nào map với bảng nào
- dữ liệu audit `created_at`, `updated_at`, `created_by`, `updated_by` được ghi ra sao
- repository đang truy vấn theo quan hệ thế nào

Nếu bạn muốn xem phần thao tác Flyway chi tiết hơn, đọc thêm [`docs/migration-guide.md`](migration-guide.md).

---

## 1. Tư duy tổng thể

Trong BookingAPI, luồng dữ liệu đi theo dạng:

```text
Controller -> Service -> Repository -> JPA Entity -> Database
```

Và ngược lại khi đọc dữ liệu:

```text
Database -> JPA Entity -> Repository -> Service -> Controller -> JSON response
```

### Điểm quan trọng

- **Flyway** chịu trách nhiệm tạo và thay đổi schema
- **JPA/Hibernate** chỉ dùng để map entity với bảng có sẵn
- **Hibernate không tự tạo bảng** vì `spring.jpa.hibernate.ddl-auto=validate`
- **Auditing** tự điền dữ liệu thời gian và người thao tác

---

## 2. Cấu hình JPA trong `application.properties`

File: `src/main/resources/application.properties`

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
spring.flyway.locations=classpath:db/migration
```

### Ý nghĩa từng dòng

#### `ddl-auto=validate`

Hibernate sẽ:

- kiểm tra entity có khớp với schema không
- không tự tạo bảng
- không tự sửa bảng

Nếu entity và schema lệch nhau, app fail sớm lúc startup.

Đây là cách rất tốt để tránh “chạy được ở local nhưng lỗi ở production”.

#### `open-in-view=false`

Tắt cơ chế Open Session in View.

Ý nghĩa:

- entity lazy loading không nên bị truy cập tùy tiện ở tầng view/controller
- service nên load dữ liệu cần thiết trước khi trả response
- tránh query ngầm khó kiểm soát

#### `hibernate.jdbc.time_zone=UTC`

Các cột thời gian được chuẩn hóa theo UTC để tránh lệch timezone giữa local, test, production.

#### `flyway.locations`

Flyway đọc migration từ:

```text
src/main/resources/db/migration
```

---

## 3. Flyway và schema hiện tại

### 3.1 Migration legacy và migration chính

Repo hiện có 3 file migration:

- `V1__create_core_tables.sql`
- `V2__seed_demo_data.sql`
- `V3__add_core_tables.sql`

### Lưu ý rất quan trọng

`V1` và `V2` là schema/seed legacy cho các bảng cũ như:

- `app_users`
- `auth_refresh_tokens`
- `info_messages`

Trong khi các entity hiện tại của BookingAPI đang dùng schema chính ở `V3`:

- `users`
- `roles`
- `user_roles`
- `hotels`
- `rooms`
- `bookings`

Nói ngắn gọn:

- `V1`/`V2` là dữ liệu lịch sử của repo
- `V3` là phần schema chính đang khớp với entity hiện tại

### 3.2 Bảng chính trong V3

```sql
roles
users
user_roles
hotels
rooms
bookings
```

### Quan hệ chính

- `users` ↔ `roles`: many-to-many qua `user_roles`
- `hotels` → `rooms`: one-to-many
- `users` → `bookings`: many-to-one
- `rooms` → `bookings`: many-to-one

---

## 4. Entity mapping: class nào map với bảng nào

### 4.1 `User`

File: `src/main/java/com/example/bookingapi/model/User.java`

Map với bảng `users`.

Các điểm chính:

- `@Entity`
- `@Table(name = "users")`
- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`
- `username` và `email` có unique constraint
- quan hệ many-to-many với `Role`

#### 4.1.1 Quan hệ many-to-many `User` – `Role`

Trong BookingAPI, quan hệ n-n giữa user và role được map bằng `@ManyToMany` ở phía `User`:

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(name = "user_roles",
	joinColumns = @JoinColumn(name = "user_id"),
	inverseJoinColumns = @JoinColumn(name = "role_id"))
private Set<Role> roles = new HashSet<>();
```

`Role.java` được giữ rất gọn vì đây là phía còn lại của quan hệ:

- entity `Role` map vào bảng `roles`
- cột `name` lưu giá trị enum `RoleName`
- migration `V3__add_core_tables.sql` seed sẵn `ROLE_USER` và `ROLE_ADMIN`

### Bảng trung gian `user_roles`

Bảng này chỉ làm nhiệm vụ liên kết:

- khóa chính kép: `(user_id, role_id)`
- `user_id` tham chiếu `users.id`
- `role_id` tham chiếu `roles.id`
- không có thêm cột nghiệp vụ riêng

Vì vậy, `@ManyToMany` là lựa chọn đúng và gọn nhất.

### Khi nào cần entity trung gian riêng?

Nếu bảng liên kết có thêm dữ liệu như:

- `assigned_at`
- `assigned_by`
- `status`
- `note`

thì không nên dùng `@ManyToMany` nữa. Lúc đó nên tách ra thành một entity riêng, ví dụ `UserRole`.

#### Ý nghĩa thực tế

Khi signup:

- password được mã hóa
- user mới được gán role mặc định `ROLE_USER`
- user được lưu vào bảng `users`
- mapping nhiều-nhiều với `roles` lưu ở `user_roles`

### 4.2 `Role`

File: `src/main/java/com/example/bookingapi/model/Role.java`

Map với bảng `roles`.

- `name` là enum `RoleName`
- giá trị thực tế thường là `ROLE_USER` và `ROLE_ADMIN`

### 4.3 `Hotel`

File: `src/main/java/com/example/bookingapi/model/Hotel.java`

Map với bảng `hotels`.

Hotel kế thừa `UserDateAudit`, nên có:

- `created_at`
- `updated_at`
- `created_by`
- `updated_by`

Quan hệ:

- `@OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)` với `Room`

#### Ý nghĩa

- xóa hotel sẽ xóa luôn các room con
- room là phần phụ thuộc hotel

### 4.4 `Room`

File: `src/main/java/com/example/bookingapi/model/Room.java`

Map với bảng `rooms`.

Quan hệ:

- `@ManyToOne(fetch = FetchType.LAZY)` với `Hotel`
- `hotel_id` là foreign key bắt buộc

#### Ý nghĩa

Mỗi room phải thuộc về đúng một hotel.

`@JsonIgnore` trên field `hotel` giúp tránh vòng lặp JSON khi trả response.

### 4.5 `Booking`

File: `src/main/java/com/example/bookingapi/model/Booking.java`

Map với bảng `bookings`.

Quan hệ:

- `@ManyToOne(fetch = FetchType.LAZY)` với `User`
- `@ManyToOne(fetch = FetchType.LAZY)` với `Room`

#### Ý nghĩa

Một booking phải gắn với:

- một user đang đặt
- một room được đặt

Booking cũng kế thừa `UserDateAudit`, nên lưu được metadata tạo/cập nhật.

---

## 5. Auditing: created_at, updated_at, created_by, updated_by

### 5.1 `DateAudit`

File: `src/main/java/com/example/bookingapi/model/audit/DateAudit.java`

Class này là `@MappedSuperclass`, chứa:

- `createdAt`
- `updatedAt`

Được gắn bởi:

- `@CreatedDate`
- `@LastModifiedDate`

### 5.2 `UserDateAudit`

File: `src/main/java/com/example/bookingapi/model/audit/UserDateAudit.java`

Kế thừa `DateAudit` và thêm:

- `createdBy`
- `updatedBy`

Được gắn bởi:

- `@CreatedBy`
- `@LastModifiedBy`

### 5.3 `AuditingConfig`

File: `src/main/java/com/example/bookingapi/config/AuditingConfig.java`

`AuditorAware<Long>` lấy `userId` từ `SecurityContextHolder`.

Điều này nghĩa là:

- nếu request đã được authenticate bằng JWT
- `createdBy` và `updatedBy` sẽ nhận đúng id user hiện tại

### Ví dụ thực tế

Khi admin tạo hotel:

- `createdBy` = id của admin đang đăng nhập
- `createdAt` = thời điểm tạo
- `updatedBy` = id của admin
- `updatedAt` = thời điểm cập nhật gần nhất

Khi user tạo booking:

- `createdBy` = id user đó
- `createdAt` = thời điểm tạo booking

### Điểm cần nhớ

Nếu request không authenticate, `AuditorAware` trả `Optional.empty()`.

Những entity yêu cầu audit có thể fail nếu cột `nullable=false` mà giá trị không được gán đúng lúc.

---

## 6. Repository đang truy vấn thế nào

### 6.1 `UserRepository`

Có các method chính:

- `findByUsername(String username)`
- `findByUsernameOrEmail(String username, String email)`
- `existsByUsername(String username)`
- `existsByEmail(String email)`

#### Dùng ở đâu?

- login bằng username/email
- signup kiểm tra trùng username/email

### 6.2 `RoleRepository`

Có:

- `findByName(RoleName name)`

Dùng khi signup để gán role mặc định `ROLE_USER`.

### 6.3 `HotelRepository`

Có:

- `findByCity(String city, Pageable pageable)`

Dùng khi cần filter hotel theo thành phố.

### 6.4 `RoomRepository`

Có:

- `findByHotelId(Long hotelId)`
- `findByIdAndHotelId(Long id, Long hotelId)`

Ý nghĩa:

- lấy danh sách room của một hotel
- đảm bảo room đang truy cập đúng thuộc hotel đó

### 6.5 `BookingRepository`

Có:

- `findByUser(User user, Pageable pageable)`

Dùng để lấy booking của riêng user đang đăng nhập.

---

## 7. Validation ở tầng DTO và entity

BookingAPI dùng validation ở cả request DTO và entity.

### 7.1 Request DTO

Ví dụ:

- `SignUpRequest`
- `HotelRequest`
- `RoomRequest`
- `BookingRequest`

Các controller dùng `@Valid` để tự động validate trước khi vào service.

### 7.2 Ví dụ các rule chính

#### `SignUpRequest`

- `name`: 4–40 ký tự
- `username`: 3–15 ký tự
- `email`: đúng định dạng email
- `password`: 6–20 ký tự

#### `HotelRequest`

- `name`: bắt buộc
- `address`, `city`, `country`: giới hạn độ dài

#### `RoomRequest`

- `roomNumber`: bắt buộc
- `pricePerNight`: không được null

#### `BookingRequest`

- `roomId`: không được null
- `checkInDate`: không được null
- `checkOutDate`: không được null và phải là tương lai

### Tại sao vẫn cần validation ở entity?

Entity validation là lớp bảo vệ bổ sung khi data đi qua nhiều tầng hoặc khi entity được dùng ở context khác.

---

## 8. Luồng ghi dữ liệu mẫu

### 8.1 Signup user

1. `AuthController` nhận `SignUpRequest`
2. `AuthServiceImpl` kiểm tra trùng username/email
3. password được encode bằng BCrypt
4. user được gán `ROLE_USER`
5. `UserRepository.save(user)` ghi vào `users`
6. mapping `user_roles` được lưu tương ứng

### 8.2 Tạo hotel

1. admin gọi `POST /api/hotels`
2. `HotelController` kiểm tra `@PreAuthorize("hasRole('ADMIN')")`
3. service tạo `Hotel`
4. JPA lưu hotel vào bảng `hotels`
5. auditing tự điền `created_at`, `updated_at`, `created_by`, `updated_by`

### 8.3 Tạo booking

1. user đăng nhập
2. client gọi `POST /api/bookings`
3. controller nhận `@CurrentUser UserPrincipal currentUser`
4. service load `User` và `Room`
5. booking được lưu vào bảng `bookings`
6. audit fields được gán tự động

---

## 9. Những điểm cần lưu ý khi làm việc với JPA trong repo này

### 9.1 Không sửa migration đã chạy

Nếu schema đã được apply ở môi trường khác, đừng sửa file migration cũ.

Hãy tạo migration mới.

### 9.2 Entity và migration phải khớp

Vì `ddl-auto=validate`, chỉ cần lệch tên cột, kiểu dữ liệu hoặc nullable là app fail lúc boot.

### 9.3 Cẩn thận với lazy loading

Quan hệ `ManyToOne`/`OneToMany` ở đây chủ yếu là `LAZY`.

Vì `open-in-view=false`, nên service cần load đủ dữ liệu trước khi trả response.

### 9.4 Audit phụ thuộc SecurityContext

Nếu request chưa authenticate, `createdBy`/`updatedBy` sẽ không có giá trị từ `AuditorAware`.

### 9.5 Dữ liệu tiền tệ dùng `BigDecimal`

`pricePerNight` và `totalPrice` đều dùng `BigDecimal` để tránh sai số của số thực.

---

## 10. Ví dụ ánh xạ từ entity sang SQL

### `Hotel`

Entity:

```java
private String name;
private String description;
private String address;
private String city;
private String country;
```

Tương ứng trong SQL:

```sql
name VARCHAR(100) NOT NULL,
description TEXT,
address VARCHAR(200),
city VARCHAR(100),
country VARCHAR(100)
```

### `Room`

Entity:

```java
private String roomNumber;
private String roomType;
private Integer capacity;
private BigDecimal pricePerNight;
```

Tương ứng trong SQL:

```sql
room_number VARCHAR(20) NOT NULL,
room_type VARCHAR(50),
capacity INT,
price_per_night NUMERIC(10, 2)
```

### `Booking`

Entity:

```java
private LocalDate checkInDate;
private LocalDate checkOutDate;
private BigDecimal totalPrice;
private String status = "CONFIRMED";
```

Tương ứng trong SQL:

```sql
check_in_date DATE NOT NULL,
check_out_date DATE NOT NULL,
total_price NUMERIC(10, 2),
status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED'
```

---

## 11. Kết luận

Trong BookingAPI, JPA không đứng riêng lẻ mà được thiết kế để làm việc chặt chẽ với:

- **Flyway** để quản lý schema
- **Spring Security** để lấy user hiện tại cho auditing
- **DTO validation** để chặn dữ liệu sai từ đầu
- **Repository** để truy vấn theo đúng quan hệ domain

Cách tổ chức này giúp repo dễ hiểu, dễ test và dễ mở rộng khi thêm feature mới.


