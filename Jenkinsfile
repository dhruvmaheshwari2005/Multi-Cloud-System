pipeline {

    agent any

    tools {
        jdk 'JDK'
        maven 'Maven'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                git branch: 'main',
                    url: 'https://github.com/dhruvmaheshwari2005/Multi-Cloud-System.git'
            }
        }

        stage('Build') {
            steps {
                echo 'Building Spring Boot application...'
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
                echo 'Archiving JAR file...'
                archiveArtifacts artifacts: 'target/*.jar',
                                 fingerprint: true
            }
        }

        stage('Run Application') {
            steps {
                echo 'Starting Spring Boot application...'
                bat 'start /B java -jar target/*.jar'
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

        always {
            echo 'Jenkins pipeline completed.'
        }
    }
}