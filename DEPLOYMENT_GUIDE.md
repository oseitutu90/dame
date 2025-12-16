# Dame Application - Complete Kubernetes Deployment Guide

This is a comprehensive, step-by-step guide to deploying the Dame (Ghanaian Checkers) Spring Boot/Vaadin application to a Kubernetes cluster using ArgoCD and GitHub Actions.

---

## Table of Contents

1. [Application Overview](#1-application-overview)
2. [Prerequisites](#2-prerequisites)
3. [Preparing the Application for Containerization](#3-preparing-the-application-for-containerization)
4. [Creating the Dockerfile](#4-creating-the-dockerfile)
5. [Kubernetes Manifests](#5-kubernetes-manifests)
6. [GitHub Actions CI/CD Pipeline](#6-github-actions-cicd-pipeline)
7. [ArgoCD GitOps Setup](#7-argocd-gitops-setup)
8. [Database Setup](#8-database-setup)
9. [Container Registry Authentication](#9-container-registry-authentication)
10. [Deployment and Verification](#10-deployment-and-verification)

---

## 1. Application Overview

### Technology Stack
| Component | Technology |
|-----------|------------|
| Backend | Spring Boot 3.2.1 |
| Frontend | Vaadin Flow 24.3.7 |
| Language | Java 21 |
| Build Tool | Maven |
| Database | PostgreSQL (production), H2 (development) |
| Authentication | Spring Security with BCrypt |

### Application Structure
```
dame/
├── src/main/java/com/dame/
│   ├── DameApplication.java          # Spring Boot entry point
│   ├── config/SecurityConfig.java    # Spring Security configuration
│   ├── engine/                       # Game logic (Board, GameLogic, etc.)
│   ├── entity/                       # JPA entities (Player, PlayerStats, etc.)
│   ├── repository/                   # Spring Data JPA repositories
│   ├── service/                      # Business logic services
│   └── ui/                           # Vaadin UI views
├── src/main/resources/
│   ├── application.yaml              # Development configuration
│   └── application-prod.yaml         # Production configuration
├── frontend/                         # Vaadin frontend assets
└── pom.xml                          # Maven configuration
```

---

## 2. Prerequisites

### On Your Local Machine
- Git
- GitHub account
- kubectl configured to access your cluster
- Helm (for installing components)

### On Your Kubernetes Cluster
- Kubernetes 1.25+ (we used v1.30.14)
- ArgoCD installed
- PostgreSQL database available
- Nginx Ingress Controller (optional, for hostname-based access)

### Cluster Details Used
```
Node: oseitutu (192.168.1.21)
OS: Ubuntu 24.04.1 LTS
Kubernetes: v1.30.14
Container Runtime: containerd 1.7.28
```

---

## 3. Preparing the Application for Containerization

### 3.1 Add Spring Boot Actuator for Health Checks

Kubernetes needs health endpoints to know if your application is healthy. Add the actuator dependency to `pom.xml`:

```xml
<!-- In pom.xml, add inside <dependencies> -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 3.2 Create Production Configuration

Create `src/main/resources/application-prod.yaml`:

```yaml
# Production Configuration
spring:
  datasource:
    # Environment variables for database connection
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:dame}
    username: ${DB_USER:dame}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      # 'update' creates tables automatically, 'validate' for strict production
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect

  h2:
    console:
      enabled: false

# Actuator endpoints for Kubernetes health probes
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true

# Logging
logging:
  level:
    com.dame: INFO
    org.springframework.security: WARN
```

### 3.3 Allow Actuator Endpoints in Security Config

Update `src/main/java/com/dame/config/SecurityConfig.java`:

```java
@Override
protected void configure(HttpSecurity http) throws Exception {
    // Allow actuator endpoints for Kubernetes health checks
    http.authorizeHttpRequests(auth -> auth
            .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
    );

    // ... rest of configuration
}
```

### 3.4 Create .gitignore

```gitignore
# Compiled class files
*.class

# Package files
*.jar
*.war

# Maven
target/

# IDE
.idea/
*.iml
.vscode/

# Node modules (Vaadin)
node_modules/
frontend/generated/

# OS files
.DS_Store

# Environment files
.env
.env.local
```

### 3.5 Create .dockerignore

```dockerignore
# Build artifacts
target/
!target/*.jar

# IDE
.idea/
*.iml
.vscode/

# Git
.git/
.gitignore

# Documentation
*.md

# Test files
src/test/

# Node modules
node_modules/
frontend/generated/

# Kubernetes manifests (not needed in image)
k8s/

# GitHub Actions
.github/
```

---

## 4. Creating the Dockerfile

Create `Dockerfile` in the project root:

```dockerfile
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (this layer will be cached)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src
COPY frontend ./frontend

# Build the application with production profile
# -Pproduction triggers Vaadin's production frontend build
RUN mvn clean package -Pproduction -DskipTests -B

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user for security
RUN groupadd -r dame && useradd -r -g dame dame

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R dame:dame /app

USER dame

# Expose the application port
EXPOSE 8080

# Health check using actuator endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with production profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

### Dockerfile Explanation

| Stage | Purpose |
|-------|---------|
| **Builder Stage** | Uses full JDK to compile Java code and build Vaadin frontend |
| **Runtime Stage** | Uses lightweight JRE only - smaller final image |
| **Multi-stage benefit** | Final image ~300MB instead of ~1GB |

---

## 5. Kubernetes Manifests

Create the directory structure:
```
k8s/
├── argocd/
│   └── application.yaml      # ArgoCD Application definition
└── base/
    ├── namespace.yaml        # Kubernetes namespace
    ├── configmap.yaml        # Non-sensitive configuration
    ├── secret.yaml           # Database credentials
    ├── deployment.yaml       # Application deployment
    ├── service.yaml          # Service exposure
    ├── ingress.yaml          # Ingress rules (optional)
    └── kustomization.yaml    # Kustomize configuration
```

### 5.1 Namespace (`k8s/base/namespace.yaml`)

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: dame
  labels:
    app.kubernetes.io/name: dame
    app.kubernetes.io/part-of: dame-app
```

**Purpose**: Isolates all Dame resources in their own namespace for organization and security.

### 5.2 ConfigMap (`k8s/base/configmap.yaml`)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: dame-config
  namespace: dame
  labels:
    app.kubernetes.io/name: dame
    app.kubernetes.io/component: config
data:
  # Database connection details (non-sensitive)
  DB_HOST: "postgres.impala.svc.cluster.local"
  DB_NAME: "dame"
  DB_USER: "dame_user"
  # JVM options for container environment
  JAVA_OPTS: "-Xms256m -Xmx512m"
```

**Purpose**: Stores non-sensitive configuration that can be changed without rebuilding the image.

**Key Points**:
- `DB_HOST` uses Kubernetes DNS: `<service>.<namespace>.svc.cluster.local`
- JVM memory settings prevent container OOM issues

### 5.3 Secret (`k8s/base/secret.yaml`)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: dame-secrets
  namespace: dame
  labels:
    app.kubernetes.io/name: dame
    app.kubernetes.io/component: secrets
type: Opaque
stringData:
  # Database password - CHANGE THIS!
  DB_PASSWORD: "dame_secure_pass_2024"
```

**Purpose**: Stores sensitive data like passwords.

**Security Note**: In production, use:
- [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) - Encrypts secrets for Git storage
- [External Secrets Operator](https://external-secrets.io/) - Syncs from external secret stores
- HashiCorp Vault

### 5.4 Deployment (`k8s/base/deployment.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dame
  namespace: dame
  labels:
    app.kubernetes.io/name: dame
    app.kubernetes.io/component: backend
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: dame
  template:
    metadata:
      labels:
        app.kubernetes.io/name: dame
        app.kubernetes.io/component: backend
    spec:
      containers:
        - name: dame
          # Image updated by GitHub Actions
          image: ghcr.io/oseitutu90/dame:latest
          imagePullPolicy: Always
          ports:
            - name: http
              containerPort: 8080
              protocol: TCP

          # Environment variables from ConfigMap and Secret
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: DB_HOST
              valueFrom:
                configMapKeyRef:
                  name: dame-config
                  key: DB_HOST
            - name: DB_NAME
              valueFrom:
                configMapKeyRef:
                  name: dame-config
                  key: DB_NAME
            - name: DB_USER
              valueFrom:
                configMapKeyRef:
                  name: dame-config
                  key: DB_USER
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: dame-secrets
                  key: DB_PASSWORD
            - name: JAVA_OPTS
              valueFrom:
                configMapKeyRef:
                  name: dame-config
                  key: JAVA_OPTS

          # Resource limits prevent runaway containers
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"

          # Liveness probe - restarts container if unhealthy
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            initialDelaySeconds: 90      # Wait for app startup
            periodSeconds: 30            # Check every 30s
            timeoutSeconds: 10
            failureThreshold: 3          # 3 failures = restart

          # Readiness probe - removes from service if not ready
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3

          # Startup probe - gives app time to start
          startupProbe:
            httpGet:
              path: /actuator/health
              port: http
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 30         # 30 * 10s = 5 minutes to start

      # Pull images from private GitHub Container Registry
      imagePullSecrets:
        - name: ghcr-secret
```

**Probe Explanation**:

| Probe | Purpose | Failure Action |
|-------|---------|----------------|
| **startupProbe** | Waits for slow-starting apps | Prevents other probes until success |
| **livenessProbe** | Detects deadlocked apps | Restarts container |
| **readinessProbe** | Detects temporary issues | Removes from load balancer |

### 5.5 Service (`k8s/base/service.yaml`)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: dame
  namespace: dame
  labels:
    app.kubernetes.io/name: dame
    app.kubernetes.io/component: backend
spec:
  type: NodePort              # Exposes on each node's IP
  ports:
    - port: 80                # Service port
      targetPort: 8080        # Container port
      nodePort: 30888         # External port (30000-32767)
      protocol: TCP
      name: http
  selector:
    app.kubernetes.io/name: dame
```

**Service Types Explained**:

| Type | Use Case | Access Method |
|------|----------|---------------|
| **ClusterIP** | Internal only | Only within cluster |
| **NodePort** | Development/Home lab | `<NodeIP>:<NodePort>` |
| **LoadBalancer** | Cloud providers | External IP assigned |

### 5.6 Ingress (`k8s/base/ingress.yaml`) - Optional

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: dame
  namespace: dame
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "300"
    # WebSocket support for Vaadin Push
    nginx.ingress.kubernetes.io/proxy-http-version: "1.1"
spec:
  ingressClassName: nginx
  defaultBackend:
    service:
      name: dame
      port:
        number: 80
  rules:
    - host: dame.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: dame
                port:
                  number: 80
    - http:                    # Catch-all for any hostname
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: dame
                port:
                  number: 80
```

### 5.7 Kustomization (`k8s/base/kustomization.yaml`)

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: dame

resources:
  - namespace.yaml
  - configmap.yaml
  - secret.yaml
  - deployment.yaml
  - service.yaml
  - ingress.yaml

commonLabels:
  app.kubernetes.io/managed-by: argocd
  app.kubernetes.io/part-of: dame-app
```

**Purpose**: Kustomize bundles all manifests and applies common settings.

---

## 6. GitHub Actions CI/CD Pipeline

### 6.1 CI Workflow (`.github/workflows/ci.yaml`)

Runs tests on every pull request:

```yaml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]
    paths-ignore:
      - 'k8s/**'
      - '*.md'

jobs:
  test:
    name: Build and Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        run: mvn test -B

      - name: Build application
        run: mvn package -Pproduction -DskipTests -B

      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: target/surefire-reports/
```

### 6.2 Deploy Workflow (`.github/workflows/deploy.yaml`)

Builds Docker image and updates Kubernetes manifests:

```yaml
name: Build and Deploy

on:
  push:
    branches: [main]
    paths-ignore:
      - 'k8s/**'
      - '*.md'
  workflow_dispatch:          # Manual trigger

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-push:
    name: Build and Push Docker Image
    runs-on: ubuntu-latest
    permissions:
      contents: write
      packages: write

    outputs:
      short_sha: ${{ steps.vars.outputs.short_sha }}

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        run: mvn test -B

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Get short SHA
        id: vars
        run: echo "short_sha=$(git rev-parse --short HEAD)" >> $GITHUB_OUTPUT

      - name: Extract metadata for Docker
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,prefix=
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  update-manifests:
    name: Update Kubernetes Manifests
    runs-on: ubuntu-latest
    needs: build-and-push
    permissions:
      contents: write

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Update image tag in deployment
        run: |
          SHORT_SHA=${{ needs.build-and-push.outputs.short_sha }}
          IMAGE="${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${SHORT_SHA}"

          # Update deployment.yaml with new image tag
          sed -i "s|image: ghcr.io/.*|image: ${IMAGE}|" k8s/base/deployment.yaml

      - name: Commit and push changes
        run: |
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git config user.name "github-actions[bot]"

          git add k8s/base/deployment.yaml

          if ! git diff --staged --quiet; then
            git commit -m "chore: update image to ${{ needs.build-and-push.outputs.short_sha }}"
            git push
          fi
```

### CI/CD Flow Diagram

```
┌─────────────────┐
│  Developer      │
│  pushes code    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│  GitHub Actions │────▶│  Run Tests      │
│  Triggered      │     │  (mvn test)     │
└────────┬────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│  Build Docker   │────▶│  Push to GHCR   │
│  Image          │     │  ghcr.io/...    │
└────────┬────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│  Update k8s/    │────▶│  Git Commit     │
│  deployment.yaml│     │  New image tag  │
└────────┬────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│  ArgoCD detects │────▶│  Syncs new      │
│  Git change     │     │  deployment     │
└─────────────────┘     └─────────────────┘
```

---

## 7. ArgoCD GitOps Setup

### 7.1 Install ArgoCD (if not already installed)

```bash
# Create namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for deployment
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd

# Get initial admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

### 7.2 ArgoCD Application Manifest (`k8s/argocd/application.yaml`)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: dame
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default

  source:
    repoURL: https://github.com/oseitutu90/dame.git
    targetRevision: main
    path: k8s/base

  destination:
    server: https://kubernetes.default.svc
    namespace: dame

  syncPolicy:
    automated:
      prune: true             # Delete resources removed from Git
      selfHeal: true          # Revert manual changes
    syncOptions:
      - CreateNamespace=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

### 7.3 Deploy the ArgoCD Application

```bash
kubectl apply -f k8s/argocd/application.yaml
```

### ArgoCD Sync Behavior

| Setting | Effect |
|---------|--------|
| `automated` | Auto-syncs when Git changes |
| `prune: true` | Deletes resources removed from Git |
| `selfHeal: true` | Reverts any manual kubectl changes |

---

## 8. Database Setup

### 8.1 Create Database and User in Existing PostgreSQL

```bash
# Connect to PostgreSQL pod
kubectl exec -it postgres-0 -n impala -- psql -U impala_user -d impala_db

# Create database
CREATE DATABASE dame;

# Create user
CREATE USER dame_user WITH PASSWORD 'dame_secure_pass_2024';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE dame TO dame_user;

# Connect to dame database and grant schema privileges
\c dame
GRANT ALL ON SCHEMA public TO dame_user;
ALTER DATABASE dame OWNER TO dame_user;

# Exit
\q
```

### 8.2 Verify Database Connection

```bash
# Test from a temporary pod
kubectl run pg-test --rm -it --image=postgres:15 --restart=Never -n dame -- \
  psql "postgresql://dame_user:dame_secure_pass_2024@postgres.impala.svc.cluster.local:5432/dame" \
  -c "SELECT 1"
```

---

## 9. Container Registry Authentication

### 9.1 Create GitHub Personal Access Token

1. Go to https://github.com/settings/tokens
2. Generate new token (classic)
3. Select scopes: `read:packages`, `write:packages`
4. Copy the token

### 9.2 Create Kubernetes Secret

```bash
# Create namespace first
kubectl create namespace dame

# Create docker-registry secret
kubectl create secret docker-registry ghcr-secret \
  --namespace dame \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_PAT \
  --docker-email=your-email@example.com
```

---

## 10. Deployment and Verification

### 10.1 Push Code to GitHub

```bash
# Add remote
git remote add origin https://github.com/oseitutu90/dame.git

# Push code
git add .
git commit -m "Initial deployment setup"
git push -u origin main
```

### 10.2 Verify GitHub Actions

```bash
# Check workflow runs
gh run list --repo oseitutu90/dame

# Watch a specific run
gh run watch <run-id>
```

### 10.3 Verify ArgoCD Sync

```bash
# Check application status
kubectl get application dame -n argocd

# Expected output:
# NAME   SYNC STATUS   HEALTH STATUS
# dame   Synced        Healthy
```

### 10.4 Verify Kubernetes Resources

```bash
# Check all resources in dame namespace
kubectl get all -n dame

# Expected output:
# NAME                        READY   STATUS    RESTARTS   AGE
# pod/dame-776dc69665-qltrv   1/1     Running   0          5m
#
# NAME           TYPE       CLUSTER-IP    EXTERNAL-IP   PORT(S)        AGE
# service/dame   NodePort   10.111.62.8   <none>        80:30888/TCP   5m
#
# NAME                   READY   UP-TO-DATE   AVAILABLE   AGE
# deployment.apps/dame   1/1     1            1           5m
```

### 10.5 Check Application Logs

```bash
kubectl logs -f deployment/dame -n dame
```

### 10.6 Access the Application

**Via NodePort**:
```
http://192.168.1.21:30888
```

**Via Port Forward** (for testing):
```bash
kubectl port-forward svc/dame -n dame 8080:80
# Access at http://localhost:8080
```

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              DEVELOPER                                   │
│                                  │                                       │
│                            git push                                      │
│                                  ▼                                       │
├─────────────────────────────────────────────────────────────────────────┤
│                           GITHUB                                         │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐     │
│  │   Repository    │───▶│ GitHub Actions  │───▶│      GHCR       │     │
│  │   (source)      │    │   (CI/CD)       │    │ (Docker images) │     │
│  └─────────────────┘    └────────┬────────┘    └─────────────────┘     │
│                                  │ updates k8s/                          │
│                                  ▼                                       │
├─────────────────────────────────────────────────────────────────────────┤
│                        KUBERNETES CLUSTER                                │
│                                                                          │
│  ┌─────────────────┐         watches git                                │
│  │     ArgoCD      │◀────────────────────                               │
│  │   (GitOps)      │                                                     │
│  └────────┬────────┘                                                     │
│           │ syncs                                                        │
│           ▼                                                              │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                      dame namespace                                │  │
│  │                                                                    │  │
│  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │  │
│  │  │  ConfigMap  │    │   Secret    │    │   Ingress   │           │  │
│  │  │  (DB config)│    │ (DB pass)   │    │ (optional)  │           │  │
│  │  └──────┬──────┘    └──────┬──────┘    └─────────────┘           │  │
│  │         │                  │                                       │  │
│  │         └────────┬─────────┘                                       │  │
│  │                  ▼                                                  │  │
│  │         ┌─────────────┐         ┌─────────────┐                   │  │
│  │         │ Deployment  │────────▶│   Service   │──▶ NodePort:30888 │  │
│  │         │   (Pod)     │         │ (NodePort)  │                   │  │
│  │         └──────┬──────┘         └─────────────┘                   │  │
│  │                │                                                   │  │
│  └────────────────┼───────────────────────────────────────────────────┘  │
│                   │                                                       │
│                   ▼                                                       │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                     impala namespace                               │  │
│  │  ┌─────────────┐                                                   │  │
│  │  │ PostgreSQL  │◀─── jdbc connection                               │  │
│  │  │  (postgres) │                                                   │  │
│  │  └─────────────┘                                                   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                          ┌─────────────────┐
                          │      USER       │
                          │ http://IP:30888 │
                          └─────────────────┘
```

---

## Troubleshooting Commands

```bash
# Pod not starting
kubectl describe pod -l app.kubernetes.io/name=dame -n dame
kubectl logs -l app.kubernetes.io/name=dame -n dame --previous

# Check events
kubectl get events -n dame --sort-by='.lastTimestamp'

# Check ArgoCD sync
kubectl get application dame -n argocd -o yaml

# Force ArgoCD sync
kubectl patch application dame -n argocd --type merge -p '{"operation":{"initiatedBy":{"username":"admin"},"sync":{}}}'

# Restart deployment
kubectl rollout restart deployment/dame -n dame
```

---

## Quick Reference

| Resource | Location |
|----------|----------|
| **Application URL** | http://192.168.1.21:30888 |
| **GitHub Repo** | https://github.com/oseitutu90/dame |
| **Container Registry** | ghcr.io/oseitutu90/dame |
| **ArgoCD UI** | https://192.168.1.21:32745 |
| **Kubernetes Namespace** | dame |
| **Database** | postgres.impala.svc.cluster.local:5432/dame |

---

*This guide was created during the deployment of Dame (Ghanaian Checkers) application on December 15, 2025.*
