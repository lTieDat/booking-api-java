# Backlog triển khai `design-db.dbml` cho BookingAPI

Tài liệu này là backlog triển khai theo đúng schema ở [`docs/design-db.dbml`](design-db.dbml). Mục tiêu là đi theo **thứ tự phụ thuộc** để tránh làm ngược và phải refactor nhiều lần.

## Nguyên tắc ưu tiên

1. Làm **core nền tảng** trước: enum, audit, base entity, migration.
2. Làm **identity/auth/actor** trước các nghiệp vụ có phân quyền.
3. Làm **master data** trước: location, hotel, room type, room, price.
4. Làm **booking + inventory hold** trước payment/invoice.
5. Làm **policy layer** sau khi booking core chạy ổn: cancellation, discount, tax.
6. Làm **financial flow** sau cùng: payOS payment link, webhook, refund, invoice.
7. Hoàn thiện **history, review, admin workflow** ở phase cuối.

---

## Epic 0 — Nền tảng dữ liệu

### Mục tiêu
Thiết lập chuẩn dữ liệu và convention dùng chung cho toàn hệ thống.

### Deliverables
- Chốt enum theo `design-db.dbml`
- Chuẩn hóa `uuid`, `timestamp`, `numeric/BigDecimal`
- Base entity + auditing
- Chuẩn Flyway migration order
- Chuẩn validation DTO

### Acceptance notes
- `ddl-auto=validate` không báo lệch schema
- audit fields hoạt động đúng khi có authenticated actor
- không dùng `float` cho tiền

---

## Epic 1 — Identity / Auth / Actor

### Mục tiêu
Xây lớp xác thực và actor context để các nghiệp vụ khác có thể trace ai thao tác.

### Deliverables
- `User`, `Manager`, `OTPToken`
- `RoleName` hiện có `ROLE_USER`, `ROLE_ADMIN`; thêm `ROLE_RECEPTIONIST` khi triển khai receptionist workflow
- Signup/login user
- Login manager/admin
- Verify email / reset password
- Actor resolver: `user`, `manager`, `system`

### Acceptance notes
- Có thể xác định actor cho booking/payment/refund/history
- OTP có expiry và index phục vụ lookup nhanh
- user và manager tách rõ quyền

---

## Epic 2 — Location + Hotel structure

### Mục tiêu
Tạo master data cho hệ thống khách sạn.

### Deliverables
- `Location`
- `Hotel`
- `HotelImage`
- `ReceptionistAssignment` sau khi có `Hotel`
- CRUD location / hotel / hotel image
- Gán receptionist theo từng hotel

### Acceptance notes
- hotel thuộc đúng location
- receptionist chỉ thao tác được booking thuộc hotel được assign
- có index/constraint cho dữ liệu lookup thường dùng

---

## Epic 3 — Room catalog

### Mục tiêu
Chuẩn hóa cấu trúc phòng và giá cơ bản.

### Deliverables
- `RoomType`
- `Room`
- `Amenity`
- CRUD room type / room / amenity

### Acceptance notes
- `RoomType` là loại phòng bán cho khách
- `Room` là phòng vật lý cụ thể
- `room_number` unique theo `room_type_id`
- `base_price` nằm ở `RoomType`, giá tại thời điểm booking snapshot vào `BookedRoom.unit_price`

---

## Epic 4 — Inventory + booking core

### Mục tiêu
Xây luồng đặt phòng lõi và chống overbooking.

### Deliverables
- `RoomInventory`
- `InventoryHold`
- `Booking`
- `BookedRoom`
- `Guest`
- Booking state machine: pending / confirmed / checked_in / checked_out / cancelled / refunded / no_show

### Acceptance notes
- tạo booking phải tạo hold inventory
- release inventory đúng khi cancelled / expired
- không để inventory âm
- booking có snapshot đủ để không phụ thuộc dữ liệu sau này
- `Payment.status` quản lý trạng thái dòng tiền, không nhét `paid` vào `Booking.status`

---

## Epic 5 — Cancellation / Discount / Tax

### Mục tiêu
Thêm lớp chính sách để tính phí và áp dụng khuyến mãi đúng nghiệp vụ.

### Deliverables
- `CancellationPolicy`
- `Discount`
- `TaxConfig`, `BookingTax`
- rule snapshot cho policy/discount/tax

### Acceptance notes
- phí huỷ dùng policy đang gắn với booking tại thời điểm hủy
- discount có điều kiện min/max order và usage rõ ràng
- tax inclusive/exclusive xử lý đúng
- snapshot không bị đổi khi config gốc thay đổi

---

## Epic 6 — Payment / Refund / Invoice

### Mục tiêu
Hoàn thiện luồng tài chính end-to-end, ưu tiên tích hợp payOS trước.

### Deliverables
- `PaymentProviderAccount`
- `Payment`
- `PaymentTransaction`
- `PaymentWebhookEvent`
- `Refund`
- `Invoice`
- `InvoiceLine`
- payOS adapter: create payment link, get payment request, cancel payment link
- payOS webhook endpoint: verify signature, persist raw payload, process idempotent
- reconciliation job cho payment bị trễ webhook hoặc callback lỗi

### Acceptance notes
- payment lưu được `orderCode`, `paymentLinkId`, `checkoutUrl`, `qrCode`, `returnUrl`, `cancelUrl`
- webhook signature được verify trước khi cập nhật trạng thái nghiệp vụ
- returnUrl/cancelUrl chỉ dùng cho UX, không dùng làm nguồn sự thật trạng thái payment
- duplicate webhook/transaction không tạo thanh toán trùng
- payment có trạng thái rõ ràng và có provider trace
- refund không vượt quá amount đã thanh toán
- invoice có line item và trạng thái draft / issued / paid / voided
- booking paid/refunded khớp với dữ liệu payment/refund

---

## Epic 7 — History / Admin workflow

### Mục tiêu
Hoàn thiện vận hành và hậu kiểm.

### Deliverables
- `BookingStatusLog`
- workflow admin/manager/receptionist cho xác nhận, check-in, check-out, huỷ

### Acceptance notes
- mọi state transition có audit trail
- manager action trace được bằng actor
- receptionist action trace được bằng user và role snapshot

---

## Thứ tự triển khai khuyến nghị theo sprint

### Sprint 1
- Epic 0
- Epic 1
- một phần Epic 2 (Location, Hotel)

### Sprint 2
- phần còn lại của Epic 2
- Epic 3

### Sprint 3
- Epic 4

### Sprint 4
- Epic 5

### Sprint 5
- Epic 6

### Sprint 6
- Epic 7
- hardening, tests, migration polish

---

## Những invariant cần giữ trong toàn bộ backlog

- Không dùng `float` cho tiền
- Không để booking flow bỏ qua inventory hold
- Không để booking state đi ngược logic
- Không cho refund vượt payment
- Không cho review nếu không có booking hợp lệ
- Không để snapshot phụ thuộc vào dữ liệu mutable sau khi booking đã tạo

---

## Tài liệu tham chiếu

- Schema chi tiết: [`docs/design-db.dbml`](design-db.dbml)
- Hướng dẫn Flyway: [`docs/migration-guide.md`](migration-guide.md)
- JPA & database integration: [`docs/jpa-database-guide.md`](jpa-database-guide.md)
- REST API walkthrough: [`docs/rest-api-walkthrough.md`](rest-api-walkthrough.md)
