-- Tạo bảng activities trong database CheckScam
USE checkscam;

CREATE TABLE IF NOT EXISTS activities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(255),
    user_avatar VARCHAR(500),
    action_type ENUM('SCAN', 'CHECK', 'UPLOAD', 'JOIN', 'COMMENT', 'LIKE', 'SHARE') NOT NULL,
    target_type VARCHAR(50),
    target_name VARCHAR(500) NOT NULL,
    target_url VARCHAR(500),
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_created_at (created_at DESC),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_action_type (action_type, created_at DESC),
    INDEX idx_user_action (user_id, action_type, created_at DESC)
);

-- Insert sample activities cho CheckScam
INSERT INTO activities (user_id, user_name, action_type, target_type, target_name, metadata) VALUES
(1, 'Nguyễn Thành Hưng', 'SCAN', 'website', 'Kiểm tra website nghi vấn: shopee-fake.com', '{"risk_level": "high", "scan_duration": "2.3s", "threats_found": 3}'),
(2, 'Xuân Dong Ho', 'CHECK', 'phone', 'Tra cứu số điện thoại: 0987654321', '{"result": "safe", "confidence": 95, "reports": 0}'),
(3, 'Lê Văn Khải', 'UPLOAD', 'report', 'Báo cáo website lừa đảo mới phát hiện', '{"category": "phishing", "severity": "high", "website": "fake-banking.vn"}'),
(4, 'CheckScam Admin', 'JOIN', 'community', 'Tham gia cộng đồng CheckScam', '{"referrer": "website", "registration_method": "email"}'),
(5, 'Minh Tuấn', 'SCAN', 'qr_code', 'Quét mã QR nghi vấn từ poster', '{"risk_level": "medium", "contains_malware": false, "redirect_url": "bit.ly/fake-123"}'),
(1, 'Nguyễn Thành Hưng', 'CHECK', 'email', 'Kiểm tra email lừa đảo từ ngân hàng giả mạo', '{"result": "scam", "confidence": 98, "email_type": "phishing"}'),
(2, 'Xuân Dong Ho', 'SCAN', 'link', 'Kiểm tra link nghi vấn từ SMS', '{"risk_level": "high", "link_type": "shortened", "destination": "malicious-site.com"}'),
(3, 'Lê Văn Khải', 'COMMENT', 'post', 'Bình luận về bài viết cảnh báo lừa đảo', '{"comment_length": 85, "post_type": "warning"}'),
(4, 'CheckScam Admin', 'UPLOAD', 'news', 'Đăng bài cảnh báo thủ đoạn lừa đảo mới', '{"category": "news", "views": 1250, "shares": 45}'),
(5, 'Minh Tuấn', 'SCAN', 'website', 'Kiểm tra trang web thương mại điện tử', '{"risk_level": "low", "scan_duration": "1.8s", "ssl_valid": true}');

-- Tạo view để thống kê nhanh
CREATE OR REPLACE VIEW activity_stats AS
SELECT 
    COUNT(*) as total_activities,
    COUNT(CASE WHEN action_type = 'SCAN' THEN 1 END) as total_scans,
    COUNT(CASE WHEN action_type = 'CHECK' THEN 1 END) as total_checks,
    COUNT(CASE WHEN action_type = 'UPLOAD' THEN 1 END) as total_reports,
    COUNT(CASE WHEN DATE(created_at) = CURDATE() THEN 1 END) as today_activities,
    COUNT(CASE WHEN JSON_EXTRACT(metadata, '$.risk_level') = 'high' THEN 1 END) as high_risk_activities
FROM activities;