@echo off
echo =====================================
echo     CheckScam Backend Startup
echo =====================================
echo.

echo [1/8] Kiểm tra Java và Maven...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java không tìm thấy. Vui lòng cài đặt Java JDK 11+
    echo Download: https://adoptium.net/
    pause
    exit /b 1
)

mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven không tìm thấy. Vui lòng cài đặt Maven
    echo Download: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo ✅ Java và Maven OK

echo.
echo [2/8] Kiểm tra MySQL service...
net start mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: MySQL service chưa chạy hoặc chưa cài đặt
    echo Đang thử khởi động MySQL...
    sc start MySQL80 >nul 2>&1
)

echo.
echo [3/8] Kiểm tra Node.js...
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Node.js không tìm thấy. Vui lòng cài đặt Node.js 16+
    pause
    exit /b 1
)
echo ✅ Node.js OK

echo.
echo [4/8] Cài đặt dependencies cho Activity Feed...
cd C:\Users\ACER\Desktop\checkscam\checkscamv2\activity-feed
call npm install
if %errorlevel% neq 0 (
    echo ERROR: Lỗi cài đặt Node.js dependencies
    pause
    exit /b 1
)

echo.
echo [5/8] Setup database cho Activity Feed...
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS activity_feed CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
mysql -u root -p activity_feed < database.sql 2>nul
echo ✅ Activity Feed database ready

echo.
echo [6/8] Khởi động Activity Feed Backend...
start "Activity Feed Backend" cmd /c "cd C:\Users\ACER\Desktop\checkscam\checkscamv2\activity-feed && npm run dev"

echo.
echo [7/8] Đợi Activity Feed khởi động (10 giây)...
timeout /t 10 /nobreak

echo.
echo [8/8] Khởi động Main Backend (Spring Boot)...
cd C:\Users\ACER\Desktop\checkscam\checkscamv2\checkscamv2
start "CheckScam Main Backend" cmd /c "mvn spring-boot:run"

echo.
echo =====================================
echo   ✅ CheckScam Backend Started!
echo =====================================
echo.
echo 🌐 Main Backend: http://localhost:8080
echo 📡 Activity Feed: http://localhost:3001
echo 📊 Health Check: http://localhost:3001/health
echo 📋 API Docs: http://localhost:8080/swagger-ui.html
echo.
echo 📝 Logs:
echo    - Activity Feed: Terminal "Activity Feed Backend"
echo    - Main Backend: Terminal "CheckScam Main Backend"
echo.
echo ⏰ Chờ 2-3 phút để backends khởi động hoàn toàn
echo.
pause