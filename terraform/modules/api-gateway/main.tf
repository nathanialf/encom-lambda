terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# API Gateway REST API
resource "aws_api_gateway_rest_api" "api" {
  name        = var.api_name
  description = var.api_description
  
  endpoint_configuration {
    types = ["REGIONAL"]
  }
  
  tags = var.tags
}

# API Gateway API Key
resource "aws_api_gateway_api_key" "api_key" {
  count = var.enable_api_key ? 1 : 0
  name  = "${var.api_name}-key"
  
  tags = var.tags
}

# Usage Plan
resource "aws_api_gateway_usage_plan" "usage_plan" {
  count = var.enable_api_key ? 1 : 0
  name  = "${var.api_name}-usage-plan"
  
  api_stages {
    api_id = aws_api_gateway_rest_api.api.id
    stage  = aws_api_gateway_stage.stage.stage_name
  }
  
  quota_settings {
    limit  = var.quota_limit
    period = var.quota_period
  }
  
  throttle_settings {
    rate_limit  = var.throttle_rate_limit
    burst_limit = var.throttle_burst_limit
  }
  
  tags = var.tags
}

# Usage Plan Key
resource "aws_api_gateway_usage_plan_key" "usage_plan_key" {
  count         = var.enable_api_key ? 1 : 0
  key_id        = aws_api_gateway_api_key.api_key[0].id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.usage_plan[0].id
}

# API Gateway Resource - /api
resource "aws_api_gateway_resource" "api_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "api"
}

# API Gateway Resource - /api/v1
resource "aws_api_gateway_resource" "v1_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.api_resource.id
  path_part   = "v1"
}

# API Gateway Resource - /api/v1/map
resource "aws_api_gateway_resource" "map_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.v1_resource.id
  path_part   = "map"
}

# API Gateway Resource - /api/v1/map/generate
resource "aws_api_gateway_resource" "generate_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.map_resource.id
  path_part   = "generate"
}

# OPTIONS method for CORS preflight
resource "aws_api_gateway_method" "options_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.generate_resource.id
  http_method   = "OPTIONS"
  authorization = "NONE"
}

# OPTIONS method integration
resource "aws_api_gateway_integration" "options_integration" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.generate_resource.id
  http_method = aws_api_gateway_method.options_method.http_method
  type        = "MOCK"
  
  request_templates = {
    "application/json" = "{\"statusCode\": 200}"
  }
}

# OPTIONS method response
resource "aws_api_gateway_method_response" "options_response" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.generate_resource.id
  http_method = aws_api_gateway_method.options_method.http_method
  status_code = "200"
  
  response_parameters = {
    "method.response.header.Access-Control-Allow-Headers" = true
    "method.response.header.Access-Control-Allow-Methods" = true
    "method.response.header.Access-Control-Allow-Origin"  = true
  }
}

# OPTIONS integration response
resource "aws_api_gateway_integration_response" "options_integration_response" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.generate_resource.id
  http_method = aws_api_gateway_method.options_method.http_method
  status_code = aws_api_gateway_method_response.options_response.status_code
  
  response_parameters = {
    "method.response.header.Access-Control-Allow-Headers" = "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token,x-api-key'"
    "method.response.header.Access-Control-Allow-Methods" = "'POST,OPTIONS'"
    "method.response.header.Access-Control-Allow-Origin"  = "'*'"
  }
}

# POST method for map generation
resource "aws_api_gateway_method" "post_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.generate_resource.id
  http_method   = "POST"
  authorization = var.enable_api_key ? "NONE" : "NONE"
  api_key_required = var.enable_api_key
}

# POST method integration with Lambda
resource "aws_api_gateway_integration" "lambda_integration" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.generate_resource.id
  http_method = aws_api_gateway_method.post_method.http_method
  
  integration_http_method = "POST"
  type                   = "AWS_PROXY"
  uri                    = var.lambda_invoke_arn
}

# POST method response
resource "aws_api_gateway_method_response" "post_response_200" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.generate_resource.id
  http_method = aws_api_gateway_method.post_method.http_method
  status_code = "200"
  
  response_parameters = {
    "method.response.header.Access-Control-Allow-Origin" = true
  }
}

# API Gateway Deployment
resource "aws_api_gateway_deployment" "deployment" {
  depends_on = [
    aws_api_gateway_method.options_method,
    aws_api_gateway_method.post_method,
    aws_api_gateway_integration.options_integration,
    aws_api_gateway_integration.lambda_integration,
    aws_api_gateway_integration_response.options_integration_response
  ]
  
  rest_api_id = aws_api_gateway_rest_api.api.id
  
  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.generate_resource.id,
      aws_api_gateway_method.options_method.id,
      aws_api_gateway_method.post_method.id,
      aws_api_gateway_integration.options_integration.id,
      aws_api_gateway_integration.lambda_integration.id,
    ]))
  }
  
  lifecycle {
    create_before_destroy = true
  }
}

# API Gateway Stage
resource "aws_api_gateway_stage" "stage" {
  deployment_id = aws_api_gateway_deployment.deployment.id
  rest_api_id   = aws_api_gateway_rest_api.api.id
  stage_name    = var.stage_name
  
  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway_logs.arn
    format = jsonencode({
      requestId      = "$context.requestId"
      ip             = "$context.identity.sourceIp"
      caller         = "$context.identity.caller"
      user           = "$context.identity.user"
      requestTime    = "$context.requestTime"
      httpMethod     = "$context.httpMethod"
      resourcePath   = "$context.resourcePath"
      status         = "$context.status"
      protocol       = "$context.protocol"
      responseLength = "$context.responseLength"
      error          = "$context.error.message"
      integrationError = "$context.integrationErrorMessage"
    })
  }
  
  tags = var.tags
}

# CloudWatch Log Group for API Gateway
resource "aws_cloudwatch_log_group" "api_gateway_logs" {
  name              = "/aws/apigateway/${var.api_name}"
  retention_in_days = var.log_retention_days
  
  tags = var.tags
}

# Data source for current AWS region
data "aws_region" "current" {}