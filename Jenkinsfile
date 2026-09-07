pipeline {

    agent any

    tools {
        maven 'Maven'
    }

    stages {

        stage('Build') {
            steps {
                echo 'Building application...'
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                bat 'mvn test'
            }
        }

        stage('Archive JAR') {
            steps {
                echo 'Archiving JAR...'
                archiveArtifacts artifacts: 'target/*.jar',
                                 fingerprint: true
            }
        }

        stage('Stop Existing Application') {
            steps {
                echo 'Checking for existing application on port 8081...'

                bat '''
                    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081 ^| findstr LISTENING') do (
                        echo Stopping process %%a
                        taskkill /PID %%a /F
                    )
                '''
            }
        }

        stage('Run Application') {
            steps {
                echo 'Starting application on port 8081...'

                bat '''
                    set JENKINS_NODE_COOKIE=dontKillMe
                    start "" /B java -jar "target\\costmonitor-0.0.1-SNAPSHOT.jar" > application.log 2>&1
                '''

                sleep 10

                bat '''
                    netstat -ano | findstr :8081
                '''
            }
        }

        stage('Verify Application') {
            steps {
                echo 'Verifying application...'

                bat '''
                    powershell -Command "try { $response = Invoke-WebRequest -Uri http://localhost:8081 -UseBasicParsing -TimeoutSec 10; Write-Host ('Application returned HTTP ' + $response.StatusCode) } catch { Write-Host 'Application is not responding on port 8081'; exit 1 }"
                '''
            }
        }
    }

    post {
        success {
            echo '================================='
            echo 'BUILD SUCCESSFUL!'
            echo 'Application is running at:'
            echo 'http://localhost:8081'
            echo '================================='
        }

        failure {
            echo '================================='
            echo 'BUILD FAILED!'
            echo 'Check application.log for application errors.'
            echo '================================='
        }

        always {
            echo 'Jenkins pipeline completed.'
        }
    }
}