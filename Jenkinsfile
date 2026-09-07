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

        stage('Run Application') {
            steps {
                echo 'Starting application on port 8081...'
                bat 'start /B java -jar target\\costmonitor-0.0.1-SNAPSHOT.jar'
            }
        }
    }

    post {
        success {
            echo '================================='
            echo 'BUILD SUCCESSFUL!'
            echo 'Application: http://localhost:8081'
            echo '================================='
        }

        failure {
            echo '================================='
            echo 'BUILD FAILED!'
            echo '================================='
        }
    }
}