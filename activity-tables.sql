-- Tạo bảng activities trong database chính của CheckScam
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
    
    INDEX idx_created_at (created_at DESC),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_action_type (action_type, created_at DESC)
);

-- Insert sample activities
INSERT INTO activities (user_id, user_name, action_type, target_type, target_name, metadata) VALUES
(1, 'Nguyễn Thành Hưng', 'SCAN', 'website', 'Kiểm tra website nghi vấn: shopee-fake.com', '{"risk_level": "high", "scan_duration": "2.3s"}'),
(2, 'Xuân Dong Ho', 'CHECK', 'phone', 'Tra cứu số điện thoại: 0987654321', '{"result": "safe", "confidence": 95}'),
(3, 'Lê Văn Khải', 'UPLOAD', 'report', 'Báo cáo website lừa đảo mới phát hiện', '{"category": "phishing", "severity": "high"}'),
(4, 'CheckScam Admin', 'JOIN', 'community', 'Tham gia cộng đồng CheckScam', '{"referrer": "website"}'),
(5, 'Minh Tuấn', 'SCAN', 'qr_code', 'Quét mã QR nghi vấn từ poster', '{"risk_level": "medium", "contains_malware": false}');