output "lambda_function_name" {
  description = "Name of the Lambda function"
  value       = module.lambda.function_name
}

output "lambda_function_arn" {
  description = "ARN of the Lambda function"
  value       = module.lambda.function_arn
}

output "api_gateway_id" {
  description = "API Gateway REST API ID"
  value       = module.api_gateway.api_id
}

output "api_gateway_invoke_url" {
  description = "API Gateway invoke URL"
  value       = module.api_gateway.invoke_url
}

output "api_gateway_endpoint" {
  description = "Full API endpoint URL for map generation"
  value       = "${module.api_gateway.invoke_url}/api/v1/map/generate"
}

output "api_key_id" {
  description = "API Key ID (if enabled)"
  value       = module.api_gateway.api_key_id
}

output "lambda_log_group" {
  description = "Lambda CloudWatch log group name"
  value       = module.lambda.log_group_name
}

output "api_gateway_log_group" {
  description = "API Gateway CloudWatch log group name"
  value       = module.api_gateway.log_group_name
}

output "custom_domain_name" {
  description = "Custom domain name for the API"
  value       = module.api_gateway.custom_domain_name
}

output "custom_domain_endpoint" {
  description = "Custom domain API endpoint URL for map generation"
  value       = module.api_gateway.custom_domain_name != null ? "https://${module.api_gateway.custom_domain_name}/api/v1/map/generate" : null
}

output "hosted_zone_id" {
  description = "Route53 hosted zone ID"
  value       = module.route53.zone_id
}

output "name_servers" {
  description = "Name servers for the hosted zone"
  value       = module.route53.name_servers
}

output "certificate_arn" {
  description = "ACM certificate ARN"
  value       = module.api_gateway.certificate_arn
}