@echo off
echo ========================================
echo COMPREHENSIVE FIX: CORS + SERVICE 503
echo ========================================

echo.
echo 1. Building new image with CORS fix...
cd /d "C:\Users\ACER\Desktop\checkscam\checkscamv2\checkscamv2"
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Maven build failed
    pause
    exit /b 1
)

docker build -t ai6-be:latest .
if %errorlevel% neq 0 (
    echo ERROR: Docker build failed
    pause
    exit /b 1
)

echo.
echo 2. Applying updated configurations...
cd /d "C:\Users\ACER\Desktop\checkscam\checkscamv2\deploy"
kubectl apply -f backend\secret.yml
kubectl apply -f backend\configmap.yml
kubectl apply -f backend\service.yml
kubectl apply -f backend\deployment.yml
kubectl apply -f ingress.yml

echo.
echo 3. Forcing restart with new image...
kubectl rollout restart deployment ai6-be-deployment

echo.
echo 4. Waiting for rollout to complete...
kubectl rollout status deployment ai6-be-deployment --timeout=300s

echo.
echo 5. Checking service endpoints...
kubectl get endpoints ai6-be
kubectl get pods -l app=ai6-be -o wide

echo.
echo 6. Testing CORS and connectivity...
timeout /t 15 /nobreak >nul
echo Testing health endpoint...
kubectl exec deployment/ai6-be-deployment -- curl -I http://localhost:8080/actuator/health

echo.
echo Testing CORS headers...
kubectl exec deployment/ai6-be-deployment -- curl -I -X OPTIONS http://localhost:8080/api/v1/activities

echo.
echo ========================================
echo CORS + Service fix completed!
echo Frontend should now work properly.
echo ========================================
pause