# Forum Module — API Documentation

> AIStudyHub SU26_SWP391 — Module Forum (thảo luận, bình luận, tương tác, kiểm duyệt)
> Base URL: `http://localhost:8080`
> Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 1. Xác thực (Authentication)

Tất cả endpoint **ghi** (POST/PUT/DELETE) yêu cầu JWT Bearer token. Các endpoint **đọc** (GET danh sách / chi tiết bài / bình luận) cho phép Guest.

### Lấy token

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "forum_a@test.vn", "password": "Password1" }
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresInMs": 900000,
    "user": { "id": "0d7977a7-...", "email": "forum_a@test.vn", "role": "ROLE_CUSTOMER" }
  }
}
```

Dùng token cho các request tiếp theo:
```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

> **Lưu ý:** Forum controller tự lấy `userId` từ JWT qua `SecurityContext` — **không cần** gửi header `X-User-Id`.

### Tài khoản test

| Email | Password | Role |
|-------|----------|------|
| `forum_a@test.vn` | `Password1` | CUSTOMER |
| `forum_b@test.vn` | `Password1` | CUSTOMER |
| `forum_admin@test.vn` | `Password1` | ADMIN |

---

## 2. Posts — Bài đăng

### `GET /api/v1/forum/posts` — Danh sách (Guest OK)

Query params: `categoryId` (UUID, optional), `sort` (`newest`|`popular`, default `newest`), `page` (default 0), `size` (default 10).

```http
GET /api/v1/forum/posts?sort=newest&page=0&size=10
```

**Response 200:**
```json
{
  "content": [
    {
      "postId": "905e8c51-e292-49c6-97c2-eba4c8cca656",
      "title": "Cach hoc Spring Boot hieu qua",
      "authorId": "0d7977a7-15ab-440a-8fb9-c37417f99d03",
      "authorName": "User A",
      "categoryName": "Hoc tap",
      "tags": ["spring", "java"],
      "status": "NORMAL",
      "likeCount": 0,
      "commentCount": 0,
      "viewCount": 0,
      "createdAt": "2026-06-23T03:40:20.744865Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 10
}
```

### `GET /api/v1/forum/posts/search` — Tìm kiếm (Guest OK)

Query params: `keyword` (required), `page`, `size`.

```http
GET /api/v1/forum/posts/search?keyword=spring&page=0&size=10
```

Trả về cùng cấu trúc `Page<PostSummaryResponse>` như trên.

### `GET /api/v1/forum/posts/{id}` — Chi tiết (Guest OK, +1 view)

```http
GET /api/v1/forum/posts/905e8c51-e292-49c6-97c2-eba4c8cca656
```

**Response 200:**
```json
{
  "postId": "905e8c51-e292-49c6-97c2-eba4c8cca656",
  "title": "Cach hoc Spring Boot hieu qua",
  "content": "Minh muon hoi cach hoc Spring Boot tu co ban",
  "authorId": "0d7977a7-15ab-440a-8fb9-c37417f99d03",
  "authorName": "User A",
  "categoryId": "677c3dfe-1437-4e47-9e03-4ee309a4a699",
  "categoryName": "Hoc tap",
  "tags": ["spring", "java"],
  "status": "NORMAL",
  "likeCount": 0,
  "commentCount": 0,
  "viewCount": 1,
  "likedByMe": false,
  "bookmarkedByMe": false,
  "attachedDocuments": [],
  "createdAt": "2026-06-23T03:40:20.744865Z",
  "updatedAt": "2026-06-23T03:40:20.744865Z"
}
```

### `POST /api/v1/forum/posts` — Tạo bài 🔒 (CUSTOMER/MODERATOR/ADMIN)

```http
POST /api/v1/forum/posts
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Cach hoc Spring Boot hieu qua",
  "content": "Minh muon hoi cach hoc Spring Boot tu co ban",
  "categoryId": "677c3dfe-1437-4e47-9e03-4ee309a4a699",
  "tags": ["spring", "java"],
  "documentIds": []
}
```
- `title` (required, ≤255), `content` (required), `categoryId` (optional), `tags` (optional — tự tạo nếu chưa có), `documentIds` (optional — gắn tài liệu từ thư viện cá nhân).

**Response 200:**
```json
{
  "success": true,
  "message": "Tạo bài đăng thành công.",
  "data": { "postId": "905e8c51-...", "title": "...", "status": "NORMAL", "...": "..." }
}
```

