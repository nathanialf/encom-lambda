# ENCOM Lambda Resource Import Strategy

This document outlines the process to import existing AWS resources from the encom-infrastructure terraform state to the new independent encom-lambda terraform configuration.

## Overview

We need to import existing AWS resources to avoid destroying them during the migration from the monolithic encom-infrastructure setup to independent service-specific terraform configurations.

## Prerequisites

1. **AWS Profiles**: Ensure AWS profiles are configured for both environments:
   - `encom-dev` profile for development resources
   - `encom-prod` profile for production resources
2. **Backup Current State**: Export existing terraform state from both environments
3. **Resource Discovery**: Obtain actual AWS resource IDs before importing

## AWS Profile Configuration

Ensure your `~/.aws/config` and `~/.aws/credentials` files include the encom profiles:

```ini
# ~/.aws/config
[profile encom-dev]
region = us-west-1
output = json

[profile encom-prod]
region = us-west-1
output = json

# ~/.aws/credentials
[encom-dev]
aws_access_key_id = YOUR_DEV_ACCESS_KEY
aws_secret_access_key = YOUR_DEV_SECRET_KEY

[encom-prod]
aws_access_key_id = YOUR_PROD_ACCESS_KEY  
aws_secret_access_key = YOUR_PROD_SECRET_KEY
```

## Resource Discovery Commands

Before running imports, obtain the actual resource IDs using the appropriate AWS profiles:

### Dev Environment Resource Discovery
```bash
# Set AWS profile for dev environment
export AWS_PROFILE=encom-dev

# Get Lambda Function ARNs
aws lambda get-function --function-name encom-map-generator-dev --query 'Configuration.FunctionArn' --output text

# Get API Gateway IDs
aws apigateway get-rest-apis --query 'items[?name==`encom-api-dev`].[id,name]' --output table

# Get IAM Role ARNs
aws iam get-role --role-name encom-map-generator-dev-role --query 'Role.Arn' --output text

# For API Gateway, get resource structure (replace <API_ID> with actual API Gateway ID)
aws apigateway get-resources --rest-api-id <API_ID> --query 'items[].[id,pathPart,parentId]' --output table

# Get deployment IDs
aws apigateway get-deployments --rest-api-id <API_ID> --query 'items[0].id' --output text
```

### Prod Environment Resource Discovery
```bash
# Set AWS profile for prod environment
export AWS_PROFILE=encom-prod

# Get Lambda Function ARNs
aws lambda get-function --function-name encom-map-generator-prod --query 'Configuration.FunctionArn' --output text

# Get API Gateway IDs
aws apigateway get-rest-apis --query 'items[?name==`encom-api-prod`].[id,name]' --output table

# Get IAM Role ARNs
aws iam get-role --role-name encom-map-generator-prod-role --query 'Role.Arn' --output text

# For API Gateway, get resource structure (replace <API_ID> with actual API Gateway ID)
aws apigateway get-resources --rest-api-id <API_ID> --query 'items[].[id,pathPart,parentId]' --output table

# Get deployment IDs
aws apigateway get-deployments --rest-api-id <API_ID> --query 'items[0].id' --output text

# Get API key information if exists
aws apigateway get-api-keys --name-query encom --query 'items[].[id,name]' --output table
```

## Import Process

### Step 1: Initialize New Terraform Backends

```bash
# Dev Environment
cd /primary/dev/encom/encom-lambda/terraform/environments/dev
export AWS_PROFILE=encom-dev
terraform init

# Prod Environment  
cd /primary/dev/encom/encom-lambda/terraform/environments/prod
export AWS_PROFILE=encom-prod
terraform init
```

### Step 2: Import Dev Environment Resources

