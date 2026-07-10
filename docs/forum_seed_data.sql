-- ============================================================
-- FORUM SEED DATA - Mẫu thử cho forum_post & forum_post_revisions
-- Chạy trong Supabase SQL Editor (project AIStudyHub)
-- Không thay đổi schema, chỉ INSERT dữ liệu
-- user_id lấy từ bảng users có sẵn (nếu trống sẽ tự sinh UUID)
-- ============================================================

-- Lấy 2 user thật đầu tiên làm tác giả
WITH picked_users AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY created_at) AS rn
    FROM users
    LIMIT 2
),
u1 AS (SELECT COALESCE((SELECT user_id FROM picked_users WHERE rn = 1), gen_random_uuid()) AS id),
u2 AS (SELECT COALESCE((SELECT user_id FROM picked_users WHERE rn = 2),
                        (SELECT id FROM u1)) AS id)

INSERT INTO forum_post (id, document_id, user_id, user_name, visibility, title, content, status, is_pinned, created_at, updated_at)
VALUES
-- Bài 1: bài ghim, đã qua 2 lần chỉnh sửa (có revisions bên dưới)
('a1000000-0000-4000-8000-000000000001', NULL, (SELECT id FROM u1), 'Nguyễn Trọng Tín', 'PUBLIC',
 '[THÔNG BÁO] Nội quy diễn đàn AI Study Hub',
 'Chào mừng các bạn đến với diễn đàn! Vui lòng: 1) Không spam, 2) Đặt tiêu đề rõ ràng, 3) Tôn trọng thành viên khác. Bài vi phạm sẽ bị gỡ.',
 'ACTIVE', true, now() - interval '7 days', now() - interval '1 day'),

-- Bài 2: chia sẻ tài liệu
('a1000000-0000-4000-8000-000000000002', NULL, (SELECT id FROM u1), 'Nguyễn Trọng Tín', 'PUBLIC',
 'Chia sẻ bộ tài liệu ôn tập SWP391 - Application Development Project',
 'Mình vừa tổng hợp bộ tài liệu ôn SWP391 gồm slide, đề mẫu và checklist báo cáo. Bạn nào cần thì comment email nhé!',
 'ACTIVE', false, now() - interval '5 days', now() - interval '5 days'),

-- Bài 3: hỏi đáp, đã sửa 1 lần (có revision bên dưới)
('a1000000-0000-4000-8000-000000000003', NULL, (SELECT id FROM u2), 'Trần Minh Khoa', 'PUBLIC',
 'Hỏi về cách dùng tính năng Chat AI với tài liệu PDF',
 'Mình upload PDF lên hệ thống rồi nhưng khi chat AI không trả lời theo nội dung tài liệu. Có ai gặp lỗi này chưa? UPDATE: đã fix, do file PDF là dạng scan không có text layer.',
 'ACTIVE', false, now() - interval '3 days', now() - interval '2 days'),

-- Bài 4: bài riêng tư (PRIVATE)
('a1000000-0000-4000-8000-000000000004', NULL, (SELECT id FROM u2), 'Trần Minh Khoa', 'PRIVATE',
 'Ghi chú cá nhân: kế hoạch ôn thi cuối kỳ',
 'Tuần 1: PRJ301 + SWP391. Tuần 2: MLN122. Tuần 3: tổng ôn + mock test.',
 'ACTIVE', false, now() - interval '2 days', now() - interval '2 days'),

-- Bài 5: bài bị ẩn (HIDDEN) để test lọc trạng thái
('a1000000-0000-4000-8000-000000000005', NULL, (SELECT id FROM u1), 'Nguyễn Trọng Tín', 'PUBLIC',
 'Bán tài khoản khóa học giá rẻ!!!',
 'Liên hệ zalo 09xxxxx để mua tài khoản...',
 'HIDDEN', false, now() - interval '1 day', now() - interval '12 hours');

-- ============================================================
-- REVISIONS: lịch sử chỉnh sửa
-- ============================================================

WITH picked_users AS (
    SELECT user_id, ROW_NUMBER() OVER (ORDER BY created_at) AS rn
    FROM users
    LIMIT 2
),
u1 AS (SELECT COALESCE((SELECT user_id FROM picked_users WHERE rn = 1), gen_random_uuid()) AS id),
u2 AS (SELECT COALESCE((SELECT user_id FROM picked_users WHERE rn = 2),
                        (SELECT id FROM u1)) AS id)

INSERT INTO forum_post_revisions (id, post_id, revision_no, title, content, status, edited_by, edited_by_name, created_at)
VALUES
-- Bài 1 sửa 2 lần: bản gốc -> bản sửa lần 1 (bản hiện tại là lần 2)
('b1000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', gen_random_uuid(),
 'Nội quy diễn đàn',
 'Chào mừng các bạn đến với diễn đàn! Vui lòng không spam.',
 'ACTIVE', (SELECT id FROM u1), 'Nguyễn Trọng Tín', now() - interval '6 days'),

('b1000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000001', gen_random_uuid(),
 '[THÔNG BÁO] Nội quy diễn đàn',
 'Chào mừng các bạn đến với diễn đàn! Vui lòng: 1) Không spam, 2) Đặt tiêu đề rõ ràng.',
 'ACTIVE', (SELECT id FROM u1), 'Nguyễn Trọng Tín', now() - interval '1 day'),

-- Bài 3 sửa 1 lần: bản gốc trước khi thêm dòng UPDATE
('b1000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000003', gen_random_uuid(),
 'Hỏi về cách dùng tính năng Chat AI với tài liệu PDF',
 'Mình upload PDF lên hệ thống rồi nhưng khi chat AI không trả lời theo nội dung tài liệu. Có ai gặp lỗi này chưa?',
 'ACTIVE', (SELECT id FROM u2), 'Trần Minh Khoa', now() - interval '2 days');

-- ============================================================
-- KIỂM TRA KẾT QUẢ
-- ============================================================
SELECT p.title, p.status, p.is_pinned, p.visibility,
       (SELECT count(*) FROM forum_post_revisions r WHERE r.post_id = p.id) AS so_revision
FROM forum_post p
ORDER BY p.is_pinned DESC, p.created_at DESC;

-- Xóa dữ liệu mẫu khi cần:
-- DELETE FROM forum_post_revisions WHERE post_id::text LIKE 'a1000000%';
-- DELETE FROM forum_post WHERE id::text LIKE 'a1000000%';
