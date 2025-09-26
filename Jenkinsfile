pipeline {
    agent any
    
    options {
        skipDefaultCheckout(true)
    }
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'prod'],
            description: 'Target environment for deployment'
        )
        choice(
            name: 'ACTION',
            choices: ['bootstrap', 'plan', 'apply'],
            description: 'Terraform action to perform'
        )
    }
    
    environment {
        AWS_REGION = 'us-west-1'
        PROJECT_NAME = 'encom-lambda'
        TF_IN_AUTOMATION = 'true'
    }
    
    tools {
        terraform 'Terraform-1.5'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    def gitCommit = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    env.BUILD_VERSION = "${env.BUILD_NUMBER}-${gitCommit.take(7)}"
                }
            }
        }
        
        stage('Test & Build JAR') {
            when {
                expression { params.ACTION == 'plan' || params.ACTION == 'apply' }
            }
            steps {
                sh '''
                    echo "Running tests..."
                    chmod +x gradlew
                    ./gradlew test
                    
                    echo "Building JAR..."
                    ./gradlew fatJar
                '''
                junit 'build/test-results/test/*.xml'
                archiveArtifacts artifacts: 'build/libs/*.jar'
            }
        }
        
        stage('Terraform Bootstrap') {
            when {
                expression { params.ACTION == 'bootstrap' }
            }
            steps {
                script {
                    def awsCredentials = params.ENVIRONMENT == 'prod' ? 'aws-encom-prod' : 'aws-encom-dev'
                    
                    withAWS(credentials: awsCredentials, region: env.AWS_REGION) {
                        dir("terraform/bootstrap") {
                            sh """
                                echo "Bootstrapping Terraform state backend for ${params.ENVIRONMENT}..."
                                terraform init
                                terraform plan -var="environment=${params.ENVIRONMENT}" -var="region=${env.AWS_REGION}" -out=bootstrap-plan
                                terraform apply bootstrap-plan
                                echo "Bootstrap completed successfully"
                            """
                        }
                    }
                }
            }
        }
        
        stage('Terraform Init') {
            when {
                expression { params.ACTION == 'plan' || params.ACTION == 'apply' }
            }
            steps {
                script {
                    def awsCredentials = params.ENVIRONMENT == 'prod' ? 'aws-encom-prod' : 'aws-encom-dev'
                    
                    withAWS(credentials: awsCredentials, region: env.AWS_REGION) {
                        dir("terraform/environments/${params.ENVIRONMENT}") {
                            sh '''
                                echo "Initializing Terraform..."
                                terraform init
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Terraform Validate') {
            when {
                expression { params.ACTION == 'plan' || params.ACTION == 'apply' }
            }
            steps {
                script {
                    def awsCredentials = params.ENVIRONMENT == 'prod' ? 'aws-encom-prod' : 'aws-encom-dev'
                    
                    withAWS(credentials: awsCredentials, region: env.AWS_REGION) {
                        dir("terraform/environments/${params.ENVIRONMENT}") {
                            sh '''
                                echo "Validating Terraform configuration..."
                                terraform validate
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Terraform Plan') {
            when {
                expression { params.ACTION == 'plan' || params.ACTION == 'apply' }
            }
            steps {
                script {
                    def awsCredentials = params.ENVIRONMENT == 'prod' ? 'aws-encom-prod' : 'aws-encom-dev'
                    
                    withAWS(credentials: awsCredentials, region: env.AWS_REGION) {
                        dir("terraform/environments/${params.ENVIRONMENT}") {
                            sh '''
                                echo "Planning Terraform changes..."
                                terraform plan -var-file=terraform.tfvars -out=tfplan
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Terraform Apply') {
            when {
                expression { params.ACTION == 'apply' }
            }
            steps {
                script {
                    def awsCredentials = params.ENVIRONMENT == 'prod' ? 'aws-encom-prod' : 'aws-encom-dev'
                    
                    withAWS(credentials: awsCredentials, region: env.AWS_REGION) {
                        dir("terraform/environments/${params.ENVIRONMENT}") {
                            sh '''
                                echo "Applying Terraform changes..."
                                terraform apply tfplan
                            '''
                        }
                    }
                }
            }
        }
        
    }
    
    post {
        always {
            script {
                if (params.ACTION == 'bootstrap') {
                    echo "Bootstrap complete! State bucket created for ${params.ENVIRONMENT} environment."
                    echo "Next steps:"
                    echo "1. Run import process using terraform/import-resources.md"
                    echo "2. Run ACTION=plan to validate configuration"
                    echo "3. Run ACTION=apply to deploy Lambda function"
                } else if (params.ACTION == 'apply') {
                    echo "Deployment complete!"
                    echo "Environment: ${params.ENVIRONMENT}"
                    
                    // Get Lambda function info if deployment was successful
                    try {
                        def awsCredentials = params.ENVIRONMENT == 'prod' ? 'aws-encom-prod' : 'aws-encom-dev'
                        withAWS(credentials: awsCredentials, region: env.AWS_REGION) {
                            dir("terraform/environments/${params.ENVIRONMENT}") {
                                def apiEndpoint = sh(
                                    script: 'terraform output -raw api_gateway_endpoint',
                                    returnStdout: true
                                ).trim()
                                echo "API Endpoint: ${apiEndpoint}"
                                
                                def lambdaName = sh(
                                    script: 'terraform output -raw lambda_function_name',
                                    returnStdout: true
                                ).trim()
                                echo "Lambda Function: ${lambdaName}"
                            }
                        }
                    } catch (Exception e) {
                        echo "Could not retrieve deployment info: ${e.message}"
                    }
                } else {
                    echo "Terraform plan completed. Review the plan and run with ACTION=apply to deploy."
                }
                
                // Cleanup
                try {
                    cleanWs()
                } catch (Exception e) {
                    echo "Warning: Workspace cleanup failed: ${e.message}"
                }
            }
        }
        failure {
            echo "Pipeline failed! Check the logs above for errors."
        }
        success {
            echo "Pipeline completed successfully!"
        }
    }
}