# Spring NIS2 Shield - Project Plan

## 🎯 Goal
Port the logic of `django-nis2-shield` to the Java Enterprise ecosystem via a **Spring Boot Starter**.
The goal is to provide a "plug-and-play" library that makes a Spring Boot application compliant with NIS2 regulations (Art. 21) simply by adding a dependency.

## 🏗 Architecture
The project will be structured as a custom **Spring Boot Starter**.
- **AutoConfiguration**: Automatic configuration of beans if properties are present.
- **Interceptor/Filter**: For logging requests and traffic analysis.
- **AOP (Aspect Oriented Programming)**: For custom annotations on specific methods.

### Technology Stack
- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Logging**: SLF4J + Logback (JSON format)
- **Rate Limiting**: Bucket4j (robust Java equivalent)
- **Security**: Spring Security integration

---

## 🗺 Roadmap & To-Do List

### Phase 1: Setup & Core (Log & Config) ✅
Goal: Have an application that, once the library is imported, logs every request in JSON format compatible with the existing infrastructure.

- [x] **Project Skeleton**:
    - [x] `pom.xml` with base dependencies (spring-boot-starter, jackson, slf4j).
    - [x] `com.nis2shield.spring` package structure.
- [x] **Configuration Properties**:
    - [x] `Nis2Properties` class (`nis2.enabled`, `nis2.logging.mode`).
    - [x] Support for `application.yml`.
- [x] **Nis2LogEngine (The Truth)**:
    - [x] Creation of `Nis2AuditingFilter` (or `HandlerInterceptor`).
    - [x] Request/Response capture (paying attention to `ContentCachingRequestWrapper`).
    - [x] Sensitive data masking (PII scrubbing).
    - [x] Standardized JSON output (same as Django: `user`, `ip`, `path`, `method`, `risk_score`).

### Phase 2: Active Defense (Proactive Protection) ✅
Porting of proactive defense mechanisms.

- [x] **Rate Limiting**:
    - [x] `Bucket4j` integration.
    - [x] Sliding Window algorithm based on IP or User.
- [x] **IP Blocking**:
    - [x] Loading blocked/suspicious IP list (Tor exit nodes locally or via API).
- [x] **Security Headers**:
    - [x] Automatic setting of HSTS, X-Content-Type-Options, CSP, Referrer-Policy, Permissions-Policy.

### Phase 3: Cryptography & Integrity (Art. 21) ✅
Tools for data protection at rest.

- [x] **CryptoUtils**:
    - [x] AES-256 helper (reusing the standard used in Django for compatibility).
    - [x] `KeyRotationManager` stub.
- [x] **Log Hashing**:
    - [x] HMAC-SHA256 calculation for every log entry to ensure non-repudiation (Integrity Signing).

### Phase 4: Integration & Release ✅ COMPLETE
- [x] **Actuator Endpoints**:
    - [x] `/actuator/health` endpoint with `Nis2HealthIndicator` for compliance status.
- [x] **Infrastructure Integration**:
    - [x] Docker Compose + Fluent Bit configuration for interoperability testing.
- [x] **Publishing**:
    - [x] Deploy to GitHub Packages (v0.1.0).
    - [x] **Deploy to Maven Central** ✅ (December 30, 2025).
- [x] **Documentation**:
    - [x] README updated with Maven Central badge.
    - [x] Webpage nis2shield.com/spring-shield updated.

---

## 🚀 v0.1.0 - RELEASED (December 30, 2025)

The **nis2-spring-shield v0.1.0** project has been released on Maven Central!

```xml
<dependency>
    <groupId>com.nis2shield</groupId>
    <artifactId>nis2-spring-shield</artifactId>
    <version>0.1.0</version>
</dependency>
```

---

## 🚀 v0.2.0 - RELEASED (December 31, 2025)

The **nis2-spring-shield v0.2.0** project includes Multi-SIEM Connectors, Webhooks, and Session Security.

### Phase 5: Session Security & Advanced Features ✅ COMPLETE
- [x] **SessionGuard**:
    - [x] Session hijacking detection (device fingerprint change).
    - [x] Automatic invalidation of suspicious sessions.
- [x] **KeyRotationManager with KMS**:
    - [x] `KmsProvider` interface for external KMS.
    - [x] `VaultKmsProvider` implementation for HashiCorp Vault.
    - [x] Key rotation configuration (90 days default).

### Phase 6: Multi-SIEM & Notifications ✅ COMPLETE
- [x] **Multi-SIEM Connectors**:
    - [x] Splunk HEC support.
    - [x] IBM QRadar (CEF/Syslog) support.
    - [x] Graylog GELF support.
    - [x] Datadog support.
- [x] **Webhook Notifications**:
    - [x] Alert on critical events (rate limit exceeded, blocked IP, etc.).
    - [x] Slack/Microsoft Teams support via webhook URL.

### Phase 7: Compliance Engine ✅ COMPLETE
- [x] **check_nis2 CLI**:
    - [x] `Nis2ComplianceChecker` service for configuration audit.
    - [x] `Nis2ComplianceRunner` for CLI via `--check-nis2`.
- [x] **Compliance Reports**:
    - [x] `ComplianceReportService` for HTML/JSON report generation.
    - [x] HTML template styled with NIS2 Shield branding.

---

## 📝 Log Conventions (Interoperability)
The JSON format must be identical to that produced by the Django middleware to ensure that the Kibana/OpenSearch dashboard works for both.

```json
{
  "timestamp": "2023-10-27T10:00:00Z",
  "app_name": "spring-backend",
  "level": "INFO",
  "module": "nis2_shield",
  "type": "audit_log",
  "user": "admin@example.com",
  "ip": "192.168.1.1",
  "path": "/api/v1/resource",
  "method": "POST",
  "status_code": 200,
  "risk_score": 0,
  "metadata": {...}
}
```
