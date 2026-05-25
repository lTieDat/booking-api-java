// =============================================================================
// HOTEL BOOKING PLATFORM — DB SCHEMA v3
// =============================================================================
//
// CHANGELOG từ v2
// ───────────────
// [PAY]  Thêm Payment, PaymentMethod, Refund
// [INV]  Thêm Invoice, InvoiceLine
// [HOLD] Thêm InventoryHold — tách "đang giữ" khỏi "available"
// [CXL]  Thêm CancellationPolicy, CancellationPolicyTier
// [TAX]  Thêm TaxConfig, BookingTax
// [SNAP] Snapshot đầy đủ: BookedRoom + Booking discount info
// [MISC] Booking.lastUpdated → updatedAt
//        BookingGuest thêm isPrimary
//        Service thêm hotelID (service thuộc hotel cụ thể)
//        room_status thêm 'out_of_order'
//        payment_status, refund_status, invoice_status enums mới
//
// =============================================================================


// ─── Enums ───────────────────────────────────────────────────────────────────

Enum booking_status {
pending      // chờ thanh toán — giữ inventory tạm (có expiredAt)
paid         // đã thanh toán — chờ khách sạn confirm
confirmed    // khách sạn xác nhận
checked_in   // khách đã nhận phòng
checked_out  // khách đã trả phòng
cancelled    // đã huỷ (chưa chắc hoàn tiền)
refunded     // đã hoàn tiền
no_show      // khách không đến
}

Enum room_status {
available
occupied
maintenance
cleaning
out_of_order  // [MISC] hỏng/sửa dài hạn, khác maintenance ngắn
inactive
}

Enum otp_purpose {
email_verification
password_reset
}

Enum guest_type {
guest
registered
}

Enum discount_type {
percentage
fixed
}

Enum discount_scope_type {
all
group
hotel
room_type
}

Enum manager_scope_type {
group
hotel
}

Enum actor_type {
user
manager
system
}

// [PAY]
Enum payment_status {
pending     // chờ xử lý
success     // thành công
failed      // thất bại
cancelled   // huỷ trước khi xử lý
refunded    // đã hoàn toàn bộ
partial_refunded
}

// [PAY]
Enum payment_method_type {
credit_card
debit_card
bank_transfer
e_wallet      // Momo, ZaloPay, VNPay...
cash          // thanh toán tại quầy
points        // loyalty points
}

// [PAY]
Enum refund_status {
pending
processing
success
failed
}

// [INV]
Enum invoice_status {
draft       // đang tích luỹ charges (folio mở)
issued      // đã xuất, chờ thanh toán
paid
voided
}

// [CXL]
Enum cancellation_charge_type {
none            // free cancel
percentage      // % tổng booking
fixed           // số tiền cố định
first_night     // bằng tiền 1 đêm đầu
total           // non-refundable — mất toàn bộ
}

// [TAX]
Enum tax_type {
vat
service_charge
city_tax
resort_fee
other
}

// [TAX]
Enum tax_apply_type {
percentage   // % trên subtotal
per_room_per_night
fixed_per_booking
}


// ─── Auth & Users ────────────────────────────────────────────────────────────

Table User {
id            uuid         [pk]
email         varchar(100) [unique, not null]
passwordHash  varchar(255) [not null]
firstName     varchar(50)
middleName    varchar(50)
lastName      varchar(50)
phone         varchar(20)
dateOfBirth   date
isVerified    boolean      [default: false]
createdAt     timestamp    [default: `now()`]
updatedAt     timestamp
}

Table OTPToken {
id        uuid        [pk]
userID    uuid        [ref: > User.id, not null]
token     varchar(10) [not null]
purpose   otp_purpose [not null]
expiresAt timestamp   [not null]
isUsed    boolean     [default: false]
createdAt timestamp   [default: `now()`]

indexes {
(userID, purpose, isUsed)
}
}

Table Manager {
id           uuid         [pk]
email        varchar(100) [unique, not null]
passwordHash varchar(255) [not null]
fullName     varchar(100)
isActive     boolean      [default: true]
createdAt    timestamp    [default: `now()`]
updatedAt    timestamp
}


// ─── Location ────────────────────────────────────────────────────────────────

Table Location {
id       uuid         [pk]
country  varchar(50)  [not null]
city     varchar(50)
province varchar(50)
district varchar(50)
detail   varchar(200)
}


