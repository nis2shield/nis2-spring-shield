# NIS2 Spring Shield

[![Maven Central](https://img.shields.io/maven-central/v/com.nis2shield/nis2-spring-shield.svg)](https://central.sonatype.com/artifact/com.nis2shield/nis2-spring-shield)
[![Java CI with Maven](https://github.com/nis2shield/nis2-spring-shield/actions/workflows/maven.yml/badge.svg)](https://github.com/nis2shield/nis2-spring-shield/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://jdk.java.net/21/)

**NIS2 Spring Shield** is a Spring Boot Starter designed to help Java enterprise applications comply with the **NIS2 Directive** requirements. It provides ready-to-use forensic logging, active defense mechanisms, and data integrity protection.

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

## License

MIT License - See LICENSE file for details.
