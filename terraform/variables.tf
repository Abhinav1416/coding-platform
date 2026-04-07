variable "aws_region" {
  description = "The AWS region to deploy to"
  default     = "ap-south-1"
}

variable "project_name" {
  description = "The name of the project"
  default     = "codeduels"
}

variable "vpc_cidr" {
  description = "The IP address range for the entire VPC"
  default     = "10.0.0.0/16"
}

variable "db_username" {
  description = "Username for the RDS Postgres database"
  default     = "postgresadmin"
}

variable "db_password" {
  description = "Password for the RDS Postgres database"
  default     = "SuperSecretPassword123!"
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT Secret Key"
  sensitive   = true
}

variable "google_client_id" {
  description = "Google OAuth Client ID"
  sensitive   = true
}

variable "judge0_api_key" {
  description = "Judge0 API Key"
  sensitive   = true
}

variable "lambda_secret" {
  description = "Internal Lambda Secret"
  sensitive   = true
}

variable "admin_email" {
  description = "Default Admin Email"
}

variable "admin_password" {
  description = "Default Admin Password"
  sensitive   = true
}