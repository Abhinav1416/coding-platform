resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/${var.project_name}/prod/jwt_secret"
  type  = "SecureString"
  value = var.jwt_secret
  overwrite = true
}

resource "aws_ssm_parameter" "google_client_id" {
  name  = "/${var.project_name}/prod/google_client_id"
  type  = "SecureString"
  value = var.google_client_id
  overwrite = true
}

resource "aws_ssm_parameter" "judge0_api_key" {
  name  = "/${var.project_name}/prod/judge0_api_key"
  type  = "SecureString"
  value = var.judge0_api_key
  overwrite = true
}

resource "aws_ssm_parameter" "lambda_secret" {
  name  = "/${var.project_name}/prod/lambda_secret"
  type  = "SecureString"
  value = var.lambda_secret
  overwrite = true
}

resource "aws_ssm_parameter" "admin_email" {
  name  = "/${var.project_name}/prod/admin_email"
  type  = "SecureString"
  value = var.admin_email
  overwrite = true
}

resource "aws_ssm_parameter" "admin_password" {
  name  = "/${var.project_name}/prod/admin_password"
  type  = "SecureString"
  value = var.admin_password
  overwrite = true
}