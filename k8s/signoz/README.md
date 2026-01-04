# SigNoz Installation Guide

## Prerequisites

- Kubernetes cluster with kubectl access
- Helm 3+
- StorageClass available (`kubectl get storageclass`)
- Minimum 8GB RAM / 4 cores / 30GB storage

## Installation

### 1. Add Helm Repository

```bash
helm repo add signoz https://charts.signoz.io
helm repo update
```

### 2. Create Namespace

```bash
kubectl create namespace platform
```

### 3. Update values.yaml

Edit `values.yaml` and set your StorageClass:

```yaml
global:
  storageClass: "your-storage-class-name"
```

### 4. Install SigNoz

```bash
helm install signoz signoz/signoz -n platform -f values.yaml
```

### 5. Wait for Pods

```bash
kubectl get pods -n platform -w
```

### 6. Access UI (Port Forward)

```bash
kubectl port-forward -n platform svc/signoz-frontend 3301:3301
```

Open: <http://localhost:3301>

## Verify Dame Integration

1. Deploy the Dame application
2. Navigate the app to generate traces
3. Open SigNoz → Traces → Filter by `service.name = dame`