// ─── Hotel Group & Hotels ────────────────────────────────────────────────────

Table HotelGroup {
id          uuid         [pk]
name        varchar(128) [not null]
description varchar(500)
logoUrl     varchar(300)
isActive    boolean      [default: true]
createdAt   timestamp    [default: `now()`]
updatedAt   timestamp
}

Table Hotel {
id         uuid         [pk]
groupID    uuid         [ref: > HotelGroup.id]
name       varchar(128) [not null]
branch     varchar(50)
locationID uuid         [ref: > Location.id, not null]
overview   varchar(500)
rating     numeric(3,2) [default: 0]  // 0.00–5.00
isActive   boolean      [default: true]
createdAt  timestamp    [default: `now()`]
updatedAt  timestamp
}

Table HotelManager {
managerID  uuid               [not null, ref: > Manager.id]
scopeType  manager_scope_type [not null]
scopeID    uuid               [not null]
assignedAt timestamp          [default: `now()`]

indexes {
(managerID, scopeType, scopeID) [pk]
}
}


// ─── Rooms ───────────────────────────────────────────────────────────────────

Table RoomType {
id           uuid         [pk]
hotelID      uuid         [ref: > Hotel.id, not null]

name         varchar(50)  [not null]
code         varchar(30)  [not null]

maxAdults    integer      [not null]
maxChildren  integer      [default: 0]
maxOccupancy integer      [not null]

bedType      varchar(50)
areaSqm      numeric(6,2)
description  varchar(500)

// Informational only — source of truth là Room records hoặc RoomInventory
totalUnits   integer

// [CXL] cancellation policy mặc định cho room type này
// override được ở level booking nếu cần
cancellationPolicyID uuid [ref: > CancellationPolicy.id]

rating       numeric(3,2) [default: 0]
isActive     boolean      [default: true]
createdAt    timestamp    [default: `now()`]
updatedAt    timestamp

indexes {
(hotelID, code) [unique]
}
}

Table Room {
id         uuid        [pk]
roomTypeID uuid        [ref: > RoomType.id, not null]

roomNo     varchar(10) [not null]
floor      varchar(10)

status     room_status [default: 'available']
isActive   boolean     [default: true]

createdAt  timestamp   [default: `now()`]
updatedAt  timestamp

indexes {
(roomTypeID, roomNo) [unique]
status
}
}

Table RoomPrice {
id            uuid          [pk]
roomTypeID    uuid          [ref: > RoomType.id, not null]
guestType     guest_type    [not null]

price         numeric(12,2) [not null]

effectiveFrom date          [not null]
effectiveTo   date          // null = open-ended

createdBy     uuid          [ref: > Manager.id]
createdAt     timestamp     [default: `now()`]
updatedAt     timestamp

indexes {
(roomTypeID, guestType, effectiveFrom) [unique]
}
}

// Source of truth số phòng còn nhận đặt theo ngày
// availableUnits = totalUnits - confirmed bookings - holds
Table RoomInventory {
id             uuid    [pk]
roomTypeID     uuid    [ref: > RoomType.id, not null]
date           date    [not null]

totalUnits     integer [not null]     // tổng phòng ngày đó (có thể thay đổi do maintenance)
availableUnits integer [not null]     // đã trừ confirmed + active holds
heldUnits      integer [default: 0]  // [HOLD] đang bị hold bởi pending bookings

// Invariant: availableUnits + heldUnits <= totalUnits
// app phải enforce khi update

indexes {
(roomTypeID, date) [unique]
}
}

// [HOLD] Giữ phòng tạm trong khi booking ở trạng thái pending
// Khi booking confirmed → giảm availableUnits, xoá/expire hold
// Khi booking cancelled/expired → release hold (tăng lại availableUnits)
Table InventoryHold {
id             uuid      [pk]

bookingID      uuid      [ref: > Booking.id, not null]
roomTypeID     uuid      [ref: > RoomType.id, not null]
date           date      [not null]

heldUnits      integer   [not null]

expiresAt      timestamp [not null]  // = Booking.expiredAt
releasedAt     timestamp             // null = còn active
isReleased     boolean   [default: false]

indexes {
(bookingID, roomTypeID, date) [unique]
(roomTypeID, date, isReleased)  // query holds đang active theo ngày
(isReleased, expiresAt)         // job dọn expired holds
}
}


