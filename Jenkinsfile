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

                archiveArtifacts(
                    artifacts: 'target/costmonitor-0.0.1-SNAPSHOT.jar',
                    fingerprint: true
                )
            }
        }

        stage('Stop Existing Application') {
            steps {
                echo 'Checking for existing application on port 8081...'

                powershell '''
                    $connections = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($connections) {

                        foreach ($connection in $connections) {

                            $pid = $connection.OwningProcess

                            Write-Host "Stopping process PID: $pid"

                            Stop-Process `
                                -Id $pid `
                                -Force `
                                -ErrorAction SilentlyContinue
                        }

                        Start-Sleep -Seconds 2
                    }
                    else {
                        Write-Host "No application is currently running on port 8081."
                    }
                '''
            }
        }

        stage('Run Application') {
            steps {
                echo 'Starting Spring Boot application on port 8081...'

                bat '''
                    if exist application.log del /F /Q application.log

                    set JENKINS_NODE_COOKIE=dontKillMe

                    start "" /B java -jar "target\\costmonitor-0.0.1-SNAPSHOT.jar" > application.log 2>&1
                '''

                echo 'Waiting for application to start...'

                sleep 15
            }
        }

        stage('Verify Application') {
            steps {
                echo 'Verifying application on port 8081...'

                powershell '''
                    $maxAttempts = 6
                    $started = $false

                    for ($i = 1; $i -le $maxAttempts; $i++) {

                        Write-Host "Checking application - attempt $i/$maxAttempts"

                        try {

                            $response = Invoke-WebRequest `
                                -Uri "http://localhost:8081" `
                                -UseBasicParsing `
                                -TimeoutSec 5 `
                                -ErrorAction Stop

                            Write-Host "Application is running."
                            Write-Host "HTTP Status: $($response.StatusCode)"

                            $started = $true
                            break
                        }
                        catch {

                            Write-Host "Application is not ready yet..."

                            Start-Sleep -Seconds 5
                        }
                    }

                    if (-not $started) {

                        Write-Host "Application failed to start on port 8081."

                        Write-Host "========== application.log =========="

                        if (Test-Path "application.log") {
                            Get-Content "application.log"
                        }
                        else {
                            Write-Host "application.log was not created."
                        }

                        exit 1
                    }
                '''
            }
        }
    }

    post {

        success {
            echo '=============================================='
            echo 'BUILD SUCCESSFUL!'
            echo 'Application is running on:'
            echo 'http://localhost:8081'
            echo '=============================================='
        }

        failure {
            echo '=============================================='
            echo 'BUILD FAILED!'
            echo '=============================================='
            echo 'Check the Jenkins console output and application.log.'
        }

        always {
            echo 'Jenkins pipeline completed.'
        }
    }
}
