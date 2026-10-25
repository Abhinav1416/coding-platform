module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.8.4"

  cluster_name    = "${var.project_name}-cluster"
  cluster_version = "1.32"

  cluster_endpoint_public_access = true

  vpc_id                   = aws_vpc.main.id
  subnet_ids               = [aws_subnet.private_1.id, aws_subnet.private_2.id]
  control_plane_subnet_ids = [aws_subnet.private_1.id, aws_subnet.private_2.id, aws_subnet.public_1.id, aws_subnet.public_2.id]

  cluster_addons = {
    coredns = {
      most_recent = true
      resolve_conflicts_on_update = "OVERWRITE"
    }
    kube-proxy = {
      most_recent = true
      resolve_conflicts_on_update = "OVERWRITE"
    }
    vpc-cni = {
      most_recent = true
      before_compute = true
      resolve_conflicts_on_update = "OVERWRITE"
      configuration_values = jsonencode({
        env = {
          ENABLE_PREFIX_DELEGATION = "true"
          WARM_PREFIX_TARGET       = "1"
        }
      })
    }
  }

  enable_irsa = true

  eks_managed_node_groups = {
    codeduels_nodes = {
      min_size     = 1
      max_size     = 4
      desired_size = 3

      instance_types = ["t3.micro"]
      capacity_type  = "SPOT"
      ami_type       = "AL2_x86_64"

      labels = {
        lifecycle = "Spot"
      }
    }
  }

  enable_cluster_creator_admin_permissions = true

  tags = {
    Name = "${var.project_name}-eks"
  }
}

# 1. The IAM Policy that allows KEDA to read the SQS queue
resource "aws_iam_policy" "keda_sqs_policy" {
  name        = "codeduels-keda-sqs-policy"
  description = "Allow KEDA to read SQS queue depth"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl"
        ]
        # Make sure this matches the actual resource name of your queue in Terraform!
        Resource = aws_sqs_queue.submission_queue.arn
      }
    ]
  })
}

# 2. The Trust Policy allowing KEDA's ServiceAccount to assume the role via OIDC
data "aws_iam_policy_document" "keda_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"
    principals {
      type        = "Federated"
      identifiers = [module.eks.oidc_provider_arn]
    }
    condition {
      test     = "StringEquals"
      variable = "${replace(module.eks.cluster_oidc_issuer_url, "https://", "")}:sub"
      values   = ["system:serviceaccount:keda:keda-operator"]
    }
  }
}

# 3. The actual IAM Role
resource "aws_iam_role" "keda_sqs_role" {
  name               = "codeduels-keda-role"
  assume_role_policy = data.aws_iam_policy_document.keda_assume_role_policy.json
}

# 4. Attach the policy to the role
resource "aws_iam_role_policy_attachment" "keda_sqs_attach" {
  role       = aws_iam_role.keda_sqs_role.name
  policy_arn = aws_iam_policy.keda_sqs_policy.arn
}

# 5. Output the Role ARN so we can easily copy it
output "keda_iam_role_arn" {
  value       = aws_iam_role.keda_sqs_role.arn
  description = "The IAM Role ARN to annotate the KEDA service account with"
}

# The Trust Policy allowing your GitHub repository to assume this role
data "aws_iam_policy_document" "github_actions_terraform_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      # IMPORTANT: Replace 'Abhinav1416' with your actual GitHub username if different!
      values = ["repo:Abhinav1416/coding-platform:*"]
    }
  }
}

# The IAM Role for GitHub Actions
resource "aws_iam_role" "github_actions_terraform_role" {
  name               = "codeduels-github-actions-terraform"
  assume_role_policy = data.aws_iam_policy_document.github_actions_terraform_assume_role.json
}

# Grant AdministratorAccess so Terraform can manage VPCs, EKS, IAM, etc.
# (In production we'd scope this down, but for a personal CI/CD pipeline, this prevents nightmare permission errors)
resource "aws_iam_role_policy_attachment" "github_actions_terraform_admin" {
  role       = aws_iam_role.github_actions_terraform_role.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}

output "github_actions_terraform_role_arn" {
  value = aws_iam_role.github_actions_terraform_role.arn
}