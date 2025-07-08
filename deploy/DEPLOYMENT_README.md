# AI6-BE Deployment Package

## Quick Deploy Instructions

### 1. Import Docker Image (if using tar file)
```bash
docker load -i ai6-be-latest.tar
```

### 2. Apply Kubernetes Configurations
```bash
# Apply in this order:
kubectl apply -f backend/secret.yml
kubectl apply -f backend/configmap.yml  
kubectl apply -f backend/service.yml
kubectl apply -f backend/deployment.yml
kubectl apply -f ingress.yml

# Restart deployment with new image
kubectl rollout restart deployment ai6-be-deployment
kubectl rollout status deployment ai6-be-deployment
```

### 3. Verify Deployment
```bash
# Check service endpoints
kubectl get endpoints ai6-be

# Check pods
kubectl get pods -l app=ai6-be

# Check logs
kubectl logs deployment/ai6-be-deployment --tail=20
```

## What's Fixed:
- ✅ CORS configuration enabled
- ✅ Service name `ai6-be` (matches error message)
- ✅ Health check endpoints (/actuator/health)
- ✅ Proper resource limits
- ✅ Startup/liveness/readiness probes

## Expected Results:
- ✅ Service `ai6-be` will have endpoints
- ✅ Frontend CORS errors will be resolved
- ✅ API calls from ai6.vn to api-v1.ai6.vn will work
- ✅ 503 "no endpoints available" error will be fixed

## Troubleshooting:
If pods are not ready:
```bash
kubectl describe pods -l app=ai6-be
kubectl logs deployment/ai6-be-deployment
```

## Image Details:
- **Image**: ai6-be:latest
- **Port**: 8080
- **Health Check**: /actuator/health
- **CORS**: Enabled for ai6.vn, www.ai6.vn
