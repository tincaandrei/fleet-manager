# Fleet Manager Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT APPLICATIONS                         │
│  (Frontend, Mobile, Third-party APIs)                              │
└─────────────────┬───────────────────────────┬───────────────────────┘
                  │                           │
                  ▼                           ▼
        ┌──────────────────────────────────────────────┐
        │     PUBLIC INTERNET / LOCAL NETWORK          │
        │  http://api.localhost  (port 80)             │
        │  https://api.localhost (port 443)            │
        └──────────────────┬───────────────────────────┘
                           │
        ┌──────────────────▼───────────────────┐
        │     TRAEFIK API GATEWAY (Port 80/443)│
        │  ┌──────────────────────────────────┐│
        │  │ - Route to services              ││
        │  │ - Load Balancing                 ││
        │  │ - HTTPS/TLS Termination          ││
        │  │ - HTTP to HTTPS redirect         ││
        │  │ - Rate Limiting (100 req/min)    ││
        │  └──────────────────────────────────┘│
        └──────────────────┬───────────────────┘
                           │
        ┌──────────────────▼───────────────────┐
        │      MIDDLEWARE CHAIN                 │
        │  ┌──────────────────────────────────┐│
        │  │ 1. JWT Authentication             ││
        │  │ 2. CORS Headers                   ││
        │  │ 3. Request Headers                ││
        │  │ 4. Compression                    ││
        │  │ 5. Retry Logic                    ││
        │  └──────────────────────────────────┘│
        └──────────────────┬───────────────────┘
                           │
     ┌─────────────────────┼─────────────────────┐
     │                     │                     │
     ▼                     ▼                     ▼
