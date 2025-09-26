# Production Environment Configuration
aws_region = "us-west-1"

# Lambda Configuration - Higher resources for production
lambda_memory_size = 1024
lambda_timeout     = 60

# API Gateway Configuration - Production (API key required)
enable_api_key             = true
api_quota_limit           = 10000
api_throttle_rate_limit   = 100
api_throttle_burst_limit  = 200

# Logging Configuration - Longer retention for prod
log_retention_days = 30