// ─── Cancellation Policy ─────────────────────────────────────────────────────

// [CXL] Policy gắn với RoomType (hoặc override khi tạo booking)
// Một policy có nhiều tiers theo thời gian trước check-in
Table CancellationPolicy {
id          uuid         [pk]
hotelID     uuid         [ref: > Hotel.id, not null]

name        varchar(100) [not null]  // "Free cancel 24h", "Non-refundable"
description varchar(500)

isDefault   boolean      [default: false]  // policy mặc định của hotel
isActive    boolean      [default: true]

createdAt   timestamp    [default: `now()`]
updatedAt   timestamp
}

// Mỗi tier = một ngưỡng thời gian + mức phạt
// Ví dụ:
//   hoursBeforeCheckIn=72, chargeType=none        → huỷ trước 72h miễn phí
//   hoursBeforeCheckIn=24, chargeType=percentage, chargeValue=50  → huỷ 24–72h mất 50%
//   hoursBeforeCheckIn=0,  chargeType=total       → huỷ trong 24h mất toàn bộ
Table CancellationPolicyTier {
id           uuid                     [pk]
policyID     uuid                     [ref: > CancellationPolicy.id, not null]

// Tier này áp dụng khi huỷ cách check-in < hoursBeforeCheckIn giờ
// Sort DESC để tìm tier phù hợp: lấy tier đầu tiên có hours <= thực tế
hoursBeforeCheckIn integer            [not null]

chargeType   cancellation_charge_type [not null]
chargeValue  numeric(12,2)            // null nếu chargeType = none hoặc total

indexes {
(policyID, hoursBeforeCheckIn) [unique]
}
}


// ─── Tax & Fee ───────────────────────────────────────────────────────────────

// [TAX] Config thuế/phí áp dụng cho hotel
// Cho phép stack nhiều loại thuế (VAT + service charge + city tax...)
Table TaxConfig {
id          uuid           [pk]
hotelID     uuid           [ref: > Hotel.id, not null]

name        varchar(100)   [not null]  // "VAT 10%", "Service Charge 5%"
type        tax_type       [not null]
applyType   tax_apply_type [not null]

rate        numeric(6,4)   [not null]
// percentage: 0.1000 = 10%
// per_room_per_night / fixed: số tiền tuyệt đối

isInclusive boolean        [default: false]
// true  = giá đã bao gồm thuế (inclusive) — chỉ hiển thị, không cộng thêm
// false = thuế cộng thêm vào giá (exclusive)

isActive    boolean        [default: true]
createdAt   timestamp      [default: `now()`]
updatedAt   timestamp

indexes {
(hotelID, type)
}
}

// [TAX] Lưu thuế đã tính cho từng booking (snapshot — không bị ảnh hưởng khi TaxConfig thay đổi)
Table BookingTax {
id           uuid          [pk]
bookingID    uuid          [ref: > Booking.id, not null]
taxConfigID  uuid          [ref: > TaxConfig.id]  // nullable nếu config bị xoá

// Snapshot tại thời điểm booking
taxName      varchar(100)  [not null]
taxType      tax_type      [not null]
applyType    tax_apply_type [not null]
rate         numeric(6,4)  [not null]
isInclusive  boolean       [not null]

amount       numeric(12,2) [not null]  // số tiền thuế thực tế

indexes {
bookingID
}
}


// ─── Discounts ───────────────────────────────────────────────────────────────

Table Discount {
id              uuid           [pk]
code            varchar(50)    [unique, not null]
name            varchar(100)   [not null]

type            discount_type  [not null]
value           numeric(12,2)  [not null]

minOrderAmount  numeric(12,2)  [default: 0]
maxUsage        integer
maxUsagePerUser integer
usedCount       integer        [default: 0]

startDate       date           [not null]
endDate         date
isActive        boolean        [default: true]

createdBy       uuid           [ref: > Manager.id]
createdAt       timestamp      [default: `now()`]
updatedAt       timestamp
}

// Polymorphic scope — enforce ở application layer
// scopeType=all: scopeID null
// scopeType=group: scopeID = HotelGroup.id
// scopeType=hotel: scopeID = Hotel.id
// scopeType=room_type: scopeID = RoomType.id
Table DiscountScope {
id         uuid                [pk]
discountID uuid                [ref: > Discount.id, not null]
scopeType  discount_scope_type [not null]
scopeID    uuid
}


