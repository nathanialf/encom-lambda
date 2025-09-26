# Development Environment Configuration
aws_region = "us-west-1"

# Lambda Configuration
lambda_memory_size = 512
lambda_timeout     = 30

# API Gateway Configuration - Development (no API key required)
enable_api_key             = false
api_quota_limit           = 5000
api_throttle_rate_limit   = 50
api_throttle_burst_limit  = 100

# Logging Configuration - Shorter retention for dev
log_retention_days = 7