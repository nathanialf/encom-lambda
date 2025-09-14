pipeline {
    agent {
        node {
            label 'any'
            customWorkspace '/var/lib/jenkins/workspace/ENCOM-Shared'
        }
    }
    
    options {
        skipDefaultCheckout(true)
    }
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'prod'],
            description: 'Target environment for artifact upload'
        )
    }
    
    environment {
        AWS_REGION = 'us-west-1'
        PROJECT_NAME = 'encom-lambda'
    }
    
    stages {
        stage('Checkout') {
            steps {
                // Checkout Lambda code to subdirectory to preserve shared workspace
                dir('encom-lambda') {
                    checkout scm
                }
                script {
                    def gitCommit = sh(script: 'cd encom-lambda && git rev-parse HEAD', returnStdout: true).trim()
                    env.BUILD_VERSION = "${env.BUILD_NUMBER}-${gitCommit.take(7)}"
                }
            }
        }
        
        stage('Test') {
            steps {
                dir('encom-lambda') {
                    sh '''
                        chmod +x gradlew
                        ./gradlew test
                    '''
                }
                junit 'encom-lambda/build/test-results/test/*.xml'
            }
        }
        
        stage('Build') {
            steps {
                dir('encom-lambda') {
                    sh '''
                        chmod +x gradlew
                        ./gradlew fatJar
                    '''
                }
                archiveArtifacts artifacts: 'encom-lambda/build/libs/*.jar'
                
                // Upload JAR to S3 for Infrastructure pipeline
                withAWS(credentials: 'aws-encom-dev', region: env.AWS_REGION) {
                    script {
                        def bucketName = 'encom-build-artifacts-dev-us-west-1'
                        def s3Key = "artifacts/lambda/encom-lambda-${env.BUILD_VERSION}.jar"
                        
                        echo "Using S3 bucket: ${bucketName} (must be created manually)"
                        
                        // Upload versioned JAR
                        s3Upload bucket: bucketName,
                                file: 'encom-lambda/build/libs/encom-lambda-1.0.0-all.jar',
                                path: s3Key
                        
                        // Also upload as "latest" for easy access
                        s3Upload bucket: bucketName,
                                file: 'encom-lambda/build/libs/encom-lambda-1.0.0-all.jar',
                                path: 'artifacts/lambda/encom-lambda-latest.jar'
                        
                        echo "JAR uploaded to S3: s3://${bucketName}/${s3Key}"
                        echo "Latest JAR: s3://${bucketName}/artifacts/lambda/encom-lambda-latest.jar"
                    }
                }
            }
        }
        
    }
    
    post {
        always {
            echo "Lambda build complete. JAR uploaded to S3 and ready for deployment."
            echo "Run ENCOM-Infrastructure pipeline to deploy infrastructure with latest JAR."
            cleanWs()
        }
    }
}