// ─── Guests & Bookings ───────────────────────────────────────────────────────

Table Guest {
id             uuid         [pk]
userID         uuid         [ref: > User.id]  // nullable — walk-in guest

firstName      varchar(50)  [not null]
middleName     varchar(50)
lastName       varchar(50)  [not null]

identifyCardNo varchar(30)
phone          varchar(20)
locationID     uuid         [ref: > Location.id]

createdAt      timestamp    [default: `now()`]
updatedAt      timestamp
}

Table Booking {
id             uuid           [pk]

hotelID        uuid           [ref: > Hotel.id, not null]
userID         uuid           [ref: > User.id]      // người đặt, nullable = guest checkout
guestID        uuid           [ref: > Guest.id]     // primary guest / contact person
discountID     uuid           [ref: > Discount.id]

// [CXL] Snapshot policy tại thời điểm đặt — policy gốc có thể thay đổi sau
cancellationPolicyID       uuid         [ref: > CancellationPolicy.id]
cancellationPolicySnapshot text         // JSON dump toàn bộ policy + tiers lúc booking

checkIn        date           [not null]
checkOut       date           [not null]

actualCheckIn  timestamp      // lúc thực sự check-in
actualCheckOut timestamp      // lúc thực sự check-out

numGuests      integer        [not null]

// ── Financials ──────────────────────────────────────────────────
subtotal       numeric(12,2)  [not null]   // tổng tiền phòng + service trước thuế/discount
discountAmount numeric(12,2)  [default: 0]
taxAmount      numeric(12,2)  [default: 0] // [TAX] tổng thuế (sum of BookingTax)
totalAmount    numeric(12,2)  [not null]   // subtotal - discountAmount + taxAmount
paidAmount     numeric(12,2)  [default: 0] // [PAY] tổng đã thanh toán thực tế
refundedAmount numeric(12,2)  [default: 0] // [PAY] tổng đã hoàn

// [SNAP] Snapshot discount tại thời điểm đặt
discountCodeSnapshot  varchar(50)
discountTypeSnapshot  discount_type
discountValueSnapshot numeric(12,2)

status         booking_status [default: 'pending']
note           varchar(300)

expiredAt      timestamp      // deadline thanh toán khi status=pending
createdAt      timestamp      [default: `now()`]
updatedAt      timestamp

indexes {
hotelID
userID
guestID
status
(hotelID, checkIn, checkOut)
(status, expiredAt)          // job xử lý booking expired
}
}

Table BookedRoom {
id         uuid          [pk]
bookingID  uuid          [ref: > Booking.id, not null]
roomTypeID uuid          [ref: > RoomType.id, not null]

quantity   integer       [not null]
unitPrice  numeric(12,2) [not null]  // giá per room per night tại thời điểm đặt

// [SNAP] Snapshot để hiển thị lịch sử kể cả khi RoomType bị sửa/xoá
roomTypeNameSnapshot varchar(50)
roomTypeCodeSnapshot varchar(30)
bedTypeSnapshot      varchar(50)
maxOccupancySnapshot integer

indexes {
bookingID
roomTypeID
}
}

Table BookingRoomAssignment {
id           uuid      [pk]
bookedRoomID uuid      [ref: > BookedRoom.id, not null]
roomID       uuid      [ref: > Room.id, not null]

assignedAt   timestamp [default: `now()`]
assignedBy   uuid      [ref: > Manager.id]
note         varchar(300)

indexes {
(bookedRoomID, roomID) [unique]
roomID
}
}

// [MISC] Multiple guests per booking — isPrimary để xác định người đại diện
Table BookingGuest {
bookingID uuid    [ref: > Booking.id, not null]
guestID   uuid    [ref: > Guest.id, not null]
isPrimary boolean [default: false]
// Chỉ 1 guest được isPrimary=true per booking — enforce ở app layer

indexes {
(bookingID, guestID) [pk]
(bookingID, isPrimary)
}
}


// ─── Payment ─────────────────────────────────────────────────────────────────

