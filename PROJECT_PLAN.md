# Spring NIS2 Shield - Project Plan

## 🎯 Obiettivo
Porting della logica di `django-nis2-shield` nell'ecosistema Java Enterprise tramite uno **Spring Boot Starter**.
L'obiettivo è fornire una libreria "plug-and-play" che renda conforme un'applicazione Spring Boot alla normativa NIS2 (Art. 21) semplicemente aggiungendo una dipendenza.

## 🏗 Architettura
Il progetto sarà strutturato come uno **Spring Boot Starter** personalizzato.
- **AutoConfiguration**: Configurazione automatica dei bean se le proprietà sono presenti.
- **Interceptor/Filter**: Per il logging delle richieste e l'analisi del traffico.
- **AOP (Aspect Oriented Programming)**: Per annotazioni custom su metodi specifici.

### Stack Tecnologico
- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Logging**: SLF4J + Logback (JSON format)
- **Rate Limiting**: Bucket4j (equivalente Java robusto)
- **Security**: Spring Security integration

---

## 🗺 Roadmap & To-Do List

### Fase 1: Setup & Core (Log & Config) ✅
L'obiettivo è avere un'applicazione che, una volta importata la libreria, logghi ogni richiesta in formato JSON compatibile con l'infrastruttura esistente.

- [x] **Project Skeleton**:
    - [x] `pom.xml` con dipendenze base (spring-boot-starter, jackson, slf4j).
    - [x] Struttura package `com.nis2shield.spring`.
- [x] **Configuration Properties**:
    - [x] Classe `Nis2Properties` (`nis2.enabled`, `nis2.logging.mode`).
    - [x] Supporto per `application.yml`.
- [x] **Nis2LogEngine (The Truth)**:
    - [x] Creazione filtro `Nis2AuditingFilter` (o `HandlerInterceptor`).
    - [x] Cattura Request/Response (facendo attenzione a `ContentCachingRequestWrapper`).
    - [x] Mascheramento dati sensibili (PII scrubbing).
    - [x] Output JSON standardizzato (uguale a Django: `user`, `ip`, `path`, `method`, `risk_score`).

### Fase 2: Active Defense (Protezione Attiva) ✅
Porting dei meccanismi di difesa proattiva.

- [x] **Rate Limiting**:
    - [x] Integrazione `Bucket4j`.
    - [x] Sliding Window algorithm su base IP o User.
- [x] **IP Blocking**:
    - [x] Caricamento lista IP bloccati/sospetti (Tor exit nodes locally or via API).
- [x] **Security Headers**:
    - [x] Set automatico di HSTS, X-Content-Type-Options, CSP, Referrer-Policy, Permissions-Policy.

### Fase 3: Crittografia & Integrità (Art. 21) ✅
Strumenti per la protezione dei dati a riposo.

- [x] **CryptoUtils**:
    - [x] Helper per AES-256 (riutilizzando lo standard usato in Django per compatibilità).
    - [x] `KeyRotationManager` stub.
- [x] **Log Hashing**:
    - [x] Calcolo HMAC-SHA256 di ogni log entry per garantire non-rifiuto (Integrity Signing).

### Fase 4: Integrazione & Rilascio ✅ COMPLETE
- [x] **Actuator Endpoints**:
    - [x] Endpoint `/actuator/health` con `Nis2HealthIndicator` per stato conformità.
- [x] **Integrazione Infrastruttura**:
    - [x] Docker Compose + Fluent Bit configuration per test di interoperabilità.
- [x] **Publishing**:
    - [x] Deploy su GitHub Packages (v0.1.0).
    - [x] **Deploy su Maven Central** ✅ (30 Dicembre 2025).
- [x] **Documentazione**:
    - [x] README aggiornato con badge Maven Central.
    - [x] Pagina web nis2shield.com/spring-shield aggiornata.

---

## 🚀 v0.1.0 - RELEASED (30 Dicembre 2025)

Il progetto **nis2-spring-shield v0.1.0** è stato rilasciato su Maven Central!

```xml
<dependency>
    <groupId>com.nis2shield</groupId>
    <artifactId>nis2-spring-shield</artifactId>
    <version>0.1.0</version>
</dependency>
```

---

## 🚀 v0.2.0 - RELEASED (31 Dicembre 2025)

Il progetto **nis2-spring-shield v0.2.0** include Multi-SIEM Connectors, Webhooks e Session Security.

### Fase 5: Session Security & Advanced Features ✅ COMPLETE
- [x] **SessionGuard**:
    - [x] Rilevamento session hijacking (device fingerprint change).
    - [x] Invalidazione automatica sessioni sospette.
- [x] **KeyRotationManager con KMS**:
    - [x] Interfaccia `KmsProvider` per KMS esterni.
    - [x] Implementazione `VaultKmsProvider` per HashiCorp Vault.
    - [x] Configurazione rotazione chiavi (90 giorni default).

### Fase 6: Multi-SIEM & Notifications ✅ COMPLETE
- [x] **Multi-SIEM Connectors**:
    - [x] Supporto Splunk HEC.
    - [x] Supporto IBM QRadar (CEF/Syslog).
    - [x] Supporto Graylog GELF.
    - [x] Supporto Datadog.
- [x] **Webhook Notifications**:
    - [x] Alert su eventi critici (rate limit exceeded, blocked IP, etc.).
    - [x] Supporto Slack/Microsoft Teams via webhook URL.

### Fase 7: Compliance Engine
- [ ] **check_nis2 CLI**:
    - [ ] Comando per verificare conformità applicazione (come Django).
- [ ] **Compliance Reports**:
    - [ ] Generazione report PDF/HTML per auditor.

---

## 📝 Convenzioni Log (Interoperabilità)
Il formato JSON deve essere identico a quello prodotto dal middleware Django per garantire che la dashboard Kibana/OpenSearch funzioni per entrambi.

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