### `PUT /api/v1/forum/posts/{id}` — Sửa bài 🔒 (chỉ chủ bài)

```http
PUT /api/v1/forum/posts/905e8c51-e292-49c6-97c2-eba4c8cca656
Authorization: Bearer <token>

{ "title": "Tieu de moi", "content": "Noi dung da cap nhat" }
```
- Không phải chủ bài → **403 Access denied**.

### `DELETE /api/v1/forum/posts/{id}` — Xóa bài 🔒 (chỉ chủ bài)

```http
DELETE /api/v1/forum/posts/905e8c51-e292-49c6-97c2-eba4c8cca656
Authorization: Bearer <token>
```
- Response 200: `{ "success": true, "message": "Xóa bài đăng thành công." }`

---

## 3. Comments — Bình luận

### `GET /api/v1/forum/posts/{postId}/comments` — Danh sách (Guest OK)

Query: `page`, `size` (default 20). Trả `Page<CommentResponse>`, root comment kèm `replies`.

```json
{
  "content": [
    {
      "commentId": "2c67bbdb-2810-4110-934e-aca94b272fb7",
      "postId": "905e8c51-...",
      "authorId": "...",
      "authorName": "User B",
      "parentId": null,
      "quotedCommentId": null,
      "quotedContent": null,
      "content": "Comment cua B",
      "likeCount": 0,
      "likedByMe": false,
      "replies": [],
      "createdAt": "2026-06-23T03:..."
    }
  ],
  "totalElements": 1
}
```

### `POST /api/v1/forum/posts/{postId}/comments` — Bình luận 🔒

```http
POST /api/v1/forum/posts/905e8c51-.../comments
Authorization: Bearer <token>

{ "content": "Comment dau tien" }
```

**Reply** (trả lời comment khác) — dùng `parentId`:
```json
{ "content": "Reply den comment B", "parentId": "2c67bbdb-2810-4110-934e-aca94b272fb7" }
```

**Quote** (trích dẫn) — dùng `quotedCommentId`:
```json
{ "content": "Trich dan comment B", "quotedCommentId": "2c67bbdb-2810-4110-934e-aca94b272fb7" }
```

> ⚠️ Field đúng là **`parentId`** (không phải `parentCommentId`). Reply được làm phẳng 2 cấp — reply của reply vẫn gắn vào root comment.

### `PUT /api/v1/forum/comments/{commentId}` — Sửa 🔒 (chỉ chủ)
```json
{ "content": "Comment da sua" }
```

### `DELETE /api/v1/forum/comments/{commentId}` — Xóa 🔒 (chỉ chủ)

---

## 4. Interactions — Like / Bookmark / Report

> Toàn bộ controller này yêu cầu đăng nhập (CUSTOMER/MODERATOR/ADMIN).

### `POST /api/v1/forum/posts/{postId}/like` — Like/Unlike bài (toggle) 🔒
```json
{ "success": true, "message": "Đã thích.", "data": { "liked": true, "likeCount": 1 } }
```
Gọi lại lần nữa → `liked: false` (bỏ thích).

### `POST /api/v1/forum/comments/{commentId}/like` — Like/Unlike comment (toggle) 🔒

### `POST /api/v1/forum/posts/{postId}/bookmark` — Lưu/Bỏ lưu bài (toggle) 🔒
```json
{ "success": true, "message": "Đã lưu bài đăng.", "data": true }
```

### `GET /api/v1/forum/bookmarks` — Danh sách bài đã lưu 🔒
Query: `page`, `size`. Trả `Page<PostSummaryResponse>`.

### `POST /api/v1/forum/reports` — Báo cáo nội dung 🔒
```http
POST /api/v1/forum/reports
Authorization: Bearer <token>

{
  "targetType": "POST",
  "targetId": "905e8c51-e292-49c6-97c2-eba4c8cca656",
  "reason": "Noi dung spam / vi pham"
}
```
- `targetType`: `POST` hoặc `COMMENT`
- Báo cáo trùng (cùng user + cùng target) → **409 Conflict**.

---

## 5. Notifications — Thông báo

> Yêu cầu đăng nhập. Notification được tạo tự động khi có người like/comment/reply/mention.

