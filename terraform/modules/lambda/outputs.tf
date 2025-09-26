output "function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.function.function_name
}

output "function_arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.function.arn
}

output "function_invoke_arn" {
  description = "Invoke ARN of the Lambda function"
  value       = aws_lambda_function.function.invoke_arn
}

output "function_qualified_arn" {
  description = "Qualified ARN of the Lambda function"
  value       = aws_lambda_function.function.qualified_arn
}

output "function_version" {
  description = "Version of the Lambda function"
  value       = aws_lambda_function.function.version
}

output "function_url" {
  description = "Lambda function URL (if enabled)"
  value       = var.enable_function_url ? aws_lambda_function_url.function_url[0].function_url : null
}

output "alias_name" {
  description = "Lambda alias name"
  value       = aws_lambda_alias.function_alias.name
}

output "alias_arn" {
  description = "Lambda alias ARN"
  value       = aws_lambda_alias.function_alias.arn
}

output "alias_invoke_arn" {
  description = "Lambda alias invoke ARN"
  value       = aws_lambda_alias.function_alias.invoke_arn
}

output "log_group_name" {
  description = "CloudWatch log group name"
  value       = aws_cloudwatch_log_group.lambda_logs.name
}

output "role_arn" {
  description = "IAM role ARN for Lambda"
  value       = aws_iam_role.lambda_role.arn
}

output "role_name" {
  description = "IAM role name for Lambda"
  value       = aws_iam_role.lambda_role.name
}