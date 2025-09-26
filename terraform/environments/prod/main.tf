terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "prod-encom-lambda-terraform-state"
    key    = "encom-lambda/prod/terraform.tfstate"
    region = "us-west-1"
  }
}

# Configure AWS Provider
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "encom-lambda"
      Environment = "prod"
      ManagedBy   = "terraform"
    }
  }
}

# Data sources
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# Local values for configuration
locals {
  project_name = "encom"
  environment  = "prod"
  
  # Lambda configuration
  lambda_function_name = "${local.project_name}-map-generator-${local.environment}"
  lambda_jar_path      = "../../../build/libs/encom-lambda-1.0.0-all.jar"
  
  # API Gateway configuration  
  api_name = "${local.project_name}-api-${local.environment}"
  
  common_tags = {
    Project     = local.project_name
    Environment = local.environment
    Region      = data.aws_region.current.name
    Account     = data.aws_caller_identity.current.account_id
  }
}

# Lambda Module
module "lambda" {
  source = "../../modules/lambda"
  
  function_name    = local.lambda_function_name
  jar_file_path    = local.lambda_jar_path
  handler          = var.lambda_handler
  runtime          = var.lambda_runtime
  memory_size      = var.lambda_memory_size
  timeout          = var.lambda_timeout
  log_retention_days = var.log_retention_days
  
  environment_variables = var.lambda_environment_variables
  
  enable_api_gateway_integration = true
  api_gateway_execution_arn      = module.api_gateway.api_execution_arn
  
  tags = local.common_tags
}

# API Gateway Module
module "api_gateway" {
  source = "../../modules/api-gateway"
  
  api_name          = local.api_name
  api_description   = "ENCOM Hexagonal Map Generation API - Production"
  stage_name        = local.environment
  lambda_invoke_arn = module.lambda.alias_invoke_arn
  
  enable_api_key        = var.enable_api_key
  quota_limit          = var.api_quota_limit
  quota_period         = var.api_quota_period
  throttle_rate_limit  = var.api_throttle_rate_limit
  throttle_burst_limit = var.api_throttle_burst_limit
  log_retention_days   = var.log_retention_days
  
  tags = local.common_tags
}