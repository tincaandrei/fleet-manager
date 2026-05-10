# Fleet Manager

Microservice-based fleet management backend built with Spring Boot 3, PostgreSQL, JWT security, and Traefik as the API gateway.

## Implemented Services

### Auth Service

Path: `auth-service/`

Implemented features:
- User registration and login
- JWT generation and validation
- BCrypt password hashing
- Role model with `USER`, `STAFF`, and `ADMIN`
- Bootstrap admin account from environment variables
- Current-user profile endpoint
- Admin-only user role update endpoint
- PostgreSQL persistence with Spring Data JPA
- OpenAPI/Swagger documentation

Gateway routes:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/users/me`
- `PUT /api/auth/admin/users/{id}/roles`

### Fleet Service

Path: `fleet-service/`

Implemented features:
- Vehicle registry CRUD
- Vehicle filtering by status, type, fuel type, ownership type, department, assigned user, and license plate
- Vehicle assignment updates
- Vehicle status updates
- Internal vehicle lookup endpoints
- JWT validation using the shared auth secret
- Role-based access control
- Dedicated PostgreSQL database
- OpenAPI/Swagger documentation

Gateway routes:
- `POST /api/fleet/vehicles`
- `GET /api/fleet/vehicles`
- `GET /api/fleet/vehicles/{id}`
- `PUT /api/fleet/vehicles/{id}`
- `PATCH /api/fleet/vehicles/{id}/status`
- `PATCH /api/fleet/vehicles/{id}/assignment`
- `DELETE /api/fleet/vehicles/{id}`
- `GET /api/fleet/internal/vehicles/{id}/exists`
- `GET /api/fleet/internal/vehicles/{id}/basic-info`
- `GET /api/fleet/internal/vehicles/active`

### Document Service

Path: `document-service/`

Implemented features:
- PDF document upload for vehicles
- Vehicle existence validation through Fleet Service
- Local file storage configured by `DOCUMENT_STORAGE_PATH`
- Local Python parser script called via `ProcessBuilder` — no separate parser service or message queue
- Raw extracted data stored as JSONB in `document_extractions`
- Staff/Admin review queue
- `FAILED_PARSING` status when the Python parser fails
- Approve as-is, approve with edits, reject, and archive flows
- Approved document data stored separately as JSONB
- Dedicated PostgreSQL database
- OpenAPI/Swagger documentation

Parser notes:
- Script: `document-service/parser/parse_inspection_pdf.py` (requires `pypdf==5.1.0`)
- Test PDFs must contain selectable/extractable text — scanned image PDFs will not yield extracted fields
- Parser environment variables (Docker image sets these automatically):
  - `DOCUMENT_PARSER_PYTHON` — Python executable (default in Docker: `/opt/document-parser-venv/bin/python`)
  - `DOCUMENT_PARSER_SCRIPT_PATH` — path to the parser script (default in Docker: `/app/parser/parse_inspection_pdf.py`)
  - `DOCUMENT_PARSER_TIMEOUT_SECONDS` — kill timeout in seconds (default: `30`)

Gateway routes:
- `POST /api/documents`
- `GET /api/documents?vehicleId={vehicleId}`
- `GET /api/documents/{id}`
- `GET /api/documents/{id}/download`
- `GET /api/documents/review-queue`
- `POST /api/documents/{id}/review`
- `PATCH /api/documents/{id}/archive`

## Infrastructure

Implemented Docker stack:
- `traefik` API gateway on port `80`
- Traefik dashboard on port `8081`
- `auth-service`
- `auth-postgres`
- `fleet-service`
- `fleet-postgres`
- `document-service`
- `document-postgres`
- Shared Docker network: `fleet-net`

PostgreSQL ports are bound to localhost only:
- Auth DB: `127.0.0.1:5432`
- Fleet DB: `127.0.0.1:5433`
- Document DB: `127.0.0.1:5434`

Traefik middlewares:
- CORS headers
- Rate limiting
- Security headers
- Compression
- Retry middleware
- Fleet prefix stripping from `/api/fleet`

## Swagger

Auth:
- Swagger UI: `http://localhost/api/auth/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost/api/auth/v3/api-docs`
- OpenAPI YAML: `http://localhost/api/auth/v3/api-docs.yaml`

