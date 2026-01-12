# Security & Compliance Specification

> **Note to OpenCode:** This document outlines the mandatory security controls, data protection measures, and compliance requirements for the CareerAgent system. All services and components MUST adhere to these principles to protect user data and ensure legal compliance. **Security is a shared responsibility across all teams.**

## 1. Data Protection & Privacy

### 1.1 Data Classification
*   **Sensitive PII**: Email, Name, Phone Number, Address, potentially resume content.
*   **Non-Sensitive PII**: User ID, IP Address (if anonymized).
*   **Public Data**: General job descriptions (after parsing and anonymization).

### 1.2 Encryption
*   **Encryption at Rest**:
    *   All sensitive PII in PostgreSQL (e.g., specific columns like full name, email in `users` and `resumes` if stored unhashed) MUST be encrypted using **AES-256**. Keys MUST be managed by a secure Key Management Service (KMS).
    *   Object Storage (MinIO/S3) MUST enforce server-side encryption (SSE).
*   **Encryption in Transit**:
    *   All external (Frontend <-> Biz Service) and internal (gRPC, Kafka over network) communications MUST use **TLS 1.2+**.

### 1.3 PII Handling
*   **Minimization**: Collect only necessary PII.
*   **Anonymization/Pseudonymization**: For analytics and logging, PII must be anonymized or pseudonymized where possible.
*   **Logging**: Sensitive PII MUST NOT be written to logs. Log anonymized IDs (e.g., `user_id` hash) instead.

### 1.4 Data Retention & Deletion
*   **Policy**: Define clear data retention periods for different data types.
*   **Right to Be Forgotten**: Implement processes for complete and irreversible deletion of user data upon request (covering all databases, backups, and logs).

### 1.5 Backup & Recovery
*   All persistent data stores (PostgreSQL, Object Storage) MUST have automated, encrypted backups.
*   Regular disaster recovery drills to ensure data can be restored.

## 2. Authentication & Authorization

*   **Authentication**:
    *   **Google OAuth2**: Sole authentication mechanism for users.
    *   **JWT (HttpOnly Cookie)**: Used for session management and authentication. MUST be securely managed (e.g., refresh token rotation).
*   **Authorization (RBAC)**:
    *   Implement Role-Based Access Control (RBAC) across all services (`ROLE_USER`, `ROLE_ADMIN`).
    *   **Least Privilege**: Grant users and services the minimum necessary permissions to perform their functions.
    *   Admin Console access strictly restricted to `ROLE_ADMIN` users with robust access logging.

## 3. Application Security

*   **Input Validation**: Strict server-side input validation for all user-provided data to prevent common vulnerabilities (e.g., XSS, SQL Injection, Command Injection).
*   **Secure Coding Practices**: Adhere to OWASP Top 10 security best practices.
*   **Dependency Scanning**: Regularly scan third-party libraries for known vulnerabilities (e.g., using Snyk, Trivy).
*   **Secrets Management**:
    *   API keys, database credentials, and other secrets MUST NOT be hardcoded.
    *   Use environment variables, Kubernetes Secrets, or a dedicated secrets manager (e.g., HashiCorp Vault).

## 4. Network & Infrastructure Security

*   **Network Segmentation**: Deploy services in separate network segments (VPCs, subnets).
*   **Firewall Rules**: Implement strict firewall rules (Security Groups) to limit traffic between services to only necessary ports and protocols.
*   **Vulnerability Scanning**: Regular scanning of infrastructure (VMs, containers) for vulnerabilities.
*   **Secure Configurations**: Default-deny policies, disable unused services, strong password policies for infrastructure.

## 5. Crawler Compliance & Ethics

*   **`robots.txt` Adherence**: The `Search-Service` MUST strictly respect `robots.txt` rules of target websites.
*   **Rate Limiting**: Implement per-domain rate limiting to avoid overwhelming target servers and being perceived as malicious.
*   **Ethical Scraping**: Avoid scraping sensitive personal data from public sites unless explicitly authorized.
*   **Legal**: Ensure compliance with relevant data privacy laws (GDPR, CCPA) for any publicly scraped data that might contain PII.

## 6. Privacy & Legal Compliance

*   **GDPR / CCPA Readiness**:
    *   **Data Subject Rights**: Implement mechanisms for users to access, rectify, erase, and port their data.
    *   **Consent Management**: Obtain explicit consent for data processing where required.
*   **Terms of Service & Privacy Policy**: Clearly define how user data is collected, used, and protected.

## 7. Incident Response Plan (Basic)

*   **Detection**: Monitoring and alerting for security events (e.g., failed logins, suspicious activity).
*   **Containment**: Steps to isolate compromised systems.
*   **Eradication**: Removing the threat.
*   **Recovery**: Restoring services and data.
*   **Post-Incident Analysis**: Learnings and preventative measures.

<h2>8. Audit & Logging</h2>

*   **Comprehensive Logging**: Log all security-relevant events (failed logins, authorization failures, data access by admins).
*   **Immutable Logs**: Ensure logs are protected from tampering.
*   **Centralized Logging**: Ship logs to a central logging system with restricted access.
