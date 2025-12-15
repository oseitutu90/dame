# Dame - Kubernetes Deployment Guide

This guide covers deploying the Ghanaian Checkers (Dame) application to a Kubernetes cluster using ArgoCD and GitHub Actions.

## Architecture Overview

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   GitHub Repo   │──────│ GitHub Actions  │──────│     GHCR        │
│                 │      │   (CI/CD)       │      │ (Container Reg) │
└─────────────────┘      └─────────────────┘      └────────┬────────┘
                                                           │
                                                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Kubernetes Cluster                           │
│  ┌─────────────┐                                                    │
│  │   ArgoCD    │◄── Watches repo for changes                        │
│  └──────┬──────┘                                                    │
│         │                                                           │
│         ▼                                                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    dame namespace                            │   │
│  │  ┌──────────┐    ┌──────────┐    ┌──────────┐              │   │
│  │  │ Ingress  │────│ Service  │────│Deployment│              │   │
│  │  │(nginx)   │    │          │    │  (dame)  │              │   │
│  │  └──────────┘    └──────────┘    └──────────┘              │   │
│  │                                        │                    │   │
│  │                                        ▼                    │   │
│  │                               ┌──────────────┐              │   │
│  │                               │  PostgreSQL  │              │   │
│  │                               │  (existing)  │              │   │
│  │                               └──────────────┘              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## Prerequisites

- Kubernetes cluster (running on your Ubuntu server)
- kubectl configured to access your cluster
- Helm (for installing ingress controller and ArgoCD)
- PostgreSQL database accessible from the cluster
- GitHub account with a repository for this project

## Step 1: Install Nginx Ingress Controller

```bash
# Add the ingress-nginx repository
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

# Install nginx ingress controller
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.type=NodePort \
  --set controller.service.nodePorts.http=30080 \
  --set controller.service.nodePorts.https=30443
```

## Step 2: Install ArgoCD

```bash
# Create argocd namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD to be ready
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd

# Get the initial admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
echo

# Port forward to access ArgoCD UI (or set up ingress)
kubectl port-forward svc/argocd-server -n argocd 8443:443
```

Access ArgoCD at: https://localhost:8443 (username: admin)

## Step 3: Configure GitHub Repository

### 3.1 Push code to GitHub

```bash
# Initialize git if not already done
git init

# Add remote (replace OWNER with your GitHub username)
git remote add origin https://github.com/OWNER/dame.git

# Push code
git add .
git commit -m "Initial commit with K8s deployment config"
git push -u origin main
```

### 3.2 Enable GitHub Container Registry

1. Go to your GitHub repository Settings > Actions > General
2. Under "Workflow permissions", select "Read and write permissions"
3. Check "Allow GitHub Actions to create and approve pull requests"
4. Save changes

## Step 4: Create Image Pull Secret

GitHub Container Registry requires authentication. Create a secret in your cluster:

```bash
# Create a Personal Access Token (PAT) at https://github.com/settings/tokens
# Required scopes: read:packages

kubectl create namespace dame

kubectl create secret docker-registry ghcr-secret \
  --namespace dame \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_PAT \
  --docker-email=YOUR_EMAIL
```

## Step 5: Configure Database Connection

### 5.1 Update ConfigMap

Edit `k8s/base/configmap.yaml` to match your PostgreSQL setup:

```yaml
data:
  DB_HOST: "your-postgresql-host"  # e.g., postgresql.database.svc.cluster.local
  DB_NAME: "dame"
  DB_USER: "dame"
```

### 5.2 Update Secret

Edit `k8s/base/secret.yaml` with your actual database password:

```yaml
stringData:
  DB_PASSWORD: "your-actual-password"
```

