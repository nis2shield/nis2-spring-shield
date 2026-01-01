# NIS2 Spring Shield

[![Maven Central](https://img.shields.io/maven-central/v/com.nis2shield/nis2-spring-shield.svg)](https://central.sonatype.com/artifact/com.nis2shield/nis2-spring-shield)
[![Java CI with Maven](https://github.com/nis2shield/nis2-spring-shield/actions/workflows/maven.yml/badge.svg)](https://github.com/nis2shield/nis2-spring-shield/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://jdk.java.net/21/)

**NIS2 Spring Shield** is a Spring Boot Starter designed to help Java enterprise applications comply with the **NIS2 Directive** requirements. It provides ready-to-use forensic logging, active defense mechanisms, and data integrity protection.

### The "Security-First" Spring Boot Starter for NIS2 Compliance.

Companies subject to NIS2 Directive need **demonstrable compliance**. This starter provides:

1.  **Forensic Logging**: JSON structured logs with HMAC-SHA256 integrity (Art. 21.2.h)
2.  **PII Encryption**: AES-256 encryption for sensitive fields (GDPR-compliant)
3.  **Active Defense**: Rate Limiting (Bucket4j) & Tor Blocking (Art. 21.2.e)
4.  **Health Monitoring**: Spring Actuator integration for operations
5.  **Multi-SIEM**: Presets for Splunk, Datadog, QRadar.

> **Part of the NIS2 Shield Ecosystem**: Use with [`@nis2shield/react-guard`](https://github.com/nis2shield/react-guard), [`@nis2shield/angular-guard`](https://github.com/nis2shield/angular-guard), or [`@nis2shield/vue-guard`](https://github.com/nis2shield/vue-guard) for client-side protection and [`nis2shield/infrastructure`](https://github.com/nis2shield/infrastructure) for a full-stack implementation.

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend                              │
│  @nis2shield/{react,angular,vue}-guard                      │
│  ├── SessionWatchdog (idle detection)                       │
│  ├── AuditBoundary (crash reports)                         │
│  └── → POST /api/nis2/telemetry/                           │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  Backend (NIS2 Adapter)                      │
│  Supported: Django, Express, Spring Boot, .NET            │
│  ├── ForensicLogger (HMAC signed logs)                     │
│  ├── RateLimiter, SessionGuard, TorBlocker                 │
│  └── → SIEM (Elasticsearch, Splunk, QRadar, etc.)          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure                            │
│  nis2shield/infrastructure                                  │
│  ├── Centralized Logging (ELK/Splunk)                       │
│  └── Audited Deployment (Terraform/Helm)                    │
└─────────────────────────────────────────────────────────────┘
```

## Features

*   **Forensic Logging (Audit):**
    *   Captures all HTTP requests and responses.
    *   **JSON Structured Logs**: Machine-readable format compatible with SIEMs.
    *   **Integrity Signing**: Logs are signed with HMAC-SHA256 to prevent tampering.
    *   **PII Encryption**: Automatically encrypts sensitive fields (User ID, Email, IP) using AES-256.
    *   **IP Anonymization**: Masks the last octet of IP addresses for GDPR compliance.

*   **Active Defense:**
    *   **Rate Limiting**: Sliding window algorithm (via Bucket4j) to prevent DoS/Brute Force.
    *   **Tor Blocker**: Blocks requests from known Tor exit nodes (configurable).
    *   **Malicious IP Blocking**: Structure ready for threat intelligence integration.

*   **Health Monitoring:**
    *   Exposes status via Spring Boot Actuator (`/actuator/health`).

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.nis2shield</groupId>
    <artifactId>nis2-spring-shield</artifactId>
    <version>0.1.0</version>
</dependency>
```

That's it! No additional repository configuration required - the library is available on **Maven Central**.


## Configuration

Configure the shield in your `application.yml`:

```yaml
nis2:
  enabled: true
  # Keys for security (Use environment variables in production!)
  encryption-key: "VGhpcyBJcyBBIFRlc3QgS2V5IEZvciBBRVMgMjU2IQ==" # Base64 AES-256 Key
  integrity-key: "change-this-to-a-very-secret-hmac-key"
  
  logging:
    enabled: true
    encrypt-pii: true     # Encrypt sensitive fields
    anonymize-ip: true    # Mask IP (192.168.1.xxx)

  active-defense:
    rate-limit-enabled: true
    rate-limit-capacity: 100        # Requests allowed
    rate-limit-window-seconds: 60   # Per window (e.g., 1 minute)
    block-tor-exit-nodes: true      # Block known Tor nodes
```

## Usage

Once added, the starter automatically configures:
1.  **`Nis2AuditingFilter`**: Intercepts requests for logging.
2.  **`ActiveDefenseFilter`**: Enforces security policies before the request reaches your controller.
3.  **`Nis2HealthIndicator`**: Adds NIS2 status to Actuator.

### Example Log Output

```json
{
  "log": {
    "module": "NIS2-SHIELD-SPRING",
    "timestamp": "2025-12-30T16:00:00Z",
    "request": {
      "method": "POST",
      "path": "/api/v1/login",
      "ip": "203.0.113.xxx",   // Anonymized
      "user_agent": "Mozilla/5.0..."
    },
    "response": {
      "status": 200,
      "duration_ms": 150
    },
    "user_id": "[ENCRYPTED]...base64..." // Encrypted PII
  },
  "integrity_hash": "a1b2c3d4..." // HMAC-SHA256 Signature
}
```


## Actuator Endpoint

Check the status of the shield:

```bash
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "nis2_shield": {
      "status": "UP",
      "details": {
        "active": "Active",
        "blocked_tor_ips": 0
      }
    }
  }
}
```

## Compliance CLI (`check_nis2`)

You can audit your application configuration against NIS2 requirements directly from the command line:

```bash
java -jar your-app.jar --check-nis2
```

**Output Example:**

```text
[NIS2 SHIELD AUDIT REPORT]
Application: Spring Boot Application
Generated: 2025-12-31T12:00:00Z
------------------------------------------------
[PASS] NIS2 Shield Enabled (General)
       NIS2 Shield middleware is active
[PASS] Integrity Key (Art. 21.2.h)
       HMAC signing key for log integrity (min 32 chars)
[FAIL] Encryption Key (Art. 21.2.f)
       AES encryption key for PII (16/24/32 chars for AES-128/192/256)
...
------------------------------------------------
COMPLIANCE SCORE: 85/100
```

Also generates a detailed HTML report for auditors.

## 📖 Recipes

### Banking API with Strict Configuration

```yaml
# application.yml
nis2:
  enabled: true
  encryption-key: ${NIS2_AES_KEY}  # Base64 AES-256
  integrity-key: ${NIS2_HMAC_KEY}
  
  logging:
    enabled: true
    encrypt-pii: true
    anonymize-ip: true
  
  active-defense:
    rate-limit-enabled: true
    rate-limit-capacity: 30      # Strict: 30 req/min for banking
    rate-limit-window-seconds: 60
    block-tor-exit-nodes: true
```

### E-commerce with Relaxed Rate Limits

```yaml
nis2:
  enabled: true
  logging:
    enabled: true
    anonymize-ip: true
  active-defense:
    rate-limit-enabled: true
    rate-limit-capacity: 200
    rate-limit-window-seconds: 60
    block-tor-exit-nodes: false  # Allow Tor for privacy
```

### Microservice with Custom Compliance Runner

```bash
# Run compliance audit at startup
java -jar your-app.jar --check-nis2
```

## Release Process

Automated releases are handled via GitHub Actions.

1. **Create Tag**: Push a new tag (e.g., `v0.2.0`).
2. **GitHub Release**: Create a release in the GitHub UI.
3. **CI/CD**: The `publish.yml` workflow triggers automatically:
    - Builds the project.
    - Generates Javadoc and Source JARs.
    - Signs artifacts with GPG (Secrets: `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`).
    - Deploys to **Maven Central** (Secrets: `OSSRH_USERNAME`, `OSSRH_TOKEN`).
    
---

## License

MIT License - See LICENSE file for details.
