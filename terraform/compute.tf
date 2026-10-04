data "aws_caller_identity" "current" {}

data "aws_ssm_parameter" "db_username" { name = "/${var.project_name}/prod/db_username" }
data "aws_ssm_parameter" "db_password" { name = "/${var.project_name}/prod/db_password" }
data "aws_ssm_parameter" "jwt_secret" { name = "/${var.project_name}/prod/jwt_secret" }
data "aws_ssm_parameter" "google_client_id" { name = "/${var.project_name}/prod/google_client_id" }
data "aws_ssm_parameter" "judge0_api_key" { name = "/${var.project_name}/prod/judge0_api_key" }
data "aws_ssm_parameter" "lambda_secret" { name = "/${var.project_name}/prod/lambda_secret" }
data "aws_ssm_parameter" "admin_email" { name = "/${var.project_name}/prod/admin_email" }
data "aws_ssm_parameter" "admin_password" { name = "/${var.project_name}/prod/admin_password" }

resource "aws_security_group" "alb_sg" {
  name        = "${var.project_name}-alb-sg"
  description = "Allow inbound HTTP and HTTPS from the internet"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP from Internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS from Internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-alb-sg"
  }
}

resource "aws_security_group" "ecs_sg" {
  name        = "${var.project_name}-ecs-sg"
  description = "Allow inbound traffic ONLY from the ALB"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Allow traffic from ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  ingress {
    description     = "Allow traffic from internal ALB (private Lambda callbacks)"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.internal_alb_sg.id]
  }

  egress {
    description = "Allow all outbound traffic (to talk to DB, Redis, APIs)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-ecs-sg"
  }
}

resource "aws_security_group" "lambda_sg" {
  name        = "${var.project_name}-lambda-sg"
  description = "Security group for the s3-finalization Lambda running inside the VPC"
  vpc_id      = aws_vpc.main.id

  egress {
    description = "Allow all outbound traffic (internal ALB, NAT for other AWS APIs)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-lambda-sg"
  }
}

resource "aws_security_group" "internal_alb_sg" {
  name        = "${var.project_name}-internal-alb-sg"
  description = "Allow inbound traffic to the internal ALB ONLY from the s3-finalization Lambda"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "HTTP from s3-finalization Lambda"
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    security_groups = [aws_security_group.lambda_sg.id]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-internal-alb-sg"
  }
}

resource "aws_security_group" "data_sg" {
  name        = "${var.project_name}-data-sg"
  description = "Allow inbound traffic ONLY from ECS containers"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL from ECS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_sg.id]
  }

  ingress {
    description     = "Redis from ECS"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_sg.id]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-data-sg"
  }
}

resource "aws_sqs_queue" "match_watch_queue" {
  name = "match-watch-queue"
  tags = { Name = "${var.project_name}-match-watch-queue" }
}

resource "aws_sqs_queue" "match_result_queue" {
  name = "match-result-queue"
  tags = { Name = "${var.project_name}-match-result-queue" }
}

resource "aws_sqs_queue" "submission_queue" {
  name = "submission-queue"
  tags = { Name = "${var.project_name}-submission-queue" }
}

resource "aws_s3_bucket" "backend_storage" {
  bucket_prefix = "${var.project_name}-backend-storage-"
  force_destroy = true
  tags = { Name = "${var.project_name}-backend-storage" }
}

resource "aws_s3_bucket" "frontend_hosting" {
  bucket_prefix = "${var.project_name}-frontend-hosting-"
  force_destroy = true
  tags = { Name = "${var.project_name}-frontend-hosting" }
}

resource "aws_s3_bucket_public_access_block" "backend_storage_block" {
  bucket                  = aws_s3_bucket.backend_storage.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_lb" "main" {
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = [aws_subnet.public_1.id, aws_subnet.public_2.id]

  tags = { Name = "${var.project_name}-alb" }
}

resource "aws_lb_target_group" "backend_tg" {
  name        = "${var.project_name}-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    healthy_threshold   = 2
    unhealthy_threshold = 10
    timeout             = 60
    interval            = 300
    matcher             = "200"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend_tg.arn
  }
}

