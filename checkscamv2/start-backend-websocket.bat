@echo off
echo Starting CheckScam Backend with WebSocket support...
cd /d C:\Users\ACER\Desktop\checkscam\checkscamv2\checkscamv2

echo Cleaning previous build...
call mvn clean

echo Building application...
call mvn compile

echo Starting Spring Boot application...
call mvn spring-boot:run

pause
