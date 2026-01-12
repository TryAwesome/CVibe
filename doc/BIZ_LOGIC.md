# Java Service: Core Business Logic (Biz Service)

> **Note to OpenCode:** This document outlines the architecture, key business flows, and technical guidelines for the `/biz-service` (Java Spring Boot) application. This service is the owner of core domain data and orchestrates complex business processes. Focus on **Domain-Driven Design (DDD)**, transactional integrity, security, and maintainability.

## 1. Core Principles

*   **Domain-Driven Design**: Model the business domain explicitly (Users, Resumes, Jobs, Applications) with clear aggregates and boundaries.
*   **Transactional Integrity**: Ensure data consistency using Spring's transactional mechanisms for all critical operations.
*   **Security First**: Implement robust authentication, authorization, and data protection according to `SECURITY_COMPLIANCE.md`.
*   **Maintainability**: Adhere to clean code principles, SOLID principles, and Spring Boot best practices.
*   **Event-Driven**: Embrace Kafka for asynchronous, decoupled business events.

## 2. Architectural Patterns

### 2.1 Layered Architecture
*   **Controller Layer**: Handles HTTP requests, input validation, and delegates to service layer.
*   **Service Layer**: Contains core business logic, orchestrates domain objects, and manages transactions.
*   **Repository Layer**: Abstracts data persistence (JPA/Hibernate for PostgreSQL).
*   **Domain Layer**: Contains business entities and value objects.

### 2.2 Spring Boot Ecosystem
*   **Spring Boot 3**: Latest stable version.
*   **Spring Data JPA**: For PostgreSQL interactions.
*   **Spring Security**: For Authentication (JWT) and Authorization (RBAC).
*   **Spring Kafka**: For Kafka message consumption and production.
*   **Lombok**: For reducing boilerplate code (getters, setters, constructors).

## 3. Key Business Flows

### 3.1 User Management & Authentication
*   **Dual Authentication Support**:
    *   **Email/Password**: Standard registration and login flows. Support password changes within User Settings.
    *   **Google OAuth2**: One-click login/registration.
*   **User Configuration**:
    *   **AI Settings**: Users can configure their own **API Key**, **Base URL**, and **Model Selection** (e.g., GPT-4, Gemini) to be used for all AI operations.
*   **Admin User Management**: CRUD for users (Admin Console only), including role assignment.

### 3.2 Profile Database (The Core Truth)
*   **Source of Truth**: The `Profile Database` is the central repository of a user's skills and experiences.
*   **Construction Logic**:
    *   **Primary Source**: Constructed from the content of the **last completed AI Interview**.
    *   **Fallback Source**: If no interview has been conducted, it is extracted from the **last uploaded PDF resume**.
*   **Dynamic Updates**: The database is modular and editable by the user or updated via subsequent interviews.

### 3.3 AI Interview (Profile Building)
*   **Objective**: To build a comprehensive `Profile Database`.
*   **Behavioral Logic**:
    *   **Comprehensive Coverage**: Must ask about all aspects required for a complete resume, with careful and exhaustive questioning.
    *   **Deep Dive**: Specifically for **Work Experience** and **Research/Academic Projects**, the AI must ask follow-up questions to extract specific details (metrics, scope, technologies used, role played, outcomes) rather than accepting surface-level answers.

### 3.4 Resume Management & Builder
*   **Resume History**:
    *   Stores uploaded raw files (users can upload directly from the Dashboard).
    *   The most recent upload becomes the fallback input when no interview has been completed.
    *   **Auto-Archive**: Automatically adds a new entry to history whenever a user **generates and exports a PDF** from the Resume Builder.
*   **Resume Builder Logic**:
    1.  **Input**: User selects a LaTeX template (System provided or **User Uploaded .tex**) and uploads a **Job Description (HC Requirements)** screenshot/text.
    2.  **AI Orchestration**:
        *   Retrieves data from `Profile Database`.
        *   Analyzes Job Description.
        *   **Tailoring**: Selects the most relevant experiences, emphasizes keywords, and polishes language to match the JD.
        *   **Injection**: Inserts processed content into the selected LaTeX template.
    3.  **Editor**: Provides a split view (Left: Source TeX, Right: Real-time PDF Compile). Support granular editing before export.

### 3.5 Job Matching & Recommendations
*   **Daily Crawl**: System triggers a daily crawl of relevant job boards.
*   **Matching Logic**: matches crawled jobs against the user's `Profile Database` (not just keywords, but semantic overlap).
*   **Dashboard Integration**: The Dashboard displays the latest high-match jobs with "Why you match" analysis and keeps the last matched snapshot visible.
*   **Action**: Supports direct deep-linking to the job post.

### 3.6 Career Growth (Gap Analysis)
*   **Input**: User provides **Target Role/Company** (via text or uploading an HC Requirement image).
*   **Analysis**: Compares `Profile Database` vs. `Target Requirements`.
*   **Output**:
    *   Identifies concrete gaps (missing skills, lack of project complexity).
    *   **Learning Path**: Generates a specific, actionable roadmap (e.g., "Learn Kubernetes", "Build a distributed system project") with concrete steps.

### 3.7 Mock Interview (Simulation)
*   **Persona**: AI acts as a Hiring Manager from the specific target company.
*   **Content**:
    *   **Coding**: Randomly selects questions from a "Big Tech Coding Question Bank".
    *   **"BaGuWen" (Standard Tech Trivia)**: Drills on core concepts (Java, OS, Network).
    *   **Resume Deep Dive**: Aggressively questions details in the provided resume.
    *   **Behavioral/Business**: Asks scenario-based questions and business interview prompts.
*   **Tone**: **High Pressure**. The AI should NOT be overly encouraging. It should express skepticism ("Are you sure?", "Explain that in more detail") to simulate a stress interview.

### 3.8 Community
*   **Features**: Users can post text and **Images**.
*   **Interaction**: Comments and Likes. Notification system for interactions.

## 4. Data Access & Persistence

*   **PostgreSQL**: Primary store for Users, Profile Database, Resume History, Jobs, Posts, Comments.
*   **Redis**: Session management, Deduplication keys for crawler.
*   **MinIO/S3**: Storage for Resume PDFs, Uploaded Job Descriptions, Community Images.

## 5. External Integrations

*   **AI Engine**: Handles all logic for Interviewing, Resume Tailoring, and Matching.
    *   *Constraint*: Must support dynamic API Key/Base URL injection based on User Settings.
*   **Search Service**: Handles the "Daily Crawl" task.

## 6. Security

*   **JWT Validation**: Standard Bearer token flow.
*   **User Secrets**: User-provided API Keys must be encrypted at rest.

<h2>7. Error Handling</h2>

*   **Custom Exceptions**: Define custom business exceptions.
*   **Global Exception Handler**: Maps to standard HTTP codes.
