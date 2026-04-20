# API Endpoint Reference

This document describes the current `auth-service` API exposed through Traefik.

## Base URL
- `http://localhost` (through Traefik)
- Direct service URL (when needed): `http://localhost:8080`

## OpenAPI / Swagger
- Swagger UI: `http://localhost/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost/v3/api-docs`
- OpenAPI YAML: `http://localhost/v3/api-docs.yaml`
- Versioned spec in repo: `auth-service/openapi.yaml`

To regenerate the versioned spec:

```powershell
cd auth-service
./scripts/generate-openapi.ps1
```

```bash
cd auth-service
./scripts/generate-openapi.sh
```

## Endpoints

### Register
```http
POST /auth/register
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
POST /auth/login
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

### Current User (Protected)
```http
GET /users/me
Authorization: Bearer <jwt>
```

Success (`200 OK`): same schema as register response.

### Update User Role (Admin Only)
```http
PUT /admin/users/{id}/roles
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
