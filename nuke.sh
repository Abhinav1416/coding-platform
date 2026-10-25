#!/bin/bash

echo "💀 [1/2] Assassinating the Network Load Balancer..."
kubectl delete svc ingress-nginx-controller -n ingress-nginx --ignore-not-found=true

echo "⏳ Waiting 30 seconds for AWS to detach the NLB..."
sleep 30

echo "🔥 [2/2] Burning infrastructure to the ground..."
cd terraform
terraform destroy -auto-approve

echo "✅ AWS is completely clean. No money wasted."