// [PAY] Lưu thông tin phương thức thanh toán của user (tokenized — không lưu số thẻ)
Table PaymentMethod {
id            uuid                [pk]
userID        uuid                [ref: > User.id, not null]

type          payment_method_type [not null]
provider      varchar(50)         // "stripe", "vnpay", "momo", "zalopay"

// Token từ payment gateway — không lưu số thẻ thật
providerToken varchar(255)        // card token, wallet account ref...
displayLabel  varchar(100)        // "Visa ****4242", "Momo 0901..."

isDefault     boolean             [default: false]
isActive      boolean             [default: true]

createdAt     timestamp           [default: `now()`]
updatedAt     timestamp

indexes {
userID
}
}

// [PAY] Một booking có thể thanh toán nhiều lần (partial, retry sau failed...)
Table Payment {
id               uuid                [pk]
bookingID        uuid                [ref: > Booking.id, not null]
paymentMethodID  uuid                [ref: > PaymentMethod.id]  // null nếu cash

amount           numeric(12,2)       [not null]
currency         varchar(3)          [default: 'VND']  // ISO 4217

status           payment_status      [not null, default: 'pending']
method           payment_method_type [not null]

// Thông tin từ payment gateway
gatewayProvider  varchar(50)         // "stripe", "vnpay"...
gatewayTxID      varchar(255)        // transaction ID từ gateway
gatewayResponse  text                // JSON response raw (để debug/reconcile)

paidAt           timestamp           // lúc payment thực sự success
failureReason    varchar(300)

createdAt        timestamp           [default: `now()`]
updatedAt        timestamp

indexes {
bookingID
gatewayTxID                        // lookup khi gateway callback
(status, createdAt)
}
}

// [PAY] Hoàn tiền — luôn link với payment gốc
Table Refund {
id            uuid          [pk]
paymentID     uuid          [ref: > Payment.id, not null]
bookingID     uuid          [ref: > Booking.id, not null]  // denorm để query nhanh

amount        numeric(12,2) [not null]   // <= Payment.amount
reason        varchar(300)

status        refund_status [not null, default: 'pending']

// Gateway info
gatewayRefundID  varchar(255)
gatewayResponse  text

requestedAt   timestamp     [default: `now()`]
processedAt   timestamp
completedAt   timestamp

requestedBy   actor_type    [not null]
requestorID   uuid          // userID hoặc managerID

indexes {
paymentID
bookingID
(status, requestedAt)
}
}


// ─── Invoice / Folio ─────────────────────────────────────────────────────────

// [INV] Invoice = tài liệu tài chính chính thức xuất cho khách
// Một booking thường có 1 invoice, nhưng có thể split (nhiều công ty thanh toán...)
Table Invoice {
id          uuid           [pk]
bookingID   uuid           [ref: > Booking.id, not null]

invoiceNo   varchar(50)    [unique, not null]  // số hoá đơn — format tuỳ hotel
status      invoice_status [default: 'draft']

// Thông tin người nhận hoá đơn (có thể khác người đặt — xuất cho công ty)
billedToName    varchar(200)
billedToAddress varchar(500)
billedToTaxCode varchar(50)   // mã số thuế nếu xuất VAT invoice

subtotal    numeric(12,2)  [not null]
taxAmount   numeric(12,2)  [default: 0]
discount    numeric(12,2)  [default: 0]
totalAmount numeric(12,2)  [not null]

issuedAt    timestamp
dueAt       timestamp
paidAt      timestamp

note        varchar(500)
createdAt   timestamp      [default: `now()`]
updatedAt   timestamp

indexes {
bookingID
invoiceNo
status
}
}

// [INV] Chi tiết từng dòng trong invoice
// Bao gồm: tiền phòng, service, thuế, discount, phí huỷ...
Table InvoiceLine {
id          uuid          [pk]
invoiceID   uuid          [ref: > Invoice.id, not null]

lineType    varchar(50)   [not null]
// 'room_charge'  — tiền phòng
// 'service'      — dịch vụ thêm
// 'tax'          — thuế
// 'discount'     — giảm giá
// 'cancellation_fee' — phí huỷ
// 'adjustment'   — điều chỉnh thủ công

description varchar(200)  [not null]  // "Deluxe Room x2 x 3 nights", "VAT 10%"

quantity    numeric(10,2) [default: 1]
unitPrice   numeric(12,2) [not null]
amount      numeric(12,2) [not null]  // quantity * unitPrice (có thể âm — discount/credit)

referenceID uuid          // link tuỳ chọn tới BookedRoom.id / BookingService.id / BookingTax.id

sortOrder   integer       [default: 0]  // thứ tự hiển thị trên hoá đơn

indexes {
invoiceID
}
}


