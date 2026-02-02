# Dame CI/CD Pipeline Guide

## Overview

This document describes the GitOps CI/CD pipeline for the Dame (Ghanaian Checkers) application.

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Developer  │─────▶│   Jenkins    │─────▶│    GHCR      │─────▶│   Image      │
│   git push   │      │   Builds     │      │  dame:sha    │      │   Updater    │
└──────────────┘      └──────────────┘      └──────────────┘      └──────┬───────┘
                                                                         │
                      ┌──────────────┐      ┌──────────────┐             │
                      │  Kubernetes  │◀─────│   Argo CD    │◀────────────┘
                      │  New Pods    │      │   Syncs      │
                      └──────────────┘      └──────────────┘
```

---

## Pipeline Components

| Component | Namespace | Purpose |
|-----------|-----------|---------|
| **Jenkins** | `jenkins` | CI: Build, test, push images |
| **Argo CD** | `argocd` | CD: GitOps deployment controller |
| **Image Updater** | `argocd-image-updater-system` | Detects new images, commits tag updates |
| **Dame App** | `dame` | The application itself |

---

## Pipeline Flow

### 1. Developer Push

```bash
git push origin main
```

### 2. Jenkins Build

Jenkins runs in a Kubernetes pod with Maven container:

```groovy
// Jenkinsfile stages
stage('Checkout')     → Clone repository
stage('Test')         → mvn -B test
stage('Build & Push') → Jib builds and pushes to GHCR
```

**Key configuration:**

- **Agent:** Kubernetes pod with `maven:3.9.6-eclipse-temurin-21`
- **Image builder:** Jib (no Docker daemon required)
- **Registry:** `ghcr.io/oseitutu90/dame`
- **Tag:** Git commit SHA (e.g., `3c17b44d`)

### 3. Image Push to GHCR

Jib pushes directly to GitHub Container Registry:

```
ghcr.io/oseitutu90/dame:3c17b44d
```

### 4. Argo CD Image Updater

Polls GHCR every 2 minutes. When new image detected:

1. Updates `k8s/overlays/dev/kustomization.yaml`
2. Commits: `"chore: update image to <sha>"`
3. Pushes to GitHub

### 5. Argo CD Sync

Detects Git change and syncs to cluster:

1. Renders Kustomize manifests
2. Applies to Kubernetes
3. Rolling update deploys new pods

---

## Key Files

### Jenkinsfile

```groovy
pipeline {
  agent {
    kubernetes {
      yaml '''
        spec:
          containers:
          - name: maven
            image: maven:3.9.6-eclipse-temurin-21
            command: [sleep]
            args: [infinity]
      '''
      defaultContainer 'maven'
    }
  }
  stages {
    stage('Test') { steps { sh 'mvn -B test' } }
    stage('Build & Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'ghcr-creds', ...)]) {
          sh 'mvn -B -Pproduction com.google.cloud.tools:jib-maven-plugin:build ...'
        }
      }
    }
  }
}
```

### pom.xml (Jib Configuration)

```xml
<plugin>
  <groupId>com.google.cloud.tools</groupId>
  <artifactId>jib-maven-plugin</artifactId>
  <version>3.4.4</version>
  <configuration>
    <from><image>eclipse-temurin:21-jre</image></from>
    <to><image>${jib.to.image}</image></to>
  </configuration>
</plugin>
```

### dame-updater.yaml

```yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet  # or Application with annotation
metadata:
  annotations:
    argocd-image-updater.argoproj.io/image-list: dame=ghcr.io/oseitutu90/dame
    argocd-image-updater.argoproj.io/dame.update-strategy: newest-build
```

---

## Required Secrets

| Secret | Namespace | Purpose |
|--------|-----------|---------|
| `ghcr-creds` | Jenkins (credential store) | Push images to GHCR |
| `ghcr-secret` | `argocd-image-updater-system` | Pull image tags from GHCR |
| `git-creds` | `argocd-image-updater-system` | Commit tag updates to GitHub |

### Creating Jenkins Credential

1. Manage Jenkins → Credentials → Global
2. Add: Username with password
   - Username: `oseitutu90`
   - Password: GitHub PAT with `write:packages` scope
   - ID: `ghcr-creds`

### Creating Kubernetes Secrets

```bash
# GHCR read access for Image Updater
kubectl create secret docker-registry ghcr-secret \
  -n argocd-image-updater-system \
  --docker-server=ghcr.io \
  --docker-username=oseitutu90 \
  --docker-password=<PAT>

# Git push access for Image Updater
kubectl create secret generic git-creds \
  -n argocd-image-updater-system \
  --from-literal=username=oseitutu90 \
  --from-literal=password=<PAT>
```

---

## Troubleshooting

### Jenkins Build Fails

| Error | Solution |
|-------|----------|
| `git: dubious ownership` | Jenkinsfile adds `git config --global --add safe.directory` |
| `No plugin found for jib` | Use fully qualified: `com.google.cloud.tools:jib-maven-plugin:build` |
| `Plugin version not found` | Use stable version (3.4.4) |
| `403 Forbidden` on GHCR push | PAT needs `write:packages` scope (use classic token) |
| `Could not find credentials ghcr-creds` | Create credential in Jenkins UI |

### Image Updater Issues

| Issue | Solution |
|-------|----------|
| Not detecting new images | Check `ghcr-secret` has read access |
| Not committing updates | Check `git-creds` has repo write access |
| Wrong update strategy | Use `newest-build` for non-semver tags |

---

## Manual Operations

### Trigger Jenkins Build

1. Open Jenkins UI
2. Navigate to `dame-1` job
3. Click "Build Now"

### Force Argo CD Sync

```bash
argocd app sync dame --force
```

### Check Image Updater Logs

```bash
kubectl logs -n argocd-image-updater-system -l app=argocd-image-updater
```
