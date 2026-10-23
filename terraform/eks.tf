module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.8.4"

  cluster_name    = "${var.project_name}-cluster"
  cluster_version = "1.32"

  cluster_endpoint_public_access  = true

  vpc_id                   = aws_vpc.main.id
  subnet_ids               = [aws_subnet.private_1.id, aws_subnet.private_2.id]
  control_plane_subnet_ids = [aws_subnet.private_1.id, aws_subnet.private_2.id, aws_subnet.public_1.id, aws_subnet.public_2.id]

  cluster_addons = {
    coredns = {
      most_recent = true
    }
    kube-proxy = {
      most_recent = true
    }
    vpc-cni = {
      most_recent    = true
      before_compute = true
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
      min_size     = 2
      max_size     = 4
      desired_size = 3

      instance_types = ["t3.micro"]
      capacity_type  = "ON_DEMAND"
      ami_type       = "AL2_x86_64"
    }
  }

  enable_cluster_creator_admin_permissions = true

  tags = {
    Name = "${var.project_name}-eks"
  }
}