// ─── Services ────────────────────────────────────────────────────────────────

Table Service {
id          uuid          [pk]
hotelID     uuid          [ref: > Hotel.id]  // [MISC] null = dịch vụ platform-wide
name        varchar(100)  [not null]
description varchar(300)
price       numeric(12,2) [default: 0]
isActive    boolean       [default: true]
createdAt   timestamp     [default: `now()`]
updatedAt   timestamp
}

Table BookingService {
id                  uuid          [pk]
bookingID           uuid          [ref: > Booking.id, not null]
serviceID           uuid          [ref: > Service.id, not null]

quantity            integer       [default: 1]

// [SNAP] Snapshot — bất biến kể cả khi Service thay đổi sau
serviceNameSnapshot varchar(100)  [not null]
unitPriceSnapshot   numeric(12,2) [not null]

indexes {
bookingID
}
}


// ─── Reviews ─────────────────────────────────────────────────────────────────

Table Review {
id        uuid          [pk]
bookingID uuid          [ref: > Booking.id, not null]
userID    uuid          [ref: > User.id, not null]
hotelID   uuid          [ref: > Hotel.id, not null]

stars     integer       [not null]  // CHECK: stars BETWEEN 1 AND 5
content   varchar(1000) [not null]

isVisible boolean       [default: true]
createdAt timestamp     [default: `now()`]
updatedAt timestamp

indexes {
(bookingID, userID, hotelID) [unique]
(hotelID, isVisible, stars)
}
}


// ─── Audit Trail ─────────────────────────────────────────────────────────────

Table BookingHistory {
id         uuid           [pk]
bookingID  uuid           [ref: > Booking.id, not null]

fromStatus booking_status
toStatus   booking_status [not null]

actorType  actor_type     [not null]
actorID    uuid           // null nếu actorType = system

note       varchar(300)
changedAt  timestamp      [default: `now()`]

indexes {
(bookingID, changedAt)
actorID
}
}


// =============================================================================
// NOTES — Business Rules cần enforce ở application layer
// =============================================================================
//
// INVENTORY
//   - Khi tạo Booking (pending): tạo InventoryHold, giảm RoomInventory.availableUnits
//   - Khi Booking → confirmed: xoá/expire InventoryHold, đã giảm availableUnits rồi nên không đổi
//   - Khi Booking → cancelled/expired: release InventoryHold, tăng lại availableUnits
//   - Job định kỳ: expire InventoryHold quá hạn, release units tương ứng
//
// PAYMENT
//   - Payment chỉ được tạo khi Booking.status = pending | confirmed
//   - Khi Payment → success: cộng vào Booking.paidAmount, chuyển Booking → paid nếu đủ
//   - Refund.amount <= Payment.amount gốc
//   - Tổng Refund của 1 Booking <= Booking.paidAmount
//
// CANCELLATION FEE
//   - Khi huỷ: tính phí theo CancellationPolicyTier dựa vào (now - checkIn)
//   - Phí huỷ = 0 → refund toàn bộ paidAmount
//   - Phí huỷ > 0 → refund (paidAmount - phí huỷ), tạo InvoiceLine lineType='cancellation_fee'
//
// DISCOUNT
//   - Kiểm tra Discount.usedCount < maxUsage trước khi apply
//   - Kiểm tra usage per user qua COUNT(Booking WHERE discountID = X AND userID = Y)
//   - Sau khi booking confirmed: tăng Discount.usedCount
//   - Snapshot code/type/value vào Booking khi apply
//
// INVOICE
//   - Invoice được tạo khi Booking → confirmed (status = draft)
//   - Cập nhật InvoiceLine khi có thêm service trong stay
//   - Invoice → issued khi checkout hoặc theo yêu cầu
//   - Invoice → paid khi Payment đủ
//
// TAX
//   - Tính thuế khi tạo booking, snapshot vào BookingTax
//   - Inclusive tax: không cộng thêm vào totalAmount, chỉ hiển thị breakdown
//   - Exclusive tax: cộng vào totalAmount
//   - taxAmount trong Booking = SUM(BookingTax.amount WHERE isInclusive = false)
//
// ============================================================================= 