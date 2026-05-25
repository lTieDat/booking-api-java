# Backlog triển khai `design-db.md` cho BookingAPI

Tài liệu này là backlog triển khai theo đúng schema ở [`docs/design-db.md`](design-db.md). Mục tiêu là đi theo **thứ tự phụ thuộc** để tránh làm ngược và phải refactor nhiều lần.

## Nguyên tắc ưu tiên

1. Làm **core nền tảng** trước: enum, audit, base entity, migration.
2. Làm **identity/auth/actor** trước các nghiệp vụ có phân quyền.
3. Làm **master data** trước: location, hotel, room type, room, price.
4. Làm **booking + inventory hold** trước payment/invoice.
5. Làm **policy layer** sau khi booking core chạy ổn: cancellation, discount, tax.
6. Làm **financial flow** sau cùng: payment, refund, invoice.
7. Hoàn thiện **history, review, admin workflow** ở phase cuối.

---

## Epic 0 — Nền tảng dữ liệu

### Mục tiêu
Thiết lập chuẩn dữ liệu và convention dùng chung cho toàn hệ thống.

### Deliverables
- Chốt enum theo `design-db.md`
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
- `HotelGroup`
- `Hotel`
- `HotelManager`
- CRUD hotel group / hotel
- Gán manager theo scope

### Acceptance notes
- hotel thuộc đúng group và location
- manager scope được enforce ở application layer
- có index/constraint cho dữ liệu lookup thường dùng

---

## Epic 3 — Room catalog

### Mục tiêu
Chuẩn hóa cấu trúc phòng và giá cơ bản.

### Deliverables
- `RoomType`
- `Room`
- `RoomPrice`
- `Service`
- CRUD room type / room / price / service

### Acceptance notes
- `RoomType` là loại phòng bán cho khách
- `Room` là phòng vật lý cụ thể
- `roomNo` unique theo phạm vi hợp lý của hotel
- giá phòng có hiệu lực theo thời gian và guest type

---

## Epic 4 — Inventory + booking core

### Mục tiêu
Xây luồng đặt phòng lõi và chống overbooking.

### Deliverables
- `RoomInventory`
- `InventoryHold`
- `Booking`
- `BookedRoom`
- `BookingGuest`
- `BookingRoomAssignment`
- Booking state machine: pending / paid / confirmed / checked_in / checked_out / cancelled / refunded / no_show

### Acceptance notes
- tạo booking phải tạo hold inventory
- release inventory đúng khi cancelled / expired
- không để inventory âm
- booking có snapshot đủ để không phụ thuộc dữ liệu sau này
- chỉ 1 guest `isPrimary=true` trên mỗi booking

---

## Epic 5 — Cancellation / Discount / Tax

### Mục tiêu
Thêm lớp chính sách để tính phí và áp dụng khuyến mãi đúng nghiệp vụ.

### Deliverables
- `CancellationPolicy`, `CancellationPolicyTier`
- `Discount`, `DiscountScope`
- `TaxConfig`, `BookingTax`
- rule snapshot cho policy/discount/tax

### Acceptance notes
- phí huỷ tính được theo mốc thời gian trước check-in
- discount có scope rõ ràng: all / group / hotel / room_type
- tax inclusive/exclusive xử lý đúng
- snapshot không bị đổi khi config gốc thay đổi

---

## Epic 6 — Payment / Refund / Invoice

### Mục tiêu
Hoàn thiện luồng tài chính end-to-end.

### Deliverables
- `PaymentMethod`
- `Payment`
- `Refund`
- `Invoice`
- `InvoiceLine`

### Acceptance notes
- payment có trạng thái rõ ràng và có gateway trace
- refund không vượt quá amount đã thanh toán
- invoice có line item và trạng thái draft / issued / paid / voided
- booking paid/refunded khớp với dữ liệu payment/refund

---

## Epic 7 — History / Review / Admin workflow

### Mục tiêu
Hoàn thiện vận hành và hậu kiểm.

### Deliverables
- `BookingHistory`
- `Review`
- workflow admin/manager cho xác nhận, check-in, check-out, huỷ

### Acceptance notes
- mọi state transition có audit trail
- review chỉ hợp lệ khi booking đã hoàn tất
- manager action trace được bằng actor

---

## Thứ tự triển khai khuyến nghị theo sprint

### Sprint 1
- Epic 0
- Epic 1
- một phần Epic 2 (Location, HotelGroup)

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

- Schema chi tiết: [`docs/design-db.md`](design-db.md)
- Hướng dẫn Flyway: [`docs/migration-guide.md`](migration-guide.md)
- JPA & database integration: [`docs/jpa-database-guide.md`](jpa-database-guide.md)
- REST API walkthrough: [`docs/rest-api-walkthrough.md`](rest-api-walkthrough.md)

