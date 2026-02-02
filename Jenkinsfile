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
          - name: kubectl
            image: bitnami/kubectl:latest
            command:
            - sleep
            args:
            - infinity
        '''
      defaultContainer 'maven'
    }
  }

  parameters {
    choice(name: 'DEPLOY_COLOR', choices: ['green', 'blue'], description: 'Which color to deploy to')
    booleanParam(name: 'AUTO_SWITCH', defaultValue: true, description: 'Auto-switch traffic after deployment')
    booleanParam(name: 'SKIP_BUILD', defaultValue: false, description: 'Skip build, just deploy/switch')
  }

  environment {
    APP_NAME = 'dame'
    IMAGE = 'ghcr.io/oseitutu90/dame'
    NAMESPACE = 'dame'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Test') {
      when { expression { !params.SKIP_BUILD } }
      steps { sh 'mvn -B test' }
    }

    stage('Build & Push') {
      when { expression { !params.SKIP_BUILD } }
      steps {
        script {
          sh 'git config --global --add safe.directory "${WORKSPACE}"'
          env.GIT_SHA = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
        }
        withCredentials([usernamePassword(
            credentialsId: 'ghcr-creds',
            usernameVariable: 'REG_USER',
            passwordVariable: 'REG_PASS'
        )]) {
          sh """
            mvn -B -Pproduction package jib:build -DskipTests \
              -Djib.to.image=${IMAGE} \
              -Djib.to.tag=${env.GIT_SHA} \
              -Djib.to.auth.username=\$REG_USER \
              -Djib.to.auth.password=\$REG_PASS
          """
        }
      }
    }

    stage('Deploy to Color') {
      steps {
        container('kubectl') {
          script {
            def color = params.DEPLOY_COLOR
            def tag = env.GIT_SHA ?: sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
            
            echo "🚀 Deploying ${IMAGE}:${tag} to dame-${color}"
            
            sh """
              kubectl set image deployment/dame-${color} dame=${IMAGE}:${tag} -n ${NAMESPACE}
              kubectl rollout status deployment/dame-${color} -n ${NAMESPACE} --timeout=300s
            """
          }
        }
      }
    }

    stage('Smoke Test') {
      steps {
        container('kubectl') {
          script {
            def color = params.DEPLOY_COLOR
            echo "🔍 Running smoke test on dame-${color}"
            
            sh """
              kubectl run smoke-test-\${BUILD_NUMBER} -n ${NAMESPACE} --rm -i \
                --image=curlimages/curl --restart=Never -- \
                curl -sf http://dame-${color}.${NAMESPACE}.svc.cluster.local:8080/actuator/health
            """
          }
        }
      }
    }

    stage('Switch Traffic') {
      when { expression { params.AUTO_SWITCH } }
      steps {
        container('kubectl') {
          script {
            def active = params.DEPLOY_COLOR
            def inactive = (active == 'blue') ? 'green' : 'blue'
            
            echo "🔄 Switching traffic: ${inactive} → ${active}"
            
            sh """
              kubectl patch virtualservice dame-vs -n ${NAMESPACE} --type=merge -p '{
                "spec": {"http": [{"route": [
                  {"destination": {"host": "dame", "subset": "${active}"}, "weight": 100},
                  {"destination": {"host": "dame", "subset": "${inactive}"}, "weight": 0}
                ]}]}
              }'
            """
          }
        }
      }
    }
  }

  post {
    success { 
      echo "✅ Build ${BUILD_NUMBER}: Deployed to ${params.DEPLOY_COLOR}. Traffic switched: ${params.AUTO_SWITCH}" 
    }
    failure { 
      echo "❌ Build ${BUILD_NUMBER} failed. Traffic unchanged - still serving from previous stable." 
    }
  }
}
