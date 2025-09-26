output "api_id" {
  description = "API Gateway REST API ID"
  value       = aws_api_gateway_rest_api.api.id
}

output "api_arn" {
  description = "API Gateway REST API ARN"
  value       = aws_api_gateway_rest_api.api.arn
}

output "api_execution_arn" {
  description = "API Gateway execution ARN"
  value       = aws_api_gateway_rest_api.api.execution_arn
}

output "api_root_resource_id" {
  description = "API Gateway root resource ID"
  value       = aws_api_gateway_rest_api.api.root_resource_id
}

output "stage_name" {
  description = "API Gateway stage name"
  value       = aws_api_gateway_stage.stage.stage_name
}

output "stage_arn" {
  description = "API Gateway stage ARN"
  value       = aws_api_gateway_stage.stage.arn
}

output "invoke_url" {
  description = "API Gateway invoke URL"
  value       = "https://${aws_api_gateway_rest_api.api.id}.execute-api.${data.aws_region.current.name}.amazonaws.com/${aws_api_gateway_stage.stage.stage_name}"
}

output "api_key_id" {
  description = "API Key ID (if enabled)"
  value       = var.enable_api_key ? aws_api_gateway_api_key.api_key[0].id : null
}

output "api_key_value" {
  description = "API Key value (if enabled)"
  value       = var.enable_api_key ? aws_api_gateway_api_key.api_key[0].value : null
  sensitive   = true
}

output "usage_plan_id" {
  description = "Usage plan ID (if enabled)"
  value       = var.enable_api_key ? aws_api_gateway_usage_plan.usage_plan[0].id : null
}

output "log_group_name" {
  description = "CloudWatch log group name for API Gateway"
  value       = aws_cloudwatch_log_group.api_gateway_logs.name
}