┌────────────┐        ┌────────────┐      ┌────────────┐
│ AUTH       │        │ FLEET      │      │ DOCUMENT   │
│ SERVICE    │        │ SERVICE    │      │ SERVICE    │
│ :8080      │        │ :8080      │      │ :8080      │
│            │        │ (Future)   │      │ (Future)   │
│ /auth/*    │        │ /fleet/*   │      │ /docs/*    │
└────┬───────┘        └────┬───────┘      └────┬───────┘
     │                     │                    │
     ▼                     ▼                    ▼
  ┌────────┐           ┌────────┐           ┌────────┐
  │ AUTH   │           │ FLEET  │           │ DOC    │
  │DATABASE│           │DATABASE│           │DATABASE│
  │ (PG)   │           │(Future)│           │(Future)│
  └────────┘           └────────┘           └────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                      TRAEFIK DASHBOARD                               │
│                  (Monitoring & Configuration)                        │
│  http://localhost:8081 (User: admin, Pass: admin123)               │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                      DOCKER NETWORK: fleet-net                       │
│  All containers communicate securely via internal network            │
└──────────────────────────────────────────────────────────────────────┘
```

## Request Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│ CLIENT REQUEST                                                      │
│ GET http://api.localhost/auth/profile                              │
│ Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...      │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
            ┌─────────────────────────┐
            │ TRAEFIK ROUTING         │
            │ Matches:                │
            │ Host: api.localhost     │
            │ Path: /auth/profile     │
            └────────────┬────────────┘
                         │
                         ▼
           ┌──────────────────────────────┐
           │ MIDDLEWARE EXECUTION (Order) │
           │ 1. ✓ Compression            │
           │ 2. ✓ Rate Limiting          │
           │ 3. ✓ JWT Validation         │
           │ 4. ✓ CORS Headers           │
           │ 5. ✓ Request Headers        │
           └────────────┬─────────────────┘
                        │
        ┌───────────────┴───────────────┐
        │ JWT VALIDATION SUCCESS        │
        │ ✓ Token valid                 │
        │ ✓ Not expired                 │
        │ ✓ User authenticated          │
        └────────────┬────────────────┬─┘
                     │                │
                     ▼ (Pass)      (Fail)
            ┌──────────────────┐  ✗ 401 Unauthorized
            │ ROUTE TO SERVICE │
            │ auth-service:8080│
            └────────┬─────────┘
                     │
                     ▼
            ┌──────────────────┐
            │ AUTH SERVICE     │
            │ Processing...    │
            │ Fetching profile │
            └────────┬─────────┘
                     │
                     ▼
            ┌──────────────────┐
            │ AUTH DATABASE    │
            │ Query: SELECT... │
            └────────┬─────────┘
                     │
                     ▼
            ┌──────────────────┐
            │ RESPONSE 200 OK  │
            │ {user data}      │
            └────────┬─────────┘
                     │
                     ▼
            ┌──────────────────────────┐
            │ RESPONSE TO CLIENT       │
            │ HTTP/1.1 200 OK          │
            │ X-Frame-Options: SAME... │
            │ X-Content-Type: nosniff  │
            │ Content-Encoding: gzip   │
            │ {user data compressed}   │
            └──────────────────────────┘
```

## Service Configuration Pattern

```
For Each Microservice:

┌─────────────────────────────────────────────┐
│ 1. Docker Compose Definition                │
│                                             │
│ service-name:                               │
│   build: ./service-name                     │
│   image: fleet/service-name:local           │
│   networks:                                 │
│     - fleet-net                             │
│   labels:                                   │
│     - traefik.enable=true                   │
│     - traefik.http.routers...               │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│ 2. Docker Labels (Auto-discovery)           │
│                                             │
│ - traefik.enable=true                       │
│ - traefik.http.routers.service.rule=...     │
│ - traefik.http.services.service.loadbal...  │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│ 3. Traefik Dynamic Config                   │
│                                             │
│ routers:                                    │
│   service-name:                             │
│     rule: Host(...) && PathPrefix(...)      │
│     middlewares:                            │
│       - jwt-auth                            │
│       - rate-limit                          │
│                                             │
│ services:                                   │
│   service-name:                             │
│     loadBalancer:                           │
│       servers:                              │
│         - url: http://service-name:8080     │
└─────────────────────────────────────────────┘
```

## Middleware Order and Purpose

```
Incoming Request
       │
       ▼
┌──────────────────────────────────────────┐
│ 1. COMPRESSION MIDDLEWARE                │
│    Purpose: Compress response (gzip)     │
│    Condition: Response > 1KB              │
│    Status: ALWAYS APPLIED                │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 2. RATE LIMITING                         │
│    Purpose: Limit requests               │
│    Limit: 100 req/min per IP              │
│    Status: ALWAYS APPLIED                │
│    On Limit: Return 429 Too Many Requests│
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 3. JWT AUTHENTICATION                    │
│    Purpose: Validate JWT token           │
│    Required: Authorization header        │
│    Exception: /auth/login, /auth/register│
│    On Fail: Return 401 Unauthorized      │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 4. CORS HEADERS                          │
│    Purpose: Allow cross-origin requests  │
│    Headers: Access-Control-Allow-*       │
│    Status: ALWAYS APPLIED                │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 5. REQUEST HEADERS                       │
│    Purpose: Add security headers         │
│    X-Frame-Options: SAMEORIGIN           │
│    X-Content-Type: nosniff               │
│    X-XSS-Protection: 1; mode=block       │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 6. RETRY LOGIC                           │
│    Purpose: Retry on failure             │
│    Attempts: 3                           │
│    Interval: 100ms                       │
└──────────────────────────────────────────┘
       │
       ▼
    SERVICE
       │
       ▼
   RESPONSE
```

## Deployment Topology

```
LOCAL DEVELOPMENT:
│
├─ Host Machine
│  ├─ Port 80   → Traefik (HTTP)
│  ├─ Port 443  → Traefik (HTTPS)
│  ├─ Port 8081 → Traefik Dashboard
│  │
│  └─ Docker Network: fleet-net
│     ├─ Container: traefik
│     ├─ Container: auth-service
│     ├─ Container: auth-postgres
│     ├─ Container: fleet-service (future)
│     ├─ Container: fleet-database (future)
│     ├─ Container: document-service (future)
│     └─ Container: document-database (future)
│
└─ Hosts File: 127.0.0.1 api.localhost, traefik.localhost


PRODUCTION:
│
├─ Load Balancer (External)
│  │
│  └─ Domain: api.example.com
│     │
│     ├─ Traefik Cluster
│     │  ├─ Node 1 (Port 80/443)
│     │  ├─ Node 2 (Port 80/443)
│     │  └─ Node 3 (Port 80/443)
│     │
│     ├─ Service Cluster
│     │  ├─ Auth Service (Replicas: 3)
│     │  ├─ Fleet Service (Replicas: 3)
│     │  └─ Document Service (Replicas: 2)
│     │
│     └─ Database Cluster
│        ├─ PostgreSQL Auth (Primary + Replicas)
│        ├─ PostgreSQL Fleet (Primary + Replicas)
│        └─ PostgreSQL Documents (Primary + Replicas)
```

## File Organization

```
fleet manager/
│
├─ Root Configuration
│  ├─ docker-compose.yml          (Main orchestration file)
│  ├─ .env.example                (Environment template)
│  ├─ TRAEFIK_SETUP.md            (This summary)
│  ├─ API_REFERENCE.md            (API documentation)
│  ├─ setup-traefik.sh            (Linux/Mac setup)
│  └─ setup-traefik.bat           (Windows setup)
│
├─ Traefik Configuration
│  ├─ traefik/traefik.yml         (Static config)
│  ├─ traefik/dynamic_conf.yml    (Dynamic routes)
│  ├─ traefik/README.md           (Detailed docs)
│  ├─ traefik/MIGRATION.md        (Migration guide)
│  ├─ traefik/acme.json           (ACME certificates)
│  └─ traefik/certs/
│     ├─ cert.pem                 (Self-signed cert)
│     └─ key.pem                  (Private key)
│
├─ Auth Service
│  ├─ auth-service/
│  │  ├─ Dockerfile
│  │  ├─ pom.xml
│  │  ├─ src/
│  │  │  ├─ main/java/com/fleet/auth/
│  │  │  │  ├─ config/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ service/
│  │  │  │  ├─ repository/
│  │  │  │  ├─ entity/
│  │  │  │  └─ ...
│  │  │  └─ main/resources/
│  │  │     └─ application-*.properties
│  │  └─ target/
│  │
│  └─ (docker-compose.yml was moved to root)
│
├─ Fleet Service (Future)
│  └─ fleet-service/
│     ├─ Dockerfile
│     ├─ pom.xml
│     ├─ src/...
│     └─ ...
│
└─ Document Service (Future)
   └─ document-service/
      ├─ Dockerfile
      ├─ pom.xml
      ├─ src/...
      └─ ...
```

---

**Generated on**: April 18, 2026  
**Version**: 1.0.0  
**Status**: Ready for Use
