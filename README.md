# Fleet Manager

Fleet Manager is a multi-tenant fleet and vehicle-document management application. It combines a React frontend with three Spring Boot services, separate PostgreSQL databases, JWT authentication, and Traefik routing.

## Features

- Organization and user administration
- Invitation-based account activation and SMTP email delivery
- Roles: `SUPERADMIN`, `BUSINESS_ADMIN`, and `EMPLOYEE`
- Tenant-isolated vehicle and document data
- Vehicle CRUD, filtering, assignment, status changes, and image storage
- PDF and image document uploads
- PDF text extraction with OCR fallback
- OpenAI-assisted document classification and structured-data extraction
- Manual document approval, rejection, and archival
- Approved document data and vehicle compliance information
- Expiration alerts and in-app notifications
- Paginated document history and PDF history export
- User profiles and profile images
- OpenAPI/Swagger documentation

Public self-registration is disabled. Users are created by an administrator and activate their accounts through an invitation.

## Technology

- Java 17 and Spring Boot 3.3
- Spring Security, JWT, Spring Data JPA, and Spring Mail
- PostgreSQL 16
- React 19, TypeScript, and Vite
- Traefik 3
- PDFBox, Tesseract OCR, and the OpenAI API
- Docker Compose

## Repository Layout

```text
.
|-- auth-service/       Authentication, organizations, users, and invitations
|-- fleet-service/      Vehicles, assignments, and vehicle images
|-- document-service/   Documents, extraction, review, alerts, and notifications
|-- frontend/           React single-page application
|-- traefik/            Gateway routing and middleware configuration
|-- scripts/            Demo-data instructions
|-- mocked-documents/   Example documents for local testing
`-- docker-compose.yml  Complete local stack
```

Each backend service owns a separate PostgreSQL database. Uploaded files are stored in Docker named volumes. Traefik exposes the frontend and APIs through port 80.

RabbitMQ carries durable document parsing jobs. Uploads and their outbox records are committed together in PostgreSQL, an outbox publisher sends confirmed messages to RabbitMQ, and `document-service` consumes them asynchronously. Failed deliveries are retried three times and then routed to `doccufleet.document.parsing.dlq`.

## Access Model

- `SUPERADMIN` can manage all organizations and access data across tenants.
- `BUSINESS_ADMIN` can manage users, vehicles, and documents belonging to their organization.
- `EMPLOYEE` has authenticated, organization-scoped access to the vehicles and documents available to them.

The legacy role names `ADMIN`, `STAFF`, and `USER` are accepted by the backend as aliases, but new code and API responses use the canonical roles above.

## Prerequisites

- Docker Desktop with Docker Compose
- An OpenAI API key for document extraction
- SMTP credentials for invitation emails

Java 17, Maven, Node.js, and npm are only required when running components outside Docker.

## Configuration

Create a `.env` file in the repository root. The file is ignored by Git.

The following values are required by `docker-compose.yml`:

```dotenv
JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
BOOTSTRAP_ADMIN_PASSWORD=replace-with-a-strong-admin-password

AUTH_DB_PASSWORD=replace-with-an-auth-db-password
FLEET_DB_PASSWORD=replace-with-a-fleet-db-password
DOCUMENT_DB_PASSWORD=replace-with-a-document-db-password

MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=replace-with-your-smtp-user
MAIL_PASSWORD=replace-with-your-smtp-password
MAIL_FROM=fleet-manager@example.com

OPENAI_API_KEY=replace-with-your-openai-api-key
```

Common optional overrides:

```dotenv
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
FRONTEND_URL=http://localhost
INVITATION_EXPIRY_HOURS=24

AUTH_DB_NAME=auth_deploy
AUTH_DB_USER=auth_user
AUTH_DB_HOST_PORT=5432
FLEET_DB_NAME=fleet_deploy
FLEET_DB_USER=fleet_user
FLEET_DB_HOST_PORT=5433
DOCUMENT_DB_NAME=document_deploy
DOCUMENT_DB_USER=document_user
DOCUMENT_DB_HOST_PORT=5434

OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-5.4-mini
OPENAI_TIMEOUT=60s
OPENAI_MAX_OUTPUT_TOKENS=1500
OPENAI_TEMPERATURE=0

MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_DEBUG=false

RABBITMQ_DEFAULT_USER=fleet
RABBITMQ_DEFAULT_PASS=fleet
RABBITMQ_AMQP_HOST_PORT=5672
RABBITMQ_MANAGEMENT_HOST_PORT=15672
```

For local, non-Docker service runs, the application property files also support storage and parser settings such as `PROFILE_IMAGE_STORAGE_PATH`, `VEHICLE_IMAGE_STORAGE_PATH`, `DOCUMENT_STORAGE_PATH`, and `DOCUMENT_OCR_*`.

## Run with Docker

Build and start the complete stack:

```powershell
docker compose up -d --build
```

Check container health and status:

```powershell
docker compose ps
```

Open the application:

```text
http://localhost
```

Sign in with `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD`.

Useful log commands:

```powershell
docker compose logs -f frontend
docker compose logs -f auth-service
docker compose logs -f fleet-service
docker compose logs -f document-service
docker compose logs -f traefik
```

Stop the stack:

```powershell
docker compose down
```

To also delete the local database and file-storage volumes:

```powershell
docker compose down -v
```

## Local Ports

| Component | Address |
| --- | --- |
| Application and API gateway | `http://localhost` |
| Traefik dashboard | `http://localhost:8081/dashboard/` |
| Auth PostgreSQL | `localhost:5432` |
| Fleet PostgreSQL | `localhost:5433` |
| Document PostgreSQL | `localhost:5434` |
| RabbitMQ AMQP | `localhost:5672` |
| RabbitMQ management UI | `http://localhost:15672` |

