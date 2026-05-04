# API Endpoint Reference

This document describes the current `auth-service` and `fleet-service` APIs exposed through Traefik.

## Base URL
- `http://localhost` (through Traefik)

## OpenAPI / Swagger
- Auth Swagger UI: `http://localhost/api/auth/swagger-ui/index.html`
- Auth OpenAPI JSON: `http://localhost/api/auth/v3/api-docs`
- Auth OpenAPI YAML: `http://localhost/api/auth/v3/api-docs.yaml`
- Versioned spec in repo: `auth-service/openapi.yaml`
- Fleet Swagger UI: `http://localhost/api/fleet/swagger-ui/index.html`
- Fleet OpenAPI JSON: `http://localhost/api/fleet/v3/api-docs`

To regenerate the versioned spec:

```powershell
cd auth-service
./scripts/generate-openapi.ps1
```

```bash
cd auth-service
./scripts/generate-openapi.sh
```

Fleet Service also keeps a versioned spec in `fleet-service/openapi.yaml`.

To regenerate it:

```powershell
cd fleet-service
.\scripts\generate-openapi.ps1
```

```bash
cd fleet-service
./scripts/generate-openapi.sh
```

## Auth Endpoints

### Register
```http
POST /api/auth/register
Content-Type: application/json
```

Request:
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123",
  "phone": "+40123456789",
  "address": "Main Street 1"
}
```

Success (`201 Created`):
```json
{
  "userId": 1,
  "username": "alice",
  "email": "alice@example.com",
  "phone": "+40123456789",
  "address": "Main Street 1",
  "role": "USER"
}
```

### Login
```http
POST /api/auth/login
Content-Type: application/json
```

Request:
```json
{
  "username": "alice",
  "password": "password123"
}
```

Success (`200 OK`):
```json
{
  "token": "<jwt>",
  "username": "alice",
  "role": "USER"
}
```

The JWT keeps the existing single `role` claim and also includes a `roles` array. For database-backed users it includes `userId`.

### Current User (Protected)
```http
GET /api/auth/users/me
Authorization: Bearer <jwt>
```

Success (`200 OK`): same schema as register response.

### Update User Role (Admin Only)
```http
PUT /api/auth/admin/users/{id}/roles
Authorization: Bearer <jwt>
Content-Type: application/json
```

Request:
```json
{
  "role": "ADMIN"
}
```

Success (`200 OK`): returns updated `MeResponse`.

## Standard Error Payload
```json
{
  "message": "Authentication required"
}
```

Common status codes:
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `409 Conflict`

## Fleet Endpoints

Gateway base path: `http://localhost/api/fleet`

The Fleet Service owns vehicle registry data only. It does not manage documents, parsing, compliance, or notifications.

Roles:
- `ADMIN`, `FLEET_MANAGER`: create, update, assign, change status, read all.
- `ADMIN`: delete.
- `AUDITOR`: read all.
- `EMPLOYEE` or current auth-service `USER`: read assigned vehicles only when the JWT contains a numeric `userId` claim or numeric subject.

The service accepts JWTs with either a `roles` claim or the current auth-service `role` claim.

### Create Vehicle
```http
POST /api/fleet/vehicles
Authorization: Bearer <jwt>
Content-Type: application/json
```

Request:
```json
{
  "licensePlate": "B-123-ABC",
  "vin": "1HGCM82633A004352",
  "brand": "Toyota",
  "model": "Corolla",
  "manufactureYear": 2022,
  "vehicleType": "CAR",
  "fuelType": "HYBRID",
  "ownershipType": "OWNED",
  "department": "Operations",
  "assignedUserId": 12,
  "assignedDriverName": "Alex Ionescu",
  "currentMileage": 25000
}
```

Success (`201 Created`): returns the vehicle. `status` defaults to `ACTIVE` if omitted.

### List Vehicles
```http
GET /api/fleet/vehicles?status=ACTIVE&vehicleType=CAR&department=Operations
Authorization: Bearer <jwt>
```

Supported filters: `status`, `vehicleType`, `fuelType`, `ownershipType`, `department`, `assignedUserId`, `licensePlate`.

### Get Vehicle
```http
GET /api/fleet/vehicles/{id}
Authorization: Bearer <jwt>
```

### Update Vehicle
```http
PUT /api/fleet/vehicles/{id}
Authorization: Bearer <jwt>
Content-Type: application/json
```

Uses the same payload shape as create.

### Change Status
```http
PATCH /api/fleet/vehicles/{id}/status
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "status": "IN_SERVICE"
}
```

### Assign Vehicle
```http
PATCH /api/fleet/vehicles/{id}/assignment
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "assignedUserId": 12,
  "assignedDriverName": "Alex Ionescu",
  "department": "Operations"
}
```

### Delete Vehicle
```http
DELETE /api/fleet/vehicles/{id}
Authorization: Bearer <jwt>
```

Success: `204 No Content`.

### Internal Vehicle Lookups
```http
GET /api/fleet/internal/vehicles/{id}/exists
GET /api/fleet/internal/vehicles/{id}/basic-info
GET /api/fleet/internal/vehicles/active
Authorization: Bearer <jwt>
```

Internal endpoints require `ADMIN`, `FLEET_MANAGER`, or `AUDITOR`.

## Run

```powershell
docker compose up --build
```

Auth Service is exposed at existing auth routes. Fleet Service is exposed through Traefik under `/api/fleet/**` and uses its own PostgreSQL database on the Docker network. For local non-Docker work:

```powershell
cd fleet-service
$env:JWT_SECRET="96394670290123498739012349851901"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```
