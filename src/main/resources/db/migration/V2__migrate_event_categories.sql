-- 1. Tạo bảng trung gian event_categories
CREATE TABLE event_categories (
    event_id UUID NOT NULL,
    category_id UUID NOT NULL,
    PRIMARY KEY (event_id, category_id),
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- 2. Di chuyển dữ liệu từ cột category_id sang bảng event_categories
INSERT INTO event_categories (event_id, category_id)
SELECT id AS event_id, category_id
FROM events
WHERE category_id IS NOT NULL;

-- 3. Xóa cột category_id cũ trong bảng events
ALTER TABLE events DROP COLUMN category_id;
