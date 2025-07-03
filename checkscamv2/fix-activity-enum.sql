-- Cập nhật enum trong database để khớp với Java code
USE checkscam;

-- Cập nhật enum để thêm POST và REPORT
ALTER TABLE activities 
MODIFY COLUMN action_type ENUM('SCAN', 'CHECK', 'UPLOAD', 'JOIN', 'COMMENT', 'LIKE', 'SHARE', 'POST', 'REPORT') NOT NULL;

-- Hoặc nếu muốn thay thế hoàn toàn:
-- ALTER TABLE activities 
-- MODIFY COLUMN action_type ENUM('POST', 'REPORT', 'JOIN', 'SCAN', 'CHECK', 'UPLOAD', 'COMMENT', 'LIKE', 'SHARE') NOT NULL;
