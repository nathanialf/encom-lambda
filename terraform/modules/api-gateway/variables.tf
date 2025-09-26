variable "api_name" {
  description = "Name of the API Gateway"
  type        = string
}

variable "api_description" {
  description = "Description of the API Gateway"
  type        = string
  default     = "ENCOM Map Generation API"
}

variable "stage_name" {
  description = "Stage name for API Gateway"
  type        = string
}

variable "lambda_invoke_arn" {
  description = "Lambda function invoke ARN for integration"
  type        = string
}

variable "enable_api_key" {
  description = "Enable API key authentication"
  type        = bool
  default     = false
}

variable "quota_limit" {
  description = "API quota limit"
  type        = number
  default     = 10000
}

variable "quota_period" {
  description = "API quota period"
  type        = string
  default     = "MONTH"
}

variable "throttle_rate_limit" {
  description = "API throttle rate limit (requests per second)"
  type        = number
  default     = 100
}

variable "throttle_burst_limit" {
  description = "API throttle burst limit"
  type        = number
  default     = 200
}

variable "log_retention_days" {
  description = "CloudWatch log retention period (days)"
  type        = number
  default     = 14
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}