**Important**: For production, consider using:
- [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets)
- [External Secrets Operator](https://external-secrets.io/)
- Kubernetes native secrets with proper RBAC

## Step 6: Update Manifests

### 6.1 Update Deployment Image

Edit `k8s/base/deployment.yaml` and replace `OWNER` with your GitHub username:

```yaml
image: ghcr.io/YOUR_USERNAME/dame:latest
```

### 6.2 Update ArgoCD Application

Edit `k8s/argocd/application.yaml` and replace `OWNER`:

```yaml
repoURL: https://github.com/YOUR_USERNAME/dame.git
```

## Step 7: Configure Local DNS

Add an entry to your local machine's hosts file to access the app:

### Linux/Mac
```bash
echo "YOUR_NODE_IP dame.local" | sudo tee -a /etc/hosts
```

### Windows (run as Administrator)
```powershell
Add-Content -Path C:\Windows\System32\drivers\etc\hosts -Value "YOUR_NODE_IP dame.local"
```

Replace `YOUR_NODE_IP` with your Kubernetes node's IP address.

## Step 8: Deploy with ArgoCD

### Option A: Via ArgoCD CLI

```bash
# Install ArgoCD CLI
# Mac: brew install argocd
# Linux: curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64

# Login to ArgoCD
argocd login localhost:8443 --username admin --password <your-password> --insecure

# Create the application
argocd app create dame \
  --repo https://github.com/YOUR_USERNAME/dame.git \
  --path k8s/base \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace dame \
  --sync-policy automated \
  --auto-prune \
  --self-heal
```

### Option B: Via kubectl

```bash
# Apply the ArgoCD Application manifest
kubectl apply -f k8s/argocd/application.yaml
```

### Option C: Via ArgoCD UI

1. Open ArgoCD UI at https://localhost:8443
2. Click "+ New App"
3. Fill in:
   - Application Name: `dame`
   - Project: `default`
   - Sync Policy: `Automatic`
   - Repository URL: `https://github.com/YOUR_USERNAME/dame.git`
   - Path: `k8s/base`
   - Cluster: `https://kubernetes.default.svc`
   - Namespace: `dame`
4. Click "Create"

## Step 9: Verify Deployment

```bash
# Check ArgoCD application status
argocd app get dame

# Or via kubectl
kubectl get application dame -n argocd

# Check pods are running
kubectl get pods -n dame

# Check the service
kubectl get svc -n dame

# Check ingress
kubectl get ingress -n dame

# View logs
kubectl logs -f deployment/dame -n dame
```

## Step 10: Access the Application

- **Via Ingress**: http://dame.local:30080
- **Via Port Forward**:
  ```bash
  kubectl port-forward svc/dame -n dame 8080:80
  # Access at http://localhost:8080
  ```

## CI/CD Pipeline Flow

1. **Developer pushes to main branch**
2. **GitHub Actions CI workflow**:
   - Runs tests
   - Builds production JAR
3. **GitHub Actions Deploy workflow**:
   - Builds Docker image
   - Pushes to GitHub Container Registry (ghcr.io)
   - Updates image tag in `k8s/base/deployment.yaml`
   - Commits and pushes the change
4. **ArgoCD detects the change**:
   - Syncs the new deployment
   - Rolls out the new container

## Troubleshooting

### Pod won't start

```bash
# Check pod events
kubectl describe pod -l app.kubernetes.io/name=dame -n dame

# Check logs
kubectl logs -l app.kubernetes.io/name=dame -n dame --previous
```

### Database connection issues

```bash
# Test database connectivity from a pod
kubectl run pg-test --rm -it --image=postgres:15 --restart=Never -n dame -- \
  psql "postgresql://dame:password@postgresql-host:5432/dame" -c "SELECT 1"
```

### Image pull errors

```bash
# Verify secret exists
kubectl get secret ghcr-secret -n dame

# Test pulling image manually
docker pull ghcr.io/YOUR_USERNAME/dame:latest
```

### ArgoCD sync issues

```bash
# Check ArgoCD logs
kubectl logs -f deployment/argocd-application-controller -n argocd

# Force sync
argocd app sync dame --force
```

## Production Considerations

1. **TLS/HTTPS**: Add cert-manager for automatic TLS certificates
2. **Secrets Management**: Use sealed-secrets or external-secrets
3. **Resource Tuning**: Adjust CPU/memory based on actual usage
4. **Horizontal Pod Autoscaler**: Add HPA for automatic scaling
5. **Database Backups**: Ensure PostgreSQL has regular backups
6. **Monitoring**: Add Prometheus/Grafana for observability
7. **Logging**: Consider Loki or ELK stack for centralized logging

## File Structure

```
dame/
├── .github/
│   └── workflows/
│       ├── ci.yaml          # PR testing
│       └── deploy.yaml      # Build & push image
├── k8s/
│   ├── argocd/
│   │   └── application.yaml # ArgoCD app definition
│   └── base/
│       ├── configmap.yaml   # Non-sensitive config
│       ├── deployment.yaml  # App deployment
│       ├── ingress.yaml     # Nginx ingress rules
│       ├── kustomization.yaml
│       ├── namespace.yaml   # Namespace definition
│       ├── secret.yaml      # Database credentials
│       └── service.yaml     # ClusterIP service
├── Dockerfile               # Multi-stage Java 21 build
└── DEPLOYMENT.md           # This file
```