resource "aws_lb" "internal" {
  name               = "${var.project_name}-internal-alb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.internal_alb_sg.id]
  subnets            = [aws_subnet.private_1.id, aws_subnet.private_2.id]

  tags = { Name = "${var.project_name}-internal-alb" }
}

resource "aws_lb_listener" "internal_http" {
  load_balancer_arn = aws_lb.internal.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend_tg.arn
  }
}

resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster"
  tags = { Name = "${var.project_name}-cluster" }
}

resource "aws_cloudwatch_log_group" "backend_logs" {
  name              = "/ecs/${var.project_name}-main-backend"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "sentinel_logs" {
  name              = "/ecs/${var.project_name}-sentinel-service"
  retention_in_days = 7
}

resource "aws_ecs_task_definition" "main_backend" {
  family                   = "${var.project_name}-main-backend-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name      = "main-backend"
      image     = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/codeduels-backend:latest"
      essential = true
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
        }
      ]
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${aws_db_instance.postgres.db_name}" },
        { name = "SPRING_JPA_HIBERNATE_DDL_AUTO", value = "update" },
        { name = "SPRING_DATA_REDIS_HOST", value = aws_elasticache_cluster.redis.cache_nodes[0].address },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" },
        { name = "AWS_SQS_QUEUE_NAME", value = aws_sqs_queue.submission_queue.name },
        { name = "AWS_S3_BUCKET_NAME", value = aws_s3_bucket.backend_storage.id },
        { name = "SPRING_CLOUD_AWS_REGION_STATIC", value = var.aws_region },
        {
          name = "SPRING_APPLICATION_JSON",
          value = jsonencode({
            "app.frontend.url" = "https://${aws_cloudfront_distribution.frontend.domain_name}",
            "aws.s3.test-case-cache-ttl-minutes"               = "30",
            "problem.upload.max-size-kb"                       = "300",
            "permissions.expiry-minutes"                       = "45",
            "scheduler.cleanup.permissions.cron"               = "0 0 4 * * *",
            "scheduler.cleanup.s3-orphans.cron"                = "0 0 5 * * *",
            "judge0.api.url"                                   = "https://judge0-ce.p.rapidapi.com",
            "judge0.api.host"                                  = "judge0-ce.p.rapidapi.com",
            "spring.mail.host"                                 = "localhost",
            "spring.mail.port"                                 = "1025",
            "spring.mail.properties.mail.smtp.auth"            = "false",
            "spring.mail.properties.mail.smtp.starttls.enable" = "false",
            "management.health.mail.enabled"                   = "false",
            "management.endpoint.health.show-details"          = "always"
          })
        }
      ]
      secrets = [
        { name = "SPRING_DATASOURCE_USERNAME", valueFrom = data.aws_ssm_parameter.db_username.arn },
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = data.aws_ssm_parameter.db_password.arn },
        { name = "JWT_SECRET_KEY", valueFrom = data.aws_ssm_parameter.jwt_secret.arn },
        { name = "GOOGLE_CLIENT_ID", valueFrom = data.aws_ssm_parameter.google_client_id.arn },
        { name = "JUDGE0_API_KEY", valueFrom = data.aws_ssm_parameter.judge0_api_key.arn },
        { name = "LAMBDA_INTERNAL_SECRET", valueFrom = data.aws_ssm_parameter.lambda_secret.arn },
        { name = "ADMIN_DEFAULT_EMAIL", valueFrom = data.aws_ssm_parameter.admin_email.arn },
        { name = "ADMIN_DEFAULT_PASSWORD", valueFrom = data.aws_ssm_parameter.admin_password.arn }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.backend_logs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "main_backend" {
  name            = "${var.project_name}-main-backend-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.main_backend.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend_tg.arn
    container_name   = "main-backend"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.http]
}

resource "aws_ecs_task_definition" "sentinel_service" {
  family                   = "${var.project_name}-sentinel-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name      = "sentinel-service"
      image     = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/codeduels-sentinel:latest"
      essential = true
      portMappings = [
        {
          containerPort = 8081
          hostPort      = 8081
        }
      ]
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SPRING_DATA_REDIS_HOST", value = aws_elasticache_cluster.redis.cache_nodes[0].address },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.sentinel_logs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "sentinel_service" {
  name            = "${var.project_name}-sentinel-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.sentinel_service.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = false
  }
}