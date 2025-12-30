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

### Fase 1: Setup & Core (Log & Config)
L'obiettivo è avere un'applicazione che, una volta importata la libreria, logghi ogni richiesta in formato JSON compatibile con l'infrastruttura esistente.

- [ ] **Project Skeleton**:
    - [ ] `pom.xml` con dipendenze base (spring-boot-starter, jackson, slf4j).
    - [ ] Struttura package `com.nis2shield.spring`.
- [ ] **Configuration Properties**:
    - [ ] Classe `Nis2Properties` (`nis2.enabled`, `nis2.logging.mode`).
    - [ ] Supporto per `application.yml`.
- [ ] **Nis2LogEngine (The Truth)**:
    - [ ] Creazione filtro `Nis2AuditingFilter` (o `HandlerInterceptor`).
    - [ ] Cattura Request/Response (facendo attenzione a `ContentCachingRequestWrapper`).
    - [ ] Mascheramento dati sensibili (PII scrubbing).
    - [ ] Output JSON standardizzato (uguale a Django: `user`, `ip`, `path`, `method`, `risk_score`).

### Fase 2: Active Defense (Protezione Attiva)
Porting dei meccanismi di difesa proattiva.

- [ ] **Rate Limiting**:
    - [ ] Integrazione `Bucket4j`.
    - [ ] Sliding Window algorithm su base IP o User.
- [ ] **IP Blocking**:
    - [ ] Caricamento lista IP bloccati/sospetti (Tor exit nodes locally or via API).
- [ ] **Security Headers**:
    - [ ] Set automatico di HSTS, X-Content-Type-Options, ecc. (se non gestiti da Spring Security).

### Fase 3: Crittografia & Integrità (Art. 21)
Strumenti per la protezione dei dati a riposo.

- [ ] **CryptoUtils**:
    - [ ] Helper per AES-256 (riutilizzando lo standard usato in Django per compatibilità).
    - [ ] `KeyRotationManager` stub.
- [ ] **Log Hashing**:
    - [ ] Calcolo SHA-256 di ogni log entry per garantire non-rifiuto (Chaining).

### Fase 4: Integrazione & Rilascio
- [ ] **Actuator Endpoints**:
    - [ ] Endpoint `/actuator/nis2` per stato conformità.
- [ ] **Integrazione Infrastruttura**:
    - [ ] Test con Docker Compose + Fluent Bit (deve ingerire i log Java senza modifiche).
- [ ] **Publishing**:
    - [ ] Deploy su Maven Central (o GitHub Packages inizialmente).

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