Database and RabbitMQ ports bind to `127.0.0.1` only. Port values can be changed through the corresponding `.env` variables.

## API Overview

All application API calls use the Traefik gateway:

- Auth and administration: `/api/auth/**`
- Fleet: `/api/fleet/**`
- Documents: `/api/documents/**`
- Notifications: `/api/notifications/**`

Except for login, invitation validation, invitation acceptance, and Swagger resources, endpoints require:

```http
Authorization: Bearer <jwt>
```

### Auth and Organization Endpoints

Representative routes:

- `POST /api/auth/login`
- `POST /api/auth/accept-invite`
- `GET /api/auth/invitations/validate`
- `GET /api/auth/users/me`
- `PUT /api/auth/users/me`
- `POST /api/auth/users/me/profile-image`
- `GET /api/auth/businesses`
- `POST /api/auth/businesses`
- `GET /api/auth/businesses/{businessId}/users`
- `POST /api/auth/businesses/{businessId}/users`
- `PUT /api/auth/businesses/{businessId}/users/{userId}/role`
- `PATCH /api/auth/admin/users/{userId}/status`
- `POST /api/auth/admin/users/{userId}/resend-invite`

### Fleet Endpoints

- `POST /api/fleet/vehicles`
- `GET /api/fleet/vehicles`
- `GET /api/fleet/vehicles/{id}`
- `PUT /api/fleet/vehicles/{id}`
- `PATCH /api/fleet/vehicles/{id}/status`
- `PATCH /api/fleet/vehicles/{id}/assignment`
- `DELETE /api/fleet/vehicles/{id}`
- `POST /api/fleet/vehicles/{id}/image`
- `GET /api/fleet/vehicles/{id}/image`
- `DELETE /api/fleet/vehicles/{id}/image`

Vehicle listing supports filters for status, vehicle type, fuel type, ownership type, department, assigned user, license plate, and organization where permitted.

### Document Endpoints

- `POST /api/documents`
- `GET /api/documents?vehicleId={vehicleId}`
- `GET /api/documents/{id}`
- `GET /api/documents/{id}/download`
- `GET /api/documents/{id}/info-folder`
- `GET /api/documents/history`
- `GET /api/documents/history/export`
- `GET /api/documents/review-queue`
- `POST /api/documents/{id}/approve`
- `POST /api/documents/{id}/reject`
- `POST /api/documents/{id}/review`
- `POST /api/documents/{id}/archive`
- `GET /api/documents/vehicles/{vehicleId}/attributes`
- `GET /api/documents/alerts/vehicles`

The service accepts PDF, JPG, JPEG, and PNG uploads. It detects the document type from the extracted content; clients do not need to supply `documentType` during upload.

### Notification Endpoints

- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `PATCH /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`

The generated OpenAPI specifications are the authoritative source for request fields, responses, and the complete route list.

## Document Upload Example

Set a token:

```bash
TOKEN="<jwt>"
```

Upload a document:

```bash
curl -X POST "http://localhost/api/documents" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@inspection.pdf;type=application/pdf" \
  -F "vehicleId=1"
```

The endpoint returns `202 Accepted` after the document and its parsing outbox event are stored. RabbitMQ then dispatches the extraction job asynchronously. Documents that cannot be accepted automatically are made available to authorized administrators for review.

List documents for a vehicle:

```bash
curl "http://localhost/api/documents?vehicleId=1" \
  -H "Authorization: Bearer $TOKEN"
```

Download a document:

```bash
curl -L "http://localhost/api/documents/<document-id>/download" \
  -H "Authorization: Bearer $TOKEN" \
  -o document.pdf
```

## Swagger and OpenAPI

| Service | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| Auth | `http://localhost/api/auth/swagger-ui/index.html` | `http://localhost/api/auth/v3/api-docs` |
| Fleet | `http://localhost/api/fleet/swagger-ui/index.html` | `http://localhost/api/fleet/v3/api-docs` |
| Documents | `http://localhost/api/documents/swagger-ui/index.html` | `http://localhost/api/documents/v3/api-docs` |

Regenerate the checked-in specifications:

```powershell
.\auth-service\scripts\generate-openapi.ps1
.\fleet-service\scripts\generate-openapi.ps1
.\document-service\scripts\generate-openapi.ps1
```

Generated files:

- `auth-service/openapi.yaml`
- `fleet-service/openapi.yaml`
- `document-service/openapi.yaml`

## Development

Run the frontend locally:

```powershell
cd frontend
npm install
npm run dev
```

Build and lint it:

```powershell
npm run build
npm run lint
```

Run backend tests from the repository root:

```powershell
.\auth-service\mvnw.cmd -f auth-service\pom.xml test
.\fleet-service\mvnw.cmd -f fleet-service\pom.xml test
.\document-service\mvnw.cmd -f document-service\pom.xml test
```

Alternatively, enter a service directory and run `.\mvnw.cmd test`.

## Demo Data

Demo seed scripts are available under each service's `scripts/` directory. See `scripts/seed-demo-multitenant.md` for the coordinated multi-tenant seed procedure.

## Current Scope

The local stack uses plain HTTP. Production TLS/ACME setup and maintenance scheduling are not currently implemented.
