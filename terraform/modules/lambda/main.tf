terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Create IAM role for Lambda execution
resource "aws_iam_role" "lambda_role" {
  name                  = "${var.function_name}-role"
  force_detach_policies = true
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
  
  tags = var.tags
  
  lifecycle {
    create_before_destroy = true
  }
}

# Attach basic Lambda execution policy
resource "aws_iam_role_policy_attachment" "lambda_basic_execution" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# Custom policy for additional permissions if needed
resource "aws_iam_role_policy" "lambda_custom_policy" {
  count = length(var.custom_policy_statements) > 0 ? 1 : 0
  name  = "${var.function_name}-custom-policy"
  role  = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = var.custom_policy_statements
  })
}

# CloudWatch Log Group for Lambda
resource "aws_cloudwatch_log_group" "lambda_logs" {
  name              = "/aws/lambda/${var.function_name}"
  retention_in_days = var.log_retention_days
  skip_destroy      = false
  
  tags = var.tags
  
  lifecycle {
    create_before_destroy = true
  }
}

# Lambda function
resource "aws_lambda_function" "function" {
  filename         = var.jar_file_path
  function_name    = var.function_name
  role            = aws_iam_role.lambda_role.arn
  handler         = var.handler
  runtime         = var.runtime
  timeout         = var.timeout
  memory_size     = var.memory_size
  
  source_code_hash = filebase64sha256(var.jar_file_path)
  
  environment {
    variables = var.environment_variables
  }
  
  depends_on = [
    aws_iam_role_policy_attachment.lambda_basic_execution,
    aws_cloudwatch_log_group.lambda_logs
  ]
  
  tags = var.tags
}

# Lambda function URL (optional - for direct HTTP access)
resource "aws_lambda_function_url" "function_url" {
  count              = var.enable_function_url ? 1 : 0
  function_name      = aws_lambda_function.function.function_name
  authorization_type = "NONE"  # Use "AWS_IAM" for authenticated access
  
  cors {
    allow_credentials = false
    allow_origins     = ["*"]
    allow_methods     = ["POST", "GET", "OPTIONS"]
    allow_headers     = ["date", "keep-alive", "content-type", "x-api-key"]
    expose_headers    = ["date", "keep-alive"]
    max_age          = 86400
  }
}

# Lambda alias for versioning
resource "aws_lambda_alias" "function_alias" {
  name             = var.alias_name
  description      = "Alias for ${var.function_name}"
  function_name    = aws_lambda_function.function.function_name
  function_version = "$LATEST"
  
  depends_on = [aws_lambda_function.function]
}

# Lambda permissions for API Gateway (if used)
resource "aws_lambda_permission" "api_gateway_invoke" {
  count         = var.enable_api_gateway_integration ? 1 : 0
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.function.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${var.api_gateway_execution_arn}/*/*"
  qualifier     = aws_lambda_alias.function_alias.name
}