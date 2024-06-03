pipeline {
    agent any
    tools {maven '3.9.7'}

    stages {
        stage('Build') {
            steps {
                script {
                    echo 'Building...'
                    sh 'mvn --version'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
                }
            }
        }
        stage('Test') {
            steps {
                script {
                    echo 'Testing...'
                    sh 'mvn test'
                }
            }
            post {
                success {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Code Quality Analysis') {
            steps {
                script {
                    echo 'Analyzing Code Quality...'
                    // Assuming SonarQube is already configured in Jenkins
                    withSonarQubeEnv('SonarQube') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }
        stage('Deploy to Test') {
            steps {
                script {
                    echo 'Deploying to Test Environment...'
                    sh 'docker build -t demo-app:test .'
                    sh 'docker run -d -p 8080:8080 demo-app:test'
                }
            }
        }
        stage('Release to Production') {
            steps {
                input message: 'Deploy to Production?', ok: 'Deploy'
                script {
                    echo 'Deploying to Production...'
                    sh 'docker build -t demo-app:latest .'
                    sh 'docker run -d -p 80:8080 demo-app:latest'
                }
            }
        }
    }

    post {
        always {
            echo 'Cleaning up...'
            sh 'docker stop $(docker ps -a -q) || true'
            sh 'docker rm $(docker ps -a -q) || true'
            cleanWs()
        }
        failure {
            mail to: 'natusvincere45@gmail.com',
                 subject: "Pipeline failed: ${currentBuild.fullDisplayName}",
                 body: "Something is wrong with ${env.BUILD_URL}"
        }
    }
}
