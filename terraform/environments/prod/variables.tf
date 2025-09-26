variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-west-1"
}

# Lambda Configuration
variable "lambda_handler" {
  description = "Lambda function handler"
  type        = string
  default     = "com.encom.mapgen.handler.MapGeneratorHandler::handleRequest"
}

variable "lambda_runtime" {
  description = "Lambda runtime"
  type        = string
  default     = "java17"
}

variable "lambda_memory_size" {
  description = "Lambda memory size in MB"
  type        = number
  default     = 1024
}

variable "lambda_timeout" {
  description = "Lambda timeout in seconds"
  type        = number
  default     = 60
}

variable "lambda_environment_variables" {
  description = "Environment variables for Lambda"
  type        = map(string)
  default = {
    ENV = "prod"
  }
}

# API Gateway Configuration
variable "enable_api_key" {
  description = "Enable API key authentication"
  type        = bool
  default     = true
}

variable "api_quota_limit" {
  description = "API quota limit per period"
  type        = number
  default     = 10000
}

variable "api_quota_period" {
  description = "API quota period"
  type        = string
  default     = "MONTH"
}

variable "api_throttle_rate_limit" {
  description = "API throttle rate limit (requests per second)"
  type        = number
  default     = 100
}

variable "api_throttle_burst_limit" {
  description = "API throttle burst limit"
  type        = number
  default     = 200
}

# Logging Configuration
variable "log_retention_days" {
  description = "CloudWatch log retention period (days)"
  type        = number
  default     = 30
}