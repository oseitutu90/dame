pipeline {
  agent {
    kubernetes {
      yaml '''
        apiVersion: v1
        kind: Pod
        spec:
          containers:
          - name: maven
            image: maven:3.9.6-eclipse-temurin-21
            command:
            - sleep
            args:
            - infinity
            resources:
              requests:
                memory: "1Gi"
                cpu: "500m"
              limits:
                memory: "2Gi"
                cpu: "1"
        '''
      defaultContainer 'maven'
    }
  }

  environment {
    APP_NAME = 'dame'
    IMAGE    = 'ghcr.io/oseitutu90/dame'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Test') {
      steps { sh 'mvn -B test' }
    }

    stage('Build & Push (Jib)') {
      steps {
        script {
          env.GIT_SHA = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
        }
        withCredentials([usernamePassword(
            credentialsId: 'ghcr-creds',
            usernameVariable: 'REG_USER',
            passwordVariable: 'REG_PASS'
        )]) {
          sh """
            mvn -B -Pproduction jib:build \
              -Djib.to.image=${IMAGE} \
              -Djib.to.tag=${GIT_SHA} \
              -Djib.to.auth.username=\$REG_USER \
              -Djib.to.auth.password=\$REG_PASS
          """
        }
      }
    }
  }

  post {
    success { echo "✅ Pushed ${IMAGE}:${env.GIT_SHA}. Image Updater will detect and deploy." }
    failure { echo "❌ Build failed. Check logs above." }
  }
}
