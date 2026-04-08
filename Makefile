# Variables
PROJECT_NAME := codeduels
ENV ?= prod

.PHONY: up down infra-up secrets

# Only runs Terraform. GitHub Actions handles the rest on 'git push'
up: infra-up
	@echo "🏗️ Infrastructure is synced. Push your code to GitHub to deploy the apps."

# Destroys everything in AWS
down:
	@echo "🔥 Nuking infrastructure in ap-south-1..."
	cd terraform && terraform destroy -var="env=$(ENV)" -auto-approve

# Provision or Update AWS Infrastructure
infra-up:
	@echo "🏗️ Syncing Terraform Infrastructure..."
	cd terraform && terraform init
	cd terraform && terraform apply -var="env=$(ENV)" -auto-approve

# Quick way to run your secure push script
secrets:
	@chmod +x push-secrets.sh
	./push-secrets.sh