### `GET /api/v1/notifications` — Danh sách 🔒
Query: `page`, `size` (default 20).
```json
{
  "content": [
    {
      "notificationId": "...",
      "type": "COMMENT",
      "message": "User B đã bình luận bài viết của bạn",
      "actorId": "...",
      "actorName": "User B",
      "targetId": "905e8c51-...",
      "read": false,
      "createdAt": "2026-06-23T03:..."
    }
  ],
  "totalElements": 2
}
```

### `GET /api/v1/notifications/unread-count` — Số chưa đọc 🔒
```json
{ "success": true, "message": "OK", "data": 2 }
```

### `PUT /api/v1/notifications/{id}/read` — Đánh dấu đã đọc 🔒
### `PUT /api/v1/notifications/read-all` — Đánh dấu tất cả đã đọc 🔒

---

## 6. Categories — Danh mục

### `GET /api/v1/forum/categories` — Danh sách (Guest OK)
```json
[ { "categoryId": "677c3dfe-...", "name": "Hoc tap", "description": "..." } ]
```

### `POST /api/v1/forum/categories` — Tạo 🔒 (chỉ MODERATOR/ADMIN)
```json
{ "name": "Lap trinh", "description": "Thao luan ve lap trinh" }
```
- CUSTOMER tạo → **403**.

---

## 7. Documents — Chia sẻ tài liệu vào Forum

> ⚠️ Phần này phụ thuộc field `Document.isPublic` của Module Document — **đang chờ align**.

### `POST /api/v1/forum/posts/{postId}/documents` — Đính kèm tài liệu 🔒
```json
{ "documentIds": ["<doc-uuid>"], "commentId": null }
```

### `GET /api/v1/forum/posts/{postId}/documents` — Tài liệu của bài (Guest OK)
### `GET /api/v1/forum/comments/{commentId}/documents` — Tài liệu của comment (Guest OK)
### `GET /api/v1/forum/documents/{documentId}/preview` — Xem trước (Guest OK)
### `GET /api/v1/forum/documents/{documentId}/download` — Tải về (Guest OK)

---

## 8. Moderation — Kiểm duyệt

> Toàn bộ controller này yêu cầu role **MODERATOR** hoặc **ADMIN**. User thường → **403**.

### `GET /api/v1/forum/moderation/reports` — Danh sách báo cáo 🔒
Query: `status` (`PENDING`|`RESOLVED`|`DISMISSED`, optional), `page`, `size`.
```json
{
  "content": [
    {
      "reportId": "...",
      "targetType": "POST",
      "targetId": "905e8c51-...",
      "reason": "spam",
      "status": "PENDING",
      "reporterName": "User B",
      "createdAt": "..."
    }
  ]
}
```

### `PUT /api/v1/forum/moderation/reports/{reportId}` — Xử lý báo cáo 🔒
```json
{ "status": "RESOLVED", "hideContent": false }
```
- `status`: `RESOLVED` | `DISMISSED`
- `hideContent: true` → ẩn bài / xóa comment bị báo cáo.

### `PUT /api/v1/forum/moderation/posts/{postId}/pin?pinned=true` — Ghim / bỏ ghim 🔒
### `DELETE /api/v1/forum/moderation/posts/{postId}` — Xóa bài (mod) 🔒
### `DELETE /api/v1/forum/moderation/comments/{commentId}` — Xóa comment (mod) 🔒

---

## 9. Mã lỗi chung

| HTTP | Ý nghĩa |
|------|---------|
| `200` | Thành công |
| `400` | Validation lỗi (thiếu field, sai định dạng) |
| `401/403` | Chưa đăng nhập / không đủ quyền |
| `404` | Không tìm thấy resource |
| `409` | Trùng lặp (vd: báo cáo cùng target 2 lần) |
| `500` | Lỗi server |

Format lỗi:
```json
{ "success": false, "message": "Access denied", "data": null }
```

---

## 10. Trạng thái test (cập nhật 2026-06-23)

| Nhóm | Endpoint | Kết quả |
|------|----------|---------|
| Posts | CRUD + search + view count | ✅ PASS |
| Comments | add / reply / quote / sửa / xóa | ✅ PASS |
| Interactions | like / bookmark / report | ✅ PASS |
| Notifications | list / unread / mark read | ✅ PASS |
| Categories | list / create | ✅ PASS |
| Moderation | reports / handle / pin / delete | ✅ PASS |
| Documents | attach / preview / download | ⏳ Chờ align `Document.isPublic` |
