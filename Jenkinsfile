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
            description: 'Deployment environment'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip unit tests'
        )
        booleanParam(
            name: 'BUILD_ONLY',
            defaultValue: false,
            description: 'Build JAR only without deploying infrastructure'
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
            when {
                expression { return params.SKIP_TESTS == false }
            }
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
        
        stage('Deploy to Development') {
            when {
                allOf {
                    expression { params.ENVIRONMENT == 'dev' }
                    expression { !params.BUILD_ONLY }
                }
            }
            steps {
                withAWS(credentials: 'aws-encom-dev', region: env.AWS_REGION) {
                    dir('../encom-infrastructure/environments/dev') {
                        sh '''
                            terraform init
                            terraform plan -var-file=terraform.tfvars
                            terraform apply -var-file=terraform.tfvars -auto-approve
                        '''
                    }
                }
            }
        }
        
        stage('Integration Tests') {
            when {
                allOf {
                    expression { params.ENVIRONMENT == 'dev' }
                    expression { !params.BUILD_ONLY }
                }
            }
            steps {
                withAWS(credentials: 'aws-encom-dev', region: env.AWS_REGION) {
                    script {
                        def apiEndpoint = sh(
                            script: 'cd ../encom-infrastructure/environments/dev && terraform output -raw api_gateway_endpoint',
                            returnStdout: true
                        ).trim()
                        
                        sh """
                            curl -X POST "${apiEndpoint}" \
                                -H "Content-Type: application/json" \
                                -d '{"hexagonCount": 10}' \
                                --fail --silent --show-error
                        """
                    }
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                allOf {
                    expression { params.ENVIRONMENT == 'prod' }
                    expression { !params.BUILD_ONLY }
                    branch 'main'
                }
            }
            steps {
                input message: 'Deploy to Production?', ok: 'Deploy'
                withAWS(credentials: 'aws-encom-prod', region: env.AWS_REGION) {
                    dir('../encom-infrastructure/environments/prod') {
                        sh '''
                            terraform init
                            terraform plan -var-file=terraform.tfvars
                            terraform apply -var-file=terraform.tfvars -auto-approve
                        '''
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                if (params.BUILD_ONLY) {
                    echo "Build-only mode: JAR artifacts preserved for infrastructure deployment"
                } else {
                    cleanWs()
                }
            }
        }
    }
}