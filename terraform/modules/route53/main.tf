terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Hosted Zone for API subdomain
resource "aws_route53_zone" "api_zone" {
  name = var.domain_name
  
  tags = var.tags
}