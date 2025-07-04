# CheckScam v2 - Environment Configuration

## Cấu hình biến môi trường

Dự án này đã được cấu hình để sử dụng biến môi trường để bảo mật thông tin nhạy cảm.

### Thiết lập ban đầu

Cập nhật các biến môi trường với thông tin thực tế của bạn:
- Thông tin database
- API keys (Gemini, OpenRouter, Google APIs, etc.)
- JWT secret
- Các cấu hình khác

### Các biến môi trường quan trọng

#### Database
- `DB_URL`: URL kết nối database MySQL
- `DB_USERNAME`: Tên đăng nhập database
- `DB_PASSWORD`: Mật khẩu database

#### Security
- `JWT_SECRET`: Khóa bí mật để ký JWT token
- `GOOGLE_RECAPTCHA_SECRET`: Secret key của Google reCAPTCHA

#### External APIs
- `GEMINI_API_KEY`: API key của Google Gemini
- `OPENROUTER_API_KEY`: API key của OpenRouter
- `VIRUSTOTAL_API_KEY`: API key của VirusTotal
- `URLSCAN_API_KEY`: API key của URLScan
- Và nhiều API keys khác...

### Chạy ứng dụng

Sau khi cấu hình biến môi trường, bạn có thể chạy ứng dụng như bình thường:

```bash
mvn spring-boot:run
```

### Lưu ý bảo mật

- File `.env` đã được thêm vào `.gitignore` để tránh commit lên repository
- Không bao giờ chia sẻ thông tin biến môi trường thực tế
- Trong production, sử dụng hệ thống quản lý secrets phù hợp

### Production

Trong môi trường production, bạn có thể:
1. Set biến môi trường trực tiếp trong hệ thống
2. Sử dụng Docker environment variables
3. Sử dụng Kubernetes ConfigMaps/Secrets
4. Sử dụng cloud provider secrets manager

Spring Boot sẽ tự động đọc các biến môi trường hệ thống.
