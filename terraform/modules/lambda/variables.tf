variable "function_name" {
  description = "Name of the Lambda function"
  type        = string
}

variable "jar_file_path" {
  description = "Path to the JAR file for Lambda deployment"
  type        = string
}

variable "handler" {
  description = "Lambda function handler"
  type        = string
  default     = "com.encom.lambda.MapGeneratorHandler"
}

variable "runtime" {
  description = "Lambda runtime"
  type        = string
  default     = "java17"
}

variable "memory_size" {
  description = "Memory allocated to Lambda function (MB)"
  type        = number
  default     = 512
}

variable "timeout" {
  description = "Lambda function timeout (seconds)"
  type        = number
  default     = 30
}

variable "environment_variables" {
  description = "Environment variables for Lambda function"
  type        = map(string)
  default     = {}
}

variable "log_retention_days" {
  description = "CloudWatch log retention period (days)"
  type        = number
  default     = 14
}

variable "custom_policy_statements" {
  description = "Additional IAM policy statements for Lambda"
  type        = list(any)
  default     = []
}

variable "enable_function_url" {
  description = "Enable Lambda function URL"
  type        = bool
  default     = false
}

variable "alias_name" {
  description = "Lambda alias name"
  type        = string
  default     = "live"
}

variable "enable_api_gateway_integration" {
  description = "Enable API Gateway integration permissions"
  type        = bool
  default     = true
}

variable "api_gateway_execution_arn" {
  description = "API Gateway execution ARN for Lambda permissions"
  type        = string
  default     = ""
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}