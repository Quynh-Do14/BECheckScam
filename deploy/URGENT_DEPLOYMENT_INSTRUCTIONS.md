# URGENT: CORS + 502 ERROR FIX

## Issue Description:
- Frontend getting 502 Bad Gateway errors
- CORS policy blocking all API calls
- Service `ai6-be` has no available endpoints

## Solution Applied:
✅ Fixed CORS configuration in Spring Boot
✅ Added proper health check endpoints  
✅ Built new Docker image: `ai6-be:latest`
✅ Updated all Kubernetes configs

## Files to Deploy:

### 1. Backend Configs (PRIORITY):
```bash
kubectl apply -f backend/secret.yml
kubectl apply -f backend/configmap.yml
kubectl apply -f backend/service.yml  
kubectl apply -f backend/deployment.yml
```

### 2. Ingress (if needed):
```bash
kubectl apply -f ingress.yml
```

### 3. Restart deployment:
```bash
kubectl rollout restart deployment ai6-be-deployment
kubectl rollout status deployment ai6-be-deployment
```

## Expected Results:
✅ Service `ai6-be` will have healthy endpoints
✅ CORS headers will be returned: `Access-Control-Allow-Origin: https://ai6.vn`
✅ Frontend API calls will work
✅ 502 errors will be resolved

## Verification Commands:
```bash
# Check service endpoints
kubectl get endpoints ai6-be

# Check pod health
kubectl get pods -l app=ai6-be

# Check deployment status  
kubectl rollout status deployment ai6-be-deployment

# Test API directly
kubectl exec deployment/ai6-be-deployment -- curl -I http://localhost:8080/actuator/health
```

## Docker Image Details:
- **Image**: `ai6-be:latest`
- **Contains**: CORS fix + health endpoints
- **Port**: 8080
- **Health Check**: `/actuator/health`

## Emergency Contact:
If issues persist, check backend logs:
```bash
kubectl logs deployment/ai6-be-deployment --tail=50
```

---
**Time-sensitive: Frontend is currently broken due to backend deployment issues.**