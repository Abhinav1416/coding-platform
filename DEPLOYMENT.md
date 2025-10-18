# Code-Duel: Deployment Architecture

![Code-Duel Architecture Diagram](./assets/4F17645B-B8AD-45B5-8DC4-F5A88A835E20_1_201_a.jpeg)


* Live Site: `https://coding-platform-uyo1.vercel.app`
* GitHub Repo: `https://github.com/Abhinav1416/coding-platform`

## Deployment At-a-Glance

* Frontend: A React (Vite) app deployed to Vercel, leveraging its global CDN and seamless CI/CD.
* Backend: A Spring Boot API packaged in Docker and deployed to AWS EC2.
* Connection: Vercel acts as a reverse proxy, securely routing all `/api` requests to an AWS Application Load Balancer (ALB), which manages traffic to the EC2 instance.
* Automation: The backend has a fully automated CI/CD pipeline using GitHub Actions, which builds the Docker image, pushes it to AWS ECR, and deploys it to EC2 on every `git push`.


## 1. Architecture Overview

This project is engineered as a decoupled, cloud-native application to ensure scalability, security, and automated delivery.
* Frontend: The React application is deployed on Vercel, which provides high-performance static asset delivery via its global CDN and integrated CI/CD.
* Backend: The Spring Boot API is containerized with Docker and hosted on AWS.
* Communication: To avoid CORS and abstract the backend, the Vercel project uses proxy rewrites. All API calls (e.g., `/api/...`) are routed server-side to the AWS backend, providing a unified and secure interface for the client.

## 2. Backend Infrastructure (AWS)

The backend is designed for resilience and security using a core set of AWS services.
* Compute & Load Balancing: An Application Load Balancer (ALB) serves as the single, public entry point. It routes traffic to the Spring Boot Docker container running on an AWS EC2 instance.
* Network Security: The architecture is secured via Security Groups. The ALB is open to public web traffic, but the EC2 instance is completely private, accepting traffic only from the ALB. This isolates the application server from the public internet.
* Container Registry: AWS ECR (Elastic Container Registry) is used to privately and securely store the backend's Docker images.

## 3. CI/CD & Automation (GitHub Actions)

The entire backend deployment is fully automated using a GitHub Actions workflow triggered by a push to the `main`branch.
1. Build: The workflow builds the Spring Boot application using a multi-stage Dockerfile. This critical step ensures the final image is small, secure, and contains no build tools or source code.
2. Push: The new image is tagged and pushed to the private AWS ECR repository.
3. Deploy: The workflow securely SSHs into the EC2 instance, pulls the new image from ECR, and redeploys the container, enabling a seamless update.

## 4. Secure Configuration Management

A two-tiered strategy is used to manage secrets, ensuring no sensitive data is ever committed to Git.
* CI/CD Secrets: Credentials for the pipeline itself (e.g., `AWS_ACCESS_KEY_ID`, `EC2_SSH_KEY`) are stored as GitHub Actions Secrets.
* Application Secrets: Credentials for the application at runtime (e.g., database passwords, JWT keys) are stored in a `.env` file on the EC2 server. This file is securely injected into the container during the `docker run` command.
