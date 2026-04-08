PROJECT_NAME := codeduels
ENV ?= prod
BRANCH := $(shell git rev-parse --abbrev-ref HEAD)

.PHONY: up down infra-up secrets

up: infra-up
	@echo "📡 Triggering GitHub Actions on branch: $(BRANCH)..."
	git commit --allow-empty -m "Triggering redeploy for fresh infrastructure"
	git push origin $(BRANCH)
	@echo "🚀 CodeDuels is starting up. Check your GitHub Actions tab!"

down:
	@echo "🔥 Nuking everything in ap-south-1..."
	cd terraform && terraform destroy -var="env=$(ENV)" -auto-approve

infra-up:
	@echo "🏗️ Syncing Terraform Infrastructure..."
	cd terraform && terraform init
	cd terraform && terraform apply -var="env=$(ENV)" -auto-approve

secrets:
	@chmod +x push-secrets.sh
	./push-secrets.sh