#!/bin/bash
set -e

echo "🔥 [1/4] Building AWS EKS Infrastructure..."
cd terraform
terraform apply -auto-approve
cd ..

echo "🔌 [2/4] Connecting kubectl to the new cluster..."
aws eks update-kubeconfig --region ap-south-1 --name codeduels-cluster

echo "🚀 [3/4] Installing NGINX Ingress Controller..."
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx || true
helm repo update
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.service.type=LoadBalancer \
  --set controller.service.annotations."service\.beta\.kubernetes\.io/aws-load-balancer-type"="nlb"

echo "🐙 [4/4] Installing ArgoCD..."
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml --server-side --force-conflicts

echo "✅ ALL SYSTEMS GO. Cluster is UP and READY."