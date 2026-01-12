# Admin Console & Operational Guidelines

> **Note to OpenCode:** This document outlines the functional requirements, operational capabilities, and security considerations for the CareerAgent Admin Console. This console is critical for system monitoring, user support, and content management.

## 1. Purpose & Scope

The Admin Console serves as the central hub for system administrators and support personnel to:
*   Monitor system health and performance.
*   Manage users and their data.
*   Curate and moderate system content (e.g., job postings, AI responses).
*   Perform operational tasks and troubleshoot issues.

## 2. Access Control

*   **Role**: Access is strictly limited to users with `ROLE_ADMIN` as defined in `CONTRACTS.md`.
*   **Authentication**: Utilizes the same JWT-based authentication mechanism as the user client.
*   **Authorization**: All routes under `/admin/**` are protected and require `ROLE_ADMIN` permissions, enforced by the Biz Service.

## 3. Key Modules & Features

### 3.1 Dashboard (`/admin`)
*   **System Health Overview**:
    *   Service status (Biz, AI, Search services).
    *   Kafka topic lag and consumer group health.
    *   PostgreSQL/Redis connection status.
    *   AI Engine token usage and cost tracking.
*   **User Statistics**: Active users, new registrations, user churn.
*   **Job Metrics**: Total jobs crawled, new jobs, parsing success rate.

### 3.2 User Management (`/admin/users`)
*   **User List**: Search, filter, and view user details (email, registration date, last login).
*   **CRUD Operations**:
    *   **View User Profile**: Access to user's resumes (excluding sensitive PII by default).
    *   **Role Assignment**: Modify user roles (`ROLE_USER` <-> `ROLE_ADMIN`).
    *   **Account Status**: Suspend/Activate user accounts.
    *   **Data Export/Deletion**: Initiate user data export or complete deletion (GDPR/privacy compliance).
*   **Impersonation (for support)**: Securely view user's session (read-only) for troubleshooting (highly restricted).

### 3.3 Content Management (`/admin/jobs`, `/admin/resumes`)
*   **Job Moderation (`/admin/jobs`)**:
    *   List of crawled jobs, with filters for status (raw, parsed, reviewed).
    *   Manual review and editing of job details (title, description, company).
    *   Approve/Reject job postings for public visibility.
    *   Initiate re-crawls for specific URLs.
*   **Resume Template Management (`/admin/resume-templates`)**:
    *   Upload and manage different resume output templates (e.g., Modern, Classic, Academic).

### 3.4 Monitoring & Troubleshooting (`/admin/logs`, `/admin/metrics`)
*   **Centralized Log Viewer**: Filterable log streams from all services, correlated by `X-Trace-ID`.
*   **Metrics Explorer**: Integration with Prometheus/Grafana dashboards for detailed operational metrics (external link or embedded).
*   **Alert Management**: View and manage system alerts.

### 3.5 Configuration Management (`/admin/settings`)
*   **Feature Flags**: Toggle application features on/off (e.g., new AI model, experimental UI).
*   **LLM Configuration**: Adjust LLM parameters (e.g., temperature, max tokens) for specific agents.
*   **Crawler Settings**: Adjust global crawl depth, concurrency limits.

## 4. Technical Implementation

*   **Platform**: Implemented as part of the `/frontend` Next.js application.
*   **Backend API**: Utilizes dedicated Biz Service API endpoints (`/api/admin/**`) for all administrative actions.
*   **UI Components**: Consistent use of Shadcn/UI for a uniform admin experience.

## 5. Security Considerations

*   **Least Privilege**: Admin users should only have access to the functions required for their role.
*   **Audit Logs**: All administrative actions (e.g., changing user role, modifying job content) MUST be logged with `user_id`, `action`, `timestamp`, and `details`.
*   **Input Validation**: Strict validation for all admin inputs to prevent XSS, SQL Injection, etc.
*   **Secure Communication**: All communication between Admin Console and Biz Service via HTTPS.

<h2>6. Operational Workflows</h2>

*   **Incident Response**: How to diagnose and respond to system alerts using the console.
*   **Content Curation**: Workflow for manual review of newly crawled jobs.
*   **User Support**: Workflow for handling user data requests or account issues.