Fleet:
- Swagger UI: `http://localhost/api/fleet/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost/api/fleet/v3/api-docs`

Documents:
- Swagger UI: `http://localhost/api/documents/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost/api/documents/v3/api-docs`

Traefik dashboard:
- `http://localhost:8081/dashboard/`

## Configuration

Copy the example environment file:

```powershell
Copy-Item .env.example .env
```

Required values:
- `JWT_SECRET`
- `BOOTSTRAP_ADMIN_USERNAME`
- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_PASSWORD`
- `AUTH_DB_NAME`
- `AUTH_DB_USER`
- `AUTH_DB_PASSWORD`
- `AUTH_DB_HOST_PORT`
- `FLEET_DB_NAME`
- `FLEET_DB_USER`
- `FLEET_DB_PASSWORD`
- `FLEET_DB_HOST_PORT`
- `DOCUMENT_DB_NAME`
- `DOCUMENT_DB_USER`
- `DOCUMENT_DB_PASSWORD`
- `DOCUMENT_DB_HOST_PORT`
- `DOCUMENT_STORAGE_PATH`

The local `.env` file is ignored by Git.

## Run

Start everything:

```powershell
docker compose up -d --build
```

Check containers:

```powershell
docker compose ps
```

View logs:

```powershell
docker compose logs -f auth-service
docker compose logs -f fleet-service
docker compose logs -f document-service
docker compose logs -f traefik
```

Stop everything:

```powershell
docker compose down
```

## Local Database Access

Auth database:

```text
Host: localhost
Port: 5432
Database: auth_deploy
User: value of AUTH_DB_USER
Password: value of AUTH_DB_PASSWORD
```

Fleet database:

```text
Host: localhost
Port: 5433
Database: fleet_deploy
User: value of FLEET_DB_USER
Password: value of FLEET_DB_PASSWORD
```

Document database:

```text
Host: localhost
Port: 5434
Database: document_deploy
User: value of DOCUMENT_DB_USER
Password: value of DOCUMENT_DB_PASSWORD
```

## Document Service Curl Examples

Set a token first:

```bash
TOKEN="<jwt>"
```

Upload a PDF:

```bash
curl -X POST "http://localhost/api/documents" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@inspection.pdf;type=application/pdf" \
  -F "vehicleId=1" \
  -F "documentType=INSPECTION"
```

List vehicle documents:

```bash
curl "http://localhost/api/documents?vehicleId=1" \
  -H "Authorization: Bearer $TOKEN"
```

Get the review queue:

```bash
curl "http://localhost/api/documents/review-queue" \
  -H "Authorization: Bearer $TOKEN"
```

Approve a document:

```bash
curl -X POST "http://localhost/api/documents/<document-id>/review" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVE",
    "approvedData": {
      "documentType": "INSPECTION",
      "inspectionNumber": "MOCK-ITP-2026-001",
      "expiryDate": "2027-03-10"
    },
    "comment": "Approved after review"
  }'
```

Reject a document:

```bash
curl -X POST "http://localhost/api/documents/<document-id>/review" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "REJECT",
    "comment": "Uploaded document is not readable"
  }'
```

Download a document:

```bash
curl -L "http://localhost/api/documents/<document-id>/download" \
  -H "Authorization: Bearer $TOKEN" \
  -o document.pdf
```

## Tests

Auth service currently includes tests for:
- JWT service behavior
- Role entity behavior
- Auth integration flow
- Normalized auth repository behavior

Run auth tests:

```powershell
cd auth-service
.\mvnw.cmd test
```

## OpenAPI Generation

Auth:

```powershell
cd auth-service
.\scripts\generate-openapi.ps1
```

Fleet:

```powershell
cd fleet-service
.\scripts\generate-openapi.ps1
```

Generated specs are kept in:
- `auth-service/openapi.yaml`
- `fleet-service/openapi.yaml`
- `document-service/openapi.yaml`

## Not Implemented Yet

- Frontend application
- Maintenance scheduling
- Notifications
- Production TLS/ACME setup
