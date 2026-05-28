# Migration Guide

Tài liệu này mô tả cách chạy Flyway migration thủ công trước khi build/test và cách thêm migration mới cho repo `BookingAPI`.

## Mục Tiêu

Repo hiện dùng:

- Flyway để quản lý schema và seed data
- Hibernate `validate` để kiểm tra entity khớp schema

Điều này có nghĩa là:

- database phải được migrate trước khi app boot thành công
- khi migration và entity lệch nhau, app sẽ fail sớm

## Migration Hiện Tại

Folder migration:

```text
src/main/resources/db/migration
```

Các file đang có:

- `V1__create_core_tables.sql`
- `V2__seed_demo_data.sql`

## Khi Nào Chạy Migration Thủ Công

Nên chạy Flyway thủ công trước khi build hoặc run app trong các tình huống:

- vừa pull code mới có migration mới
- vừa thêm/chỉnh migration SQL
- muốn cập nhật database local trước khi chạy `spring-boot:run`
- muốn kiểm tra schema history trước khi test bug liên quan DB

## Cách 1: Dùng Environment Variables `FLYWAY_*`

Đây là cách gọn nhất khi chạy thủ công với Maven Flyway plugin.

### Set biến môi trường

```bash
export FLYWAY_URL=jdbc:postgresql://localhost:5432/booking_api
export FLYWAY_USER=postgres
export FLYWAY_PASSWORD=postgres
```

### Xem trạng thái migration

```bash
./mvnw flyway:info
```

### Validate migration

```bash
./mvnw flyway:validate
```

### Chạy migration

```bash
./mvnw flyway:migrate
```

### Sau đó mới build/test

```bash
./mvnw test
```

hoặc:

```bash
./mvnw package
```

## Cách 2: Truyền trực tiếp qua `-Dflyway.*`

Hữu ích khi không muốn export env vars.

### Info

```bash
./mvnw \
  -Dflyway.url=jdbc:postgresql://localhost:5432/booking_api \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres \
  flyway:info
```

### Validate

```bash
./mvnw \
  -Dflyway.url=jdbc:postgresql://localhost:5432/booking_api \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres \
  flyway:validate
```

### Migrate

```bash
./mvnw \
  -Dflyway.url=jdbc:postgresql://localhost:5432/booking_api \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres \
  flyway:migrate
```

## Quy Trình Khuyến Nghị Trước Khi Build

Đây là flow nên dùng ở local:

1. kiểm tra migration đang có:

```bash
./mvnw flyway:info \
  -Dflyway.url=jdbc:postgresql://localhost:5432/booking_api \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres
```

2. validate checksum và trạng thái migration:

```bash
./mvnw flyway:validate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/booking_api \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres
```

3. chạy migration:

```bash
./mvnw flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/booking_api \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres
```

4. build hoặc test:

```bash
./mvnw test
```

## Thêm Migration Mới

### Naming convention

Flyway đang dùng versioned migration:

```text
V{version}__{description}.sql
```

Ví dụ:

- `V3__add_user_status_column.sql`
- `V4__seed_default_roles.sql`

### Quy tắc

- dùng version tăng dần
- description ngắn, rõ ý nghĩa
- không sửa file migration đã chạy ở downstream environment
- nếu cần thay đổi schema/data đã deploy, hãy tạo migration mới

### Ví dụ thêm cột mới

```sql
ALTER TABLE app_users
ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
```

Tên file:

```text
V3__add_status_to_app_users.sql
```

## Seed Data

Seed data hiện tại nằm trong:

```text
V2__seed_demo_data.sql
```

Nó đang tạo:

- user demo `demo@booking.local` / `secret123`
- 2 bản ghi mẫu trong `info_messages`

Khi cần seed reference data mới:

- thêm migration mới kiểu `V{n}__seed_...sql`
- không nên sửa lại seed migration cũ nếu đã được apply ở môi trường khác

## Các Lệnh Hữu Ích Khác

### Repair

Khi checksum trong `flyway_schema_history` lệch do ai đó sửa migration đã apply:

```bash
./mvnw flyway:repair \
  -Dflyway.url=jdbc:postgresql://localhost:5432/mydb \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres
```

Chỉ dùng khi bạn hiểu rõ nguyên nhân mismatch.

### Clean

```bash
./mvnw flyway:clean \
  -Dflyway.url=jdbc:postgresql://localhost:5432/booking_api \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres
```

`clean` sẽ xóa toàn bộ object trong schema. Không dùng cho database đang có dữ liệu quan trọng.

## Lưu Ý Với Test

Test trong repo dùng H2 với profile `test`.

Khi chạy:

```bash
./mvnw test
```

Spring Boot sẽ:

- khởi tạo H2 in-memory
- chạy Flyway migration lên H2
- để Hibernate `validate` schema

Bạn không cần migrate PostgreSQL local trước để chạy unit/integration test của repo.

## Gợi Ý Thực Tế Cho Team

Nếu đang làm feature có thay đổi DB, flow an toàn nhất là:

1. sửa entity/repository/service
2. viết migration SQL mới
3. chạy `flyway:validate`
4. chạy `flyway:migrate` trên DB local
5. chạy `./mvnw test`
6. chạy app và test endpoint thật

## Tài Liệu Tham Chiếu

- Flyway Maven Goal: [https://documentation.red-gate.com/fd/maven-goal-277579365.html](https://documentation.red-gate.com/fd/maven-goal-277579365.html)
- Flyway Migrate: [https://documentation.red-gate.com/fd/migrate-277578887.html](https://documentation.red-gate.com/fd/migrate-277578887.html)
- Flyway Validate: [https://documentation.red-gate.com/fd/validate-277578898.html](https://documentation.red-gate.com/fd/validate-277578898.html)
- Flyway Versioned Migrations: [https://documentation.red-gate.com/fd/versioned-migrations-273973333.html](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html)
