data "aws_ssm_parameter" "rds_db_username" {
  name            = "/${var.project_name}/prod/db_username"
  with_decryption = true
}

data "aws_ssm_parameter" "rds_db_password" {
  name            = "/${var.project_name}/prod/db_password"
  with_decryption = true
}

resource "aws_db_subnet_group" "default" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = [aws_subnet.private_1.id, aws_subnet.private_2.id]

  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }
}

resource "aws_elasticache_subnet_group" "default" {
  name       = "${var.project_name}-redis-subnet-group"
  subnet_ids = [aws_subnet.private_1.id, aws_subnet.private_2.id]
}

resource "aws_db_instance" "postgres" {
  identifier            = "${var.project_name}-postgres"
  engine                = "postgres"
  engine_version        = "16"
  instance_class        = "db.t3.micro"
  allocated_storage     = 20
  max_allocated_storage = 100

  db_name  = "codeduelsdb"
  username = data.aws_ssm_parameter.rds_db_username.value
  password = data.aws_ssm_parameter.rds_db_password.value

  db_subnet_group_name   = aws_db_subnet_group.default.name
  vpc_security_group_ids = [aws_security_group.data_sg.id]

  skip_final_snapshot = true
  publicly_accessible = false

  tags = {
    Name = "${var.project_name}-postgres"
  }
}

resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "${var.project_name}-redis"
  engine               = "redis"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  engine_version       = "7.0"
  port                 = 6379

  subnet_group_name  = aws_elasticache_subnet_group.default.name
  security_group_ids = [aws_security_group.data_sg.id]
}

resource "aws_security_group" "data_sg" {
  name        = "${var.project_name}-data-sg"
  description = "Allow inbound traffic ONLY from EKS Nodes"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL from EKS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  ingress {
    description     = "Redis from EKS"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
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