```bash
cd /primary/dev/encom/encom-lambda/terraform/environments/dev
export AWS_PROFILE=encom-dev

# Import Lambda Function
terraform import module.lambda.aws_lambda_function.function encom-map-generator-dev

# Import Lambda IAM Role
terraform import module.lambda.aws_iam_role.lambda_role encom-map-generator-dev-role

# Import Lambda IAM Policy Attachments
terraform import module.lambda.aws_iam_role_policy_attachment.lambda_basic_execution encom-map-generator-dev-role/arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# Import Lambda Alias
terraform import module.lambda.aws_lambda_alias.function_alias encom-map-generator-dev:live

# Import CloudWatch Log Group for Lambda
terraform import module.lambda.aws_cloudwatch_log_group.lambda_logs /aws/lambda/encom-map-generator-dev

# Import API Gateway REST API (replace with actual API ID)
terraform import module.api_gateway.aws_api_gateway_rest_api.api <DEV_API_GATEWAY_ID>

# Import API Gateway Resources (replace with actual resource IDs)
terraform import module.api_gateway.aws_api_gateway_resource.api_resource <API_ID>/<API_RESOURCE_ID>
terraform import module.api_gateway.aws_api_gateway_resource.v1_resource <API_ID>/<V1_RESOURCE_ID>  
terraform import module.api_gateway.aws_api_gateway_resource.map_resource <API_ID>/<MAP_RESOURCE_ID>
terraform import module.api_gateway.aws_api_gateway_resource.generate_resource <API_ID>/<GENERATE_RESOURCE_ID>

# Import API Gateway Methods
terraform import module.api_gateway.aws_api_gateway_method.options_method <API_ID>/<GENERATE_RESOURCE_ID>/OPTIONS
terraform import module.api_gateway.aws_api_gateway_method.post_method <API_ID>/<GENERATE_RESOURCE_ID>/POST

# Import API Gateway Integrations
terraform import module.api_gateway.aws_api_gateway_integration.options_integration <API_ID>/<GENERATE_RESOURCE_ID>/OPTIONS
terraform import module.api_gateway.aws_api_gateway_integration.lambda_integration <API_ID>/<GENERATE_RESOURCE_ID>/POST

# Import API Gateway Method Responses
terraform import module.api_gateway.aws_api_gateway_method_response.options_response <API_ID>/<GENERATE_RESOURCE_ID>/OPTIONS/200
terraform import module.api_gateway.aws_api_gateway_method_response.post_response_200 <API_ID>/<GENERATE_RESOURCE_ID>/POST/200
terraform import module.api_gateway.aws_api_gateway_method_response.post_response_400 <API_ID>/<GENERATE_RESOURCE_ID>/POST/400
terraform import module.api_gateway.aws_api_gateway_method_response.post_response_500 <API_ID>/<GENERATE_RESOURCE_ID>/POST/500

# Import API Gateway Integration Response
terraform import module.api_gateway.aws_api_gateway_integration_response.options_integration_response <API_ID>/<GENERATE_RESOURCE_ID>/OPTIONS/200

# Import API Gateway Deployment (replace with actual deployment ID)
terraform import module.api_gateway.aws_api_gateway_deployment.deployment <DEPLOYMENT_ID>

# Import API Gateway Stage
terraform import module.api_gateway.aws_api_gateway_stage.stage <API_ID>/dev

# Import CloudWatch Log Group for API Gateway
terraform import module.api_gateway.aws_cloudwatch_log_group.api_gateway_logs /aws/apigateway/encom-api-dev

# Import Lambda Permission for API Gateway
terraform import module.lambda.aws_lambda_permission.api_gateway_invoke encom-map-generator-dev/AllowExecutionFromAPIGateway
```

### Step 3: Import Prod Environment Resources

```bash
cd /primary/dev/encom/encom-lambda/terraform/environments/prod
export AWS_PROFILE=encom-prod

# Import Lambda Function
terraform import module.lambda.aws_lambda_function.function encom-map-generator-prod

# Import Lambda IAM Role
terraform import module.lambda.aws_iam_role.lambda_role encom-map-generator-prod-role

# Import Lambda IAM Policy Attachments  
terraform import module.lambda.aws_iam_role_policy_attachment.lambda_basic_execution encom-map-generator-prod-role/arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# Import Lambda Alias
terraform import module.lambda.aws_lambda_alias.function_alias encom-map-generator-prod:live

# Import CloudWatch Log Group for Lambda
terraform import module.lambda.aws_cloudwatch_log_group.lambda_logs /aws/lambda/encom-map-generator-prod

# Import API Gateway REST API (replace with actual API ID)
terraform import module.api_gateway.aws_api_gateway_rest_api.api <PROD_API_GATEWAY_ID>

# Import API Gateway Resources (replace with actual resource IDs)
terraform import module.api_gateway.aws_api_gateway_resource.api_resource <API_ID>/<API_RESOURCE_ID>
terraform import module.api_gateway.aws_api_gateway_resource.v1_resource <API_ID>/<V1_RESOURCE_ID>
terraform import module.api_gateway.aws_api_gateway_resource.map_resource <API_ID>/<MAP_RESOURCE_ID> 
terraform import module.api_gateway.aws_api_gateway_resource.generate_resource <API_ID>/<GENERATE_RESOURCE_ID>

# Import API Gateway Methods
terraform import module.api_gateway.aws_api_gateway_method.options_method <API_ID>/<GENERATE_RESOURCE_ID>/OPTIONS
terraform import module.api_gateway.aws_api_gateway_method.post_method <API_ID>/<GENERATE_RESOURCE_ID>/POST

# Import API Gateway Integrations
terraform import module.api_gateway.aws_api_gateway_integration.options_integration <API_ID>/<GENERATE_RESOURCE_ID>/OPTIONS  
terraform import module.api_gateway.aws_api_gateway_integration.lambda_integration <API_ID>/<GENERATE_RESOURCE_ID>/POST

# Import API Gateway Method Responses
terraform import module.api_gateway.aws_api_gateway_method_response.options_response <API_ID>/<GENERATE_RESOURCE_ID>/OPTIONS/200
terraform import module.api_gateway.aws_api_gateway_method_response.post_response_200 <API_ID>/<GENERATE_RESOURCE_ID>/POST/200
terraform import module.api_gateway.aws_api_gateway_method_response.post_response_400 <API_ID>/<GENERATE_RESOURCE_ID>/POST/400
terraform import module.api_gateway.aws_api_gateway_method_response.post_response_500 <API_ID>/<GENERATE_RESOURCE_ID>/POST/500

# Import API Gateway Integration Response
terraform import module.api_gateway.aws_api_gateway_integration_response.options_integration_response <API_ID>/<GENERATE_RESOURCE_ID>/OPTIONS/200

# Import API Gateway Deployment (replace with actual deployment ID)
terraform import module.api_gateway.aws_api_gateway_deployment.deployment <DEPLOYMENT_ID>

# Import API Gateway Stage
terraform import module.api_gateway.aws_api_gateway_stage.stage <API_ID>/prod

# Import CloudWatch Log Group for API Gateway
terraform import module.api_gateway.aws_cloudwatch_log_group.api_gateway_logs /aws/apigateway/encom-api-prod

# Import Lambda Permission for API Gateway
terraform import module.lambda.aws_lambda_permission.api_gateway_invoke encom-map-generator-prod/AllowExecutionFromAPIGateway

# For Production - Import API Key resources (if they exist)
# Only run these if API keys are actually enabled in prod
terraform import module.api_gateway.aws_api_gateway_api_key.api_key[0] <API_KEY_ID>
terraform import module.api_gateway.aws_api_gateway_usage_plan.usage_plan[0] <USAGE_PLAN_ID>  
terraform import module.api_gateway.aws_api_gateway_usage_plan_key.usage_plan_key[0] <USAGE_PLAN_KEY_ID>
```

## Validation Steps

After importing all resources:

### 1. Verify Import Success
```bash
# In dev environment
cd /primary/dev/encom/encom-lambda/terraform/environments/dev
export AWS_PROFILE=encom-dev
terraform plan
# Should show no changes (or minimal changes like tags)

terraform validate
# Should show no errors

# In prod environment
cd /primary/dev/encom/encom-lambda/terraform/environments/prod
export AWS_PROFILE=encom-prod
terraform plan
# Should show no changes (or minimal changes like tags)

terraform validate
# Should show no errors
```

### 2. Test API Endpoints
```bash
# Test dev endpoint
export AWS_PROFILE=encom-dev
curl -X POST "https://<DEV_API_ID>.execute-api.us-west-1.amazonaws.com/dev/api/v1/map/generate" \
  -H "Content-Type: application/json" \
  -d '{"width": 10, "height": 10, "seed": 12345}'

# Test prod endpoint (with API key if enabled)
export AWS_PROFILE=encom-prod
curl -X POST "https://<PROD_API_ID>.execute-api.us-west-1.amazonaws.com/prod/api/v1/map/generate" \
  -H "Content-Type: application/json" \
  -H "x-api-key: <API_KEY>" \
  -d '{"width": 10, "height": 10, "seed": 12345}'
```

### 3. Validate Configurations
- Check Lambda environment variables match
- Verify API Gateway CORS settings
- Confirm CloudWatch log retention periods
- Test throttling and quota limits

## Bootstrap State Buckets

Before running imports, ensure the new state buckets exist:

```bash
# Create dev state bucket
export AWS_PROFILE=encom-dev
cd /primary/dev/encom/encom-lambda/terraform/bootstrap
terraform init
terraform apply -var="environment=dev"

# Create prod state bucket  
export AWS_PROFILE=encom-prod
cd /primary/dev/encom/encom-lambda/terraform/bootstrap
terraform init
terraform apply -var="environment=prod"
```

## Rollback Plan

If import fails or causes issues:

1. **Restore Original State**: Restore the encom-infrastructure terraform state from backup
2. **Clean New State**: Delete the new encom-lambda terraform state files  
3. **Verify Resources**: Ensure all AWS resources are still functioning
4. **Debug Issues**: Investigate terraform import errors and resource mismatches

## Notes

- Always use the appropriate AWS profile for each environment
- The exact resource IDs must be obtained from AWS before running imports
- Some minor configuration drift may occur (tags, descriptions) - this is normal
- API Gateway deployments may need to be recreated if there are integration changes
- Lambda function code will need to be deployed through the new terraform configuration
- Consider running imports during maintenance windows to minimize impact