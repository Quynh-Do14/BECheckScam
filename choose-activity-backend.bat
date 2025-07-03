@echo off
echo =====================================
echo   CheckScam Activity Feed Options
echo =====================================
echo.
echo Chọn backend cho Activity Feed:
echo [1] Java Spring Boot (tích hợp vào main backend)
echo [2] Node.js riêng biệt (Socket.io)
echo [3] Cả hai (demo đầy đủ)
echo.
set /p choice="Nhập lựa chọn (1/2/3): "

if "%choice%"=="1" goto java_backend
if "%choice%"=="2" goto node_backend  
if "%choice%"=="3" goto both_backends
goto invalid_choice

:java_backend
echo.
echo =====================================
echo   Chạy với Java Spring Boot Backend
echo =====================================
echo.

echo [1/4] Setup database cho activities...
cd C:\Users\ACER\Desktop\checkscam\checkscamv2
mysql -u root -p checkscam < activity-tables.sql

echo [2/4] Cập nhật environment...
echo Sửa useMainBackendForActivity = true trong environment.ts

echo [3/4] Khởi động Main Backend (với Activity)...
start "CheckScam Main Backend" cmd /c "mvn spring-boot:run"

echo [4/4] Khởi động Frontend...
timeout /t 20 /nobreak
cd C:\Users\ACER\Desktop\fe-checkscam
start "Angular Frontend" cmd /c "ng serve --open"

echo.
echo ✅ Activity Feed chạy trên Main Backend
echo 🌐 Frontend: http://localhost:4200
echo 📡 Backend: http://localhost:8080
echo 🔄 Activities API: http://localhost:8080/api/v1/activities
goto end

:node_backend
echo.
echo =====================================
echo   Chạy với Node.js Backend riêng biệt  
echo =====================================
echo.

echo [1/4] Setup database cho Node.js...
cd C:\Users\ACER\Desktop\checkscam\checkscamv2\activity-feed
mysql -u root -p < database.sql

echo [2/4] Cập nhật environment...
echo Sửa useMainBackendForActivity = false trong environment.ts

echo [3/4] Khởi động Activity Feed Server...
start "Activity Feed Server" cmd /c "npm run dev"

echo [4/4] Khởi động Frontend...
timeout /t 15 /nobreak
cd C:\Users\ACER\Desktop\fe-checkscam
start "Angular Frontend" cmd /c "ng serve --open"

echo.
echo ✅ Activity Feed chạy trên Node.js riêng biệt
echo 🌐 Frontend: http://localhost:4200  
echo 📡 Activity Feed: http://localhost:3001
echo 🔄 Socket.io WebSocket: Enabled
goto end

:both_backends  
echo.
echo =====================================
echo   Chạy cả hai backends (Demo đầy đủ)
echo =====================================
echo.

echo [1/6] Setup databases...
cd C:\Users\ACER\Desktop\checkscam\checkscamv2
mysql -u root -p checkscam < activity-tables.sql
cd C:\Users\ACER\Desktop\checkscam\checkscamv2\activity-feed
mysql -u root -p < database.sql

echo [2/6] Khởi động Main Backend...
cd C:\Users\ACER\Desktop\checkscam\checkscamv2
start "CheckScam Main Backend" cmd /c "mvn spring-boot:run"

echo [3/6] Khởi động Activity Feed Server...
cd C:\Users\ACER\Desktop\checkscam\checkscamv2\activity-feed
start "Activity Feed Server" cmd /c "npm run dev"

echo [4/6] Đợi backends khởi động...
timeout /t 25 /nobreak

echo [5/6] Khởi động Frontend...
cd C:\Users\ACER\Desktop\fe-checkscam
start "Angular Frontend" cmd /c "ng serve --open"

echo [6/6] Mở demo pages...
timeout /t 10 /nobreak
start http://localhost:4200/activity-feed
start http://localhost:3001

echo.
echo ✅ Demo đầy đủ - Cả hai backends
echo 🌐 Frontend: http://localhost:4200
echo 📡 Main Backend: http://localhost:8080  
echo 📡 Activity Feed: http://localhost:3001
echo 🔄 Có thể switch giữa 2 backends trong environment.ts
goto end

:invalid_choice
echo Lựa chọn không hợp lệ. Vui lòng chọn 1, 2 hoặc 3.
pause
goto end

:end
echo.
echo =====================================
echo   Hướng dẫn sử dụng
echo =====================================
echo.
echo 🔧 Để chuyển đổi backend:
echo    Sửa useMainBackendForActivity trong environment.ts
echo    true = Java backend, false = Node.js backend
echo.
echo 📝 Log activities từ code:
echo    Java: activityService.logScanActivity(...)
echo    Angular: this.activityService.logScanActivity(...)
echo.
echo 🎯 Test Activity Feed:
echo    Vào http://localhost:4200/activity-feed
echo    Tạo hoạt động mẫu và xem real-time updates
echo.
pause