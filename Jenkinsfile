pipeline {
    agent {
        node {
            label 'any'
            customWorkspace '/var/lib/jenkins/workspace/ENCOM-Shared'
        }
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
                checkout scm
                script {
                    env.BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
                }
            }
        }
        
        stage('Test') {
            when {
                expression { return params.SKIP_TESTS == false }
            }
            steps {
                sh '''
                    chmod +x gradlew
                    ./gradlew test
                '''
                junit 'build/test-results/test/*.xml'
            }
        }
        
        stage('Build') {
            steps {
                sh '''
                    chmod +x gradlew
                    ./gradlew fatJar
                '''
                archiveArtifacts artifacts: 'build/libs/*.jar'
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