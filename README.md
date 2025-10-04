# Class-Scheduler-App
This project is an engineering thesis focused on designing and implementing a web application for managing tutoring schedules and additional class registrations.

## 🎯 Purpose
The system allows users to:
- Plan and manage tutoring sessions.
- Synchronize individual calendars to avoid conflicts.
- Handle class registrations and manage participant preferences.
- Notify users about upcoming lessons and available time slots.

## 🏗️ Architecture
The project is based on a multi-layered architecture:
- **Frontend:** React + JavaScript  
- **Backend:** Spring Boot (Java)  
- **Database:** PostgreSQL  
- **CI/CD & DevOps:** Jenkins, Docker, Kubernetes  
- **Cloud Deployment:** AWS  

## 🧩 Engineering Aspects
- Continuous Integration and Delivery automation.  
- Environment configuration and deployment pipelines.  
- Container orchestration.  
- Testing and performance verification.  

## 🌿 Git Workflow (GitFlow)
This project follows the **GitFlow branching model** to maintain a clean and consistent development process.

### 🔹 Main Rules
- `main` and `develop` branches are **protected** — force pushes are **not allowed**.  
- Every new feature must be developed in a separate branch using the pattern:  
  ```
  feature/<feature-name>
  ```
  Example: `feature/schedule-calendar`

- Bug fixes must use a similar naming pattern:  
  ```
  fix/<bug-description>
  ```
  Example: `fix/login-auth-error`

- All changes must go through **Pull Requests (PRs)** before merging into `develop`.  
  Each PR requires:
  - Code review approval  
  - Successful CI/CD pipeline checks  

- The `main` branch is updated **only through release merges** from `develop` (e.g., `release/v1.0.0`).  

This ensures stability, proper versioning, and reliable CI/CD automation across environments.

## 🧪 Tests
- Unit tests for backend and frontend modules.  
- Integration tests for API endpoints.  
- Automated builds and test runs triggered by Jenkins pipeline.  

## 👨‍💻 Author
**Milosz Markowski**  
Wroclaw University of Science and Technology  
Field of study: Computer Engineering
