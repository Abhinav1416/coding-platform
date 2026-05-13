# CodeDuels: Real-Time 1v1 Competitive Programming Platform

CodeDuels is a full-stack, real-time competitive programming platform where users compete in one-on-one DSA battles with live matchmaking, code execution, real-time result updates, and performance tracking.

The platform has served **2,000+ registered users** and evolved from a manually deployed AWS application into a **Terraform-managed, ECS-based, microservice-oriented cloud architecture** using AWS SQS, ElastiCache Redis, RDS PostgreSQL, CloudFront, S3, Lambda, GitHub Actions, and AWS ECS Fargate.

> Built to explore real-world backend engineering concepts such as asynchronous processing, real-time communication, cloud deployment, infrastructure as code, CI/CD, authentication, authorization, and distributed service communication.

---

## Deployment Status

> **Note:** The live deployment is currently paused to control AWS costs. The infrastructure can be spun up on demand via Terraform.

**Repository:** [https://github.com/Abhinav1416/coding-platform/](https://github.com/Abhinav1416/coding-platform/)

---

## Key Highlights

- Served **2,000+ registered users** on a live competitive programming platform
- Built real-time 1v1 coding battles with matchmaking, live result updates, and WebSocket-based communication
- Migrated from manual AWS Console deployment to Terraform-managed Infrastructure as Code
- Deployed frontend using AWS S3 + Amazon CloudFront with secure static hosting
- Deployed backend services on AWS ECS Fargate behind an Application Load Balancer
- Introduced a dedicated Sentinel microservice for asynchronous code execution
- Decoupled submission processing using AWS SQS and Redis Pub/Sub
- Used AWS RDS PostgreSQL for durable data storage and ElastiCache Redis for real-time state and messaging
- Added separate CI/CD pipelines for frontend, main backend, and Sentinel service
- Added backend unit and integration tests using JUnit 5, Mockito, and Spring Boot Test
- Designed a cost-aware deployment model where backend infrastructure can be paused when not actively demoed

---

## Screenshots

### Home Page

![Home Page](./assets/screenshots/HomePage.png)

### Normal Match Page

![Normal Match Page](./assets/screenshots/NormalMatchPage.png)

### Codeforces Match Page

![Codeforces Match Page](./assets/screenshots/CodeforcesMatchPage.png)

---

## System Evolution

CodeDuels was initially built and deployed using a manual AWS Console-based setup with an EC2-hosted backend.

After validating the platform with real users, the system was upgraded into a more production-oriented architecture. The Phase 2 migration introduced:

- Terraform-based Infrastructure as Code
- ECS Fargate-based backend deployment
- CloudFront and S3-based frontend hosting
- AWS-managed RDS PostgreSQL and ElastiCache Redis
- A dedicated Sentinel microservice for asynchronous code execution
- Independent CI/CD pipelines for frontend, main backend, and Sentinel service
- AWS SSM Parameter Store for runtime secrets
- A cost-aware deployment approach to control AWS expenses during non-demo periods

This migration improved deployment repeatability, security boundaries, service separation, and long-term maintainability.

---

## Architecture Overview

![Architecture Diagram](./assets/screenshots/Architecture.png)

### High-Level Flow

```text
User
  ↓
Amazon CloudFront
  ↓
S3 Frontend Hosting

CloudFront /api and /ws traffic
  ↓
Application Load Balancer
  ↓
Main Backend - ECS Fargate
  ↓
RDS PostgreSQL
  ↓
ElastiCache Redis

Main Backend
  ↓
SQS Submission Queue
  ↓
Sentinel Service - ECS Fargate
  ↓
Judge0 API
  ↓
Redis Pub/Sub
  ↓
Main Backend
  ↓
WebSocket Broadcast to Users
```

The frontend is served through CloudFront and S3. API and WebSocket traffic is routed through CloudFront to the Application Load Balancer, which forwards requests to the main backend running on ECS Fargate.

Long-running code execution is not handled directly inside the user-facing backend. Instead, submissions are pushed to AWS SQS and processed asynchronously by the Sentinel service. Once execution completes, Sentinel publishes the result to Redis Pub/Sub, and the main backend broadcasts the result to both users over WebSockets.

---

## Microservices

### 1. Main Backend

The `main-backend` service is built with:

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Spring WebSockets
- STOMP over WebSocket

#### Responsibilities

- Handles REST API requests
- Manages Google OAuth and JWT authentication
- Coordinates matchmaking and match lifecycle
- Maintains STOMP/WebSocket connections for real-time updates
- Sends submission jobs to AWS SQS
- Subscribes to Redis Pub/Sub topics
- Broadcasts execution results to competing users
- Manages user stats, match state, and problem-related workflows

---

### 2. Sentinel Service

The `sentinel-service` is a dedicated Spring Boot background worker responsible for asynchronous code execution.

The service is built with:

- Java 21
- Spring Boot 3
- Spring Cloud AWS
- AWS SQS
- Redis Pub/Sub
- Judge0 API

#### Responsibilities

- Continuously polls the AWS SQS submission queue
- Parses and validates submission jobs
- Calls the external Judge0 API for code execution
- Processes execution results such as:
    - Accepted
    - Wrong Answer
    - Time Limit Exceeded
    - Runtime Error
    - Compilation Error
- Publishes final execution results to Redis Pub/Sub topics

This separation keeps long-running code execution outside the main backend and allows the user-facing API service to remain responsive during submission spikes.

---

### 3. Frontend

The `codeduels-frontend` is built with:

- React
- Vite
- TypeScript
- Tailwind CSS
- Monaco Editor
- StompJS/SockJS

#### Responsibilities

- User authentication flow
- Matchmaking interface
- Real-time duel experience
- Code editor interface
- Live submission result updates
- Profile and stats display

The frontend is built through GitHub Actions and deployed to AWS S3, with CloudFront used for global content delivery.

---

## Code Submission Flow

![Code Submission Flow](./assets/screenshots/submission-flow.png)

### Step-by-Step Flow

1. User writes code in the Monaco editor and submits it during a duel.
2. The frontend sends the submission request to the main backend.
3. The main backend validates the request, user, match state, and problem constraints.
4. The backend stores or updates relevant submission and match state.
5. A submission job is pushed to AWS SQS.
6. Sentinel service consumes the message from SQS.
7. Sentinel calls the Judge0 API to execute the code.
8. Judge0 returns the execution result.
9. Sentinel processes the result and publishes it to a Redis Pub/Sub topic.
10. Main backend receives the Redis event.
11. Main backend broadcasts the result to both users through WebSockets.
12. Frontend updates the duel screen in real time.

This design avoids blocking API threads while waiting for external code execution and makes the submission pipeline more resilient under bursts of traffic.

---

## Infrastructure as Code

The Phase 2 infrastructure is provisioned using Terraform instead of manual AWS Console configuration.

Terraform currently manages **54 AWS resources**, including:

- VPC
- Public, private, and isolated subnets
- Route tables
- Internet Gateway
- NAT Gateway with Elastic IP
- Application Load Balancer
- Target groups
- ECS cluster
- ECS task definitions for main backend and Sentinel service
- ECS services for both backend services
- IAM roles and policies
- SQS queues
- S3 buckets
- Lambda function and permissions
- CloudFront distribution with cache behaviors and OAC
- RDS PostgreSQL
- ElastiCache Redis
- CloudWatch log groups

The infrastructure is parameterized using Terraform variables such as:

```bash
-var="env=prod"
```

This allows the same infrastructure code to be adapted for environments such as development, staging, and production.

### Current Terraform Tradeoff

For rapid Phase 2 iteration, the project currently uses local Terraform state.

#### Planned Improvement

- Migrate Terraform state to an S3 backend
- Add DynamoDB locking for safer team-based infrastructure changes

---

## AWS Network Topology

The AWS network follows a layered VPC design.

### Edge Layer

- Amazon CloudFront serves static frontend assets from S3.
- CloudFront also routes `/api` and `/ws` traffic to the Application Load Balancer.

### Public Subnet

- Application Load Balancer receives inbound API/WebSocket traffic.
- NAT Gateway allows private backend services to make outbound internet requests, such as calls to Google OAuth and Judge0.

### Private Subnet

- ECS Fargate runs:
    - Main backend service
    - Sentinel service
- ECS tasks do not expose public IP addresses.

### Isolated Subnet

- RDS PostgreSQL
- ElastiCache Redis
- The database and Redis layers are not publicly accessible and only allow traffic from approved backend security groups.

---

## Deployment & CI/CD

The project uses separate GitHub Actions pipelines for frontend, main backend, and Sentinel service.

![CI/CD Pipeline Diagram](./assets/screenshots/cicd-pipeline.png)

### Frontend Pipeline

**Trigger:** Push to `main` branch

**Pipeline:**

1. Install dependencies
2. Build React application
3. Sync `dist/` folder to S3 frontend hosting bucket
4. Invalidate CloudFront cache

### Main Backend Pipeline

**Trigger:** Push to `main` branch

**Pipeline:**

1. Run Gradle tests
2. Build Docker image
3. Push image to AWS ECR
4. Trigger ECS rolling deployment update for `main-backend`

### Sentinel Service Pipeline

**Trigger:** Push to `main` branch

**Pipeline:**

1. Run Gradle unit tests
2. Build Docker image
3. Push image to AWS ECR
4. Trigger ECS rolling deployment update for `sentinel-service`

This allows frontend and backend services to be built, tested, and deployed independently.

---

## Cost-Aware AWS Deployment

Because this is a student-built placement project, the infrastructure is designed with cost awareness in mind.

The frontend can remain available through CloudFront and S3, while backend services can be started only when required for demos or testing.

### Cost-Control Strategy

- Keep static frontend hosted on S3 + CloudFront
- Scale ECS Fargate services down when backend is not needed
- Stop RDS temporarily during inactive periods
- Avoid claiming 24/7 production availability when backend infrastructure is paused
- Keep Terraform code ready to recreate or update infrastructure consistently

This makes the project practical to maintain while still demonstrating real AWS infrastructure, deployment automation, and cloud architecture skills.

---

## Testing

Backend testing was added across both the main backend and Sentinel service.

### Main Backend Testing

Testing tools:

- JUnit 5
- Mockito
- Spring Boot Test

Areas tested:

- Authentication flows
- Matchmaking logic
- WebSocket broadcasting logic
- Repository-layer constraints
- Newly added orchestration logic for asynchronous submission processing

### Sentinel Service Testing

Testing tools:

- JUnit 5
- Mockito

Areas tested:

- SQS message consumption
- Judge0 request payload construction
- Execution result processing
- Redis Pub/Sub publishing

The Sentinel service has unit tests covering the core business flow from message consumption to result publishing. Across the two backend services, around **15-20 core test classes** validate important parts of the event-driven workflow.

---

## Core Features

### User Features

- Google OAuth login
- JWT-based authentication
- 1v1 competitive programming duels
- Match room creation and joining
- Difficulty-based problem selection
- Real-time coding interface
- Monaco-based code editor
- Support for C++, Java, and Python
- Live submission status updates
- Match timer and automatic match completion
- User stats and performance tracking

### Admin / Problem Setter Features

- Role-based access control for admin and problem setter workflows
- Problem metadata creation
- Hidden test case upload flow
- S3 pre-signed URL-based direct uploads
- Lambda-based event processing after test case upload
- Permission-based problem publishing workflow

### Backend / System Features

- Asynchronous code execution using SQS
- Dedicated Sentinel worker service
- Redis Pub/Sub-based result propagation
- WebSocket-based real-time communication
- PostgreSQL-based durable persistence
- Redis-backed real-time state management
- CloudWatch logging
- SSM Parameter Store-based secret management
- Terraform-managed AWS infrastructure
- Independent CI/CD pipelines

---

## Security

CodeDuels includes multiple layers of application and infrastructure security.

### Authentication & Authorization

- Google OAuth-based login
- JWT-based stateless authentication
- Role-Based Access Control for users, admins, and problem setters
- Attribute-Based Access Control for granular problem-management permissions

> **Note:** 2FA exists in backend code but is not currently active in the deployed frontend, so it is not presented as a live user-facing feature.

### Infrastructure Security

- ECS tasks run inside private subnets
- RDS and ElastiCache run in isolated private subnets
- No public IP exposure for backend containers
- Security groups restrict service-to-service communication
- SSM Parameter Store is used for runtime secret management
- IAM roles are used to grant least-privilege access to AWS services
- S3 frontend access is secured through CloudFront Origin Access Control

---

## Performance & Reliability

### Asynchronous Processing

Code execution is handled asynchronously through SQS and the Sentinel service. This prevents long-running Judge0 calls from blocking the main backend API.

### Real-Time Communication

Redis Pub/Sub connects the asynchronous worker layer with the WebSocket layer. This allows the system to push execution results to users as soon as they are processed.

### Caching and State

Redis is used for real-time match state and Pub/Sub communication, while PostgreSQL remains the durable source of truth.

### Direct S3 Uploads

Large test case files are uploaded directly to S3 using pre-signed URLs, avoiding unnecessary load on the backend server.

### Transactional Consistency

Critical backend operations use transactional boundaries to reduce race-condition risks during match creation, submission handling, and match completion.

---

## Tech Stack

| Category | Technology |
| --- | --- |
| **Frontend** | React, Vite, TypeScript, Tailwind CSS, Monaco Editor, StompJS/SockJS |
| **Main Backend** | Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Spring WebSockets |
| **Sentinel Service** | Java 21, Spring Boot 3, Spring Cloud AWS, SQS, Redis Pub/Sub |
| **Database** | AWS RDS PostgreSQL |
| **Cache / PubSub** | AWS ElastiCache Redis |
| **Code Execution** | Judge0 API |
| **Cloud Compute** | AWS ECS Fargate |
| **Networking** | VPC, Public/Private/Isolated Subnets, ALB, NAT Gateway, Security Groups |
| **Frontend Hosting** | AWS S3, Amazon CloudFront, OAC |
| **Messaging** | AWS SQS |
| **Serverless** | AWS Lambda |
| **Secrets** | AWS SSM Parameter Store |
| **Container Registry** | AWS ECR |
| **Infrastructure as Code** | Terraform |
| **CI/CD** | GitHub Actions |
| **Testing** | JUnit 5, Mockito, Spring Boot Test |

---

## Local Development

### Prerequisites

- Java 21
- Gradle
- Node.js
- npm
- Docker
- PostgreSQL
- Redis

### Clone Repository

```bash
git clone https://github.com/Abhinav1416/coding-platform.git
cd coding-platform
```

---

## Environment Variables

> Do not commit real secrets.

### Frontend

```env
VITE_API_BASE_URL=
VITE_WS_BASE_URL=
```

### Main Backend

```env
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
JWT_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
REDIS_HOST=
REDIS_PORT=
AWS_REGION=
SQS_SUBMISSION_QUEUE_URL=
```

### Sentinel Service

```env
AWS_REGION=
SQS_SUBMISSION_QUEUE_URL=
JUDGE0_API_URL=
JUDGE0_API_KEY=
REDIS_HOST=
REDIS_PORT=
```

In the AWS deployment, runtime secrets are managed through AWS SSM Parameter Store and accessed by ECS tasks through IAM roles.