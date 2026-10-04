data "aws_ssm_parameter" "lambda_secret_decrypted" {
  name            = "/${var.project_name}/prod/lambda_secret"
  with_decryption = true
}

resource "aws_iam_role" "lambda_exec_role" {
  name = "${var.project_name}-lambda-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic_execution" {
  role       = aws_iam_role.lambda_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "lambda_vpc_access" {
  role       = aws_iam_role.lambda_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

resource "aws_lambda_function" "s3_finalization_lambda" {
  filename         = "lambda_function.jar"
  function_name    = "${var.project_name}-s3-finalization"
  role             = aws_iam_role.lambda_exec_role.arn
  handler          = "com.yourlambda.handler.S3FinalizationHandler::handleRequest"
  runtime          = "java21"
  timeout          = 30
  memory_size      = 512

  source_code_hash = filebase64sha256("lambda_function.jar")

  vpc_config {
    subnet_ids         = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_group_ids = [aws_security_group.lambda_sg.id]
  }

  environment {
    variables = {
      BACKEND_API_ENDPOINT = "http://${aws_lb.internal.dns_name}"
      INTERNAL_API_SECRET  = data.aws_ssm_parameter.lambda_secret_decrypted.value
    }
  }

  depends_on = [
    aws_iam_role_policy_attachment.lambda_basic_execution,
    aws_iam_role_policy_attachment.lambda_vpc_access,
  ]
}

resource "aws_lambda_permission" "allow_s3_to_call_lambda" {
  statement_id  = "AllowExecutionFromS3Bucket"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.s3_finalization_lambda.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.backend_storage.arn
}

resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = aws_s3_bucket.backend_storage.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.s3_finalization_lambda.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "uploads/pending/"
    filter_suffix       = ".zip"
  }

  depends_on = [aws_lambda_permission.allow_s3_to_call_lambda]
}