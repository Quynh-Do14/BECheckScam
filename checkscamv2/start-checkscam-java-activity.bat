@echo off
echo =====================================
echo   CheckScam với Activity Feed - Java Backend
echo =====================================
echo.

echo [1/8] Kiểm tra yêu cầu hệ thống...

:: Kiểm tra Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ ERROR: Java JDK không tìm thấy
    echo Vui lòng cài đặt Java JDK 17: https://adoptium.net/
    pause
    exit /b 1
)
echo ✅ Java JDK OK

:: Kiểm tra Maven
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ ERROR: Maven không tìm thấy
    echo Vui lòng cài đặt Maven: https://maven.apache.org/
    pause
    exit /b 1
)
echo ✅ Maven OK

:: Kiểm tra MySQL
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ WARNING: MySQL client không tìm thấy
    echo Activity Feed sẽ dùng embedded H2 database
) else (
    echo ✅ MySQL OK
)

echo.
echo [2/8] Khởi động MySQL service...
net start mysql >nul 2>&1

echo.
echo [3/8] Setup database cho Activity Feed...
cd C:\Users\ACER\Desktop\checkscam\checkscamv2\checkscamv2

:: Tạo database và bảng
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS checkscam CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
mysql -u root -p checkscam < setup-activity-tables.sql 2>nul
if %errorlevel% equ 0 (
    echo ✅ Database setup thành công
) else (
    echo ⚠️ Database setup có lỗi, sẽ dùng H2 database
)

echo.
echo [4/8] Build project Java...
call mvn clean compile
if %errorlevel% neq 0 (
    echo ❌ ERROR: Maven build failed
    pause
    exit /b 1
)
echo ✅ Java project build thành công

echo.
echo [5/8] Khởi động CheckScam Main Backend (với Activity Feed)...
start "CheckScam Backend" cmd /c "mvn spring-boot:run"

echo.
echo [6/8] Đợi backend khởi động (30 giây)...
timeout /t 30 /nobreak

echo.
echo [7/8] Test API endpoints...
curl -f http://localhost:8080/api/v1/activities/statistics >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Activity Feed API hoạt động
) else (
    echo ⚠️ Backend chưa sẵn sàng, vui lòng đợi thêm
)

echo.
echo [8/8] Khởi động Angular Frontend...
cd C:\Users\ACER\Desktop\fe-checkscam
start "Angular Frontend" cmd /c "ng serve --open"

echo.
echo =====================================
echo   ✅ CheckScam hoàn chỉnh đã khởi động!
echo =====================================
echo.
echo 🌐 URLs:
echo    Frontend: http://localhost:4200
echo    Backend API: http://localhost:8080
echo    Activity Feed: http://localhost:4200/activity-feed
echo.
echo 📡 Activity Feed APIs:
echo    GET http://localhost:8080/api/v1/activities
echo    POST http://localhost:8080/api/v1/activities  
echo    GET http://localhost:8080/api/v1/activities/statistics
echo    GET http://localhost:8080/api/v1/activities/dangerous
echo.
echo 🔧 Tính năng:
echo    ✅ Activity tracking tích hợp trong main backend
echo    ✅ REST API cho tất cả hoạt động
echo    ✅ WebSocket support (optional)
echo    ✅ Real-time statistics
echo    ✅ Dangerous activity detection
echo.
echo 📝 Test Activity Feed:
echo    1. Vào http://localhost:4200/activity-feed
echo    2. Click "Tạo hoạt động" để test
echo    3. Xem statistics updates real-time
echo    4. Test "Hoạt động nguy hiểm"
echo.
echo 🔍 Log activities từ code:
echo    Java: activityService.logScanActivity(userId, userName, website, riskLevel)
echo    Angular: this.activityService.logScanActivity(userId, userName, website, riskLevel)
echo.
echo 📋 Troubleshooting:
echo    - Nếu lỗi database: Kiểm tra MySQL đang chạy
echo    - Nếu lỗi CORS: Kiểm tra frontend port 4200
echo    - Nếu lỗi API: Check logs trong terminal "CheckScam Backend"
echo.
pause