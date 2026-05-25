# Local MinIO Upload

Muc tieu:
- chay upload file local bang MinIO trong Docker
- app Spring Boot upload file len object storage qua S3-compatible API
- client upload file truoc, nhan `url` va `objectKey`, sau do dung metadata tra ve trong `HotelRequest.previewImage`

## Kien truc

Luong local:
1. chay MinIO bang Docker
2. backend goi MinIO Java SDK de tao bucket neu chua co
3. backend upload file vao bucket
4. backend tra ve metadata:
   - `bucket`
   - `objectKey`
   - `url`
   - `contentType`
   - `size`
5. client dung `url`, `bucket`, `objectKey`, `contentType`, `size` de gan vao `previewImage` khi tao/cap nhat hotel

Mac dinh local bucket se duoc bat public read de co the hien preview image truc tiep bang URL.

## Docker Compose

File: [docker-compose.minio.yml](/Users/datle/work/study/BookingApp/BookingAPI/docker-compose.minio.yml)

Chay local:

```bash
docker compose -f docker-compose.minio.yml up -d
```

MinIO endpoints:
- S3 API: `http://localhost:9000`
- Console: `http://localhost:9001`

Mac dinh credentials:
- user: `minioadmin`
- password: `minioadmin`

## Cau hinh app

Them vao `.env` hoac export environment variables:

```bash
MINIO_ENDPOINT=http://localhost:9000
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=booking-assets
MINIO_AUTO_CREATE_BUCKET=true
MINIO_PUBLIC_READ=true
```

## API upload

Endpoint:

```http
POST /api/files/upload
Authorization: Bearer <admin-token>
Content-Type: multipart/form-data
```

Form fields:
- `file`: bat buoc
- `folder`: tuy chon, mac dinh `hotel-images`

Vi du `curl`:

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer <admin-token>" \
  -F "file=@/path/to/preview.jpg" \
  -F "folder=hotel-images"
```

Response mau:

```json
{
  "bucket": "booking-assets",
  "objectKey": "hotel-images/2026/05/25/550e8400-e29b-41d4-a716-446655440000-preview.jpg",
  "url": "http://localhost:9000/booking-assets/hotel-images/2026/05/25/550e8400-e29b-41d4-a716-446655440000-preview.jpg",
  "contentType": "image/jpeg",
  "size": 182736
}
```

## API upload truc tiep cho hotel preview

Endpoint:

```http
POST /api/hotels/{hotelId}/preview-image
Authorization: Bearer <admin-token>
Content-Type: multipart/form-data
```

Form fields:
- `file`: bat buoc
- `altText`: tuy chon

Vi du `curl`:

```bash
curl -X POST http://localhost:8080/api/hotels/30000000-0000-0000-0000-000000000001/preview-image \
  -H "Authorization: Bearer <admin-token>" \
  -F "file=@/path/to/preview.jpg" \
  -F "altText=Grand Palace Hotel front view"
```

Ket qua:
- backend upload file vao MinIO folder `hotel-images/{hotelId}/...`
- backend cap nhat `previewImage` cua hotel
- backend luu `bucket`, `objectKey`, `contentType`, `size` de co the xoa object sau nay
- response tra ve `HotelResponse` moi nhat

## API xoa hotel preview

Endpoint:

```http
DELETE /api/hotels/{hotelId}/preview-image
Authorization: Bearer <admin-token>
```

Ket qua:
- neu preview image co `bucket` va `objectKey`, backend xoa object tu MinIO
- backend xoa row preview image trong database
- neu hotel khong co preview image, API tra ve success idempotent

## Dung voi Hotel API

Sau khi upload xong, lay metadata response va goi API tao hotel:

```json
{
  "name": "Grand Palace Hotel",
  "description": "Central business hotel",
  "location": {
    "country": "Vietnam",
    "city": "Ho Chi Minh City",
    "province": "Ho Chi Minh",
    "district": "District 1",
    "detail": "123 Nguyen Hue Street"
  },
  "previewImage": {
    "bucket": "booking-assets",
    "objectKey": "hotel-images/2026/05/25/550e8400-e29b-41d4-a716-446655440000-preview.jpg",
    "url": "http://localhost:9000/booking-assets/hotel-images/2026/05/25/550e8400-e29b-41d4-a716-446655440000-preview.jpg",
    "contentType": "image/jpeg",
    "size": 182736,
    "altText": "Grand Palace Hotel front view"
  }
}
```

## Ghi chu

- local/dev: co the de `MINIO_PUBLIC_READ=true`
- prod: nen can nhac dung private bucket + signed URL hoac endpoint proxy download
- `bucket` va `objectKey` co the null neu preview image la URL ngoai he thong; khi do API xoa preview chi xoa metadata trong database
