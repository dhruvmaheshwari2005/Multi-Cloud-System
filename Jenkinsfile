pipeline {

    agent any

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

    }

    post {
        success {
            echo '================================='
            echo 'BUILD SUCCESSFUL!'
            echo '================================='
        }

        failure {
            echo '================================='
            echo 'BUILD FAILED!'
            echo '================================='
        }
    }
}