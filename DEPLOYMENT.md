# DoccuFleet deployment on an Azure Ubuntu VM

This deployment keeps the existing local `docker-compose.yml` unchanged and uses separate production files:

- `docker-compose.prod.yml` for initial HTTP access by public IP;
- `docker-compose.https.yml` as an additional override after a domain points to the VM;
- `.env.prod` for deployment secrets, created from `.env.prod.example`.

The production routing model uses one public origin. The frontend and every API are served by Traefik from the same IP or domain:

- frontend: `/`;
- authentication API: `/api/auth/**`;
- fleet API: `/api/fleet/**`;
- document API: `/api/documents/**`;
- notification API: `/api/notifications/**`.

The frontend uses relative API paths by default. `VITE_API_BASE_URL` remains available for a future split-origin deployment but should be left empty for this setup.

## Repository audit

| Component | Technology | Production dependency |
| --- | --- | --- |
| `frontend` | React 19, TypeScript, Vite, Nginx | Traefik |
| `auth-service` | Spring Boot 3, Java 17 | Auth PostgreSQL, SMTP |
| `fleet-service` | Spring Boot 3, Java 17 | Fleet PostgreSQL |
| `document-service` | Spring Boot 3, Java 17, PDFBox, Tesseract, OpenAI | Document PostgreSQL, RabbitMQ, auth and fleet services |
| `traefik` | Traefik 3 | Docker provider |
| `rabbitmq` | RabbitMQ 3 | Persistent broker volume |

Dockerfiles exist at:

- `frontend/Dockerfile`;
- `auth-service/Dockerfile`;
- `fleet-service/Dockerfile`;
- `document-service/Dockerfile`.

Local-only values remain in the Vite development proxy, `application-local.properties`, OpenAPI generation scripts, and default application fallbacks. The production Compose file overrides database URLs, service URLs, frontend URL, CORS origins, storage locations, RabbitMQ connection details, secrets, and logging settings.

All Spring services accept `ALLOWED_ORIGINS` as a comma-separated list. CORS credentials are disabled because authentication uses an `Authorization: Bearer` header rather than browser cookies. In the recommended same-origin deployment, CORS is not required by the browser, but the explicit setting supports administrative tools or a future separate frontend.

## VM and network requirements

Minimum target:

- Ubuntu VM;
- 2 vCPU;
- 4 GiB RAM;
- enough disk space for Docker images, three databases, uploads, logs, and backups;
- Docker Engine and Docker Compose v2 installed.

Allow inbound traffic in the Azure Network Security Group:

- TCP 22, restricted to the administrator's IP where possible;
- TCP 80 from the internet;
- TCP 443 from the internet.

Do not create public rules for PostgreSQL `5432`, RabbitMQ `5672`/`15672`, or backend port `8080`. The production Compose file does not publish these ports.

The configured container memory ceilings total approximately 3.1 GiB, leaving space for Ubuntu and the Docker daemon. Auth and Fleet use a 384 MiB Java heap. Document Service uses a 512 MiB heap because OCR and PDF processing also need native memory. On a busy installation, 4 GiB remains a constrained configuration; monitor `docker stats` and upgrade the VM if the document service is killed with exit code 137.

## Initial HTTP deployment by public IP

From the repository root on the VM:

```bash
cp .env.prod.example .env.prod
nano .env.prod
chmod 600 .env.prod
```

Replace every placeholder. For initial IP testing:

```dotenv
FRONTEND_URL=http://YOUR_PUBLIC_IP
ALLOWED_ORIGINS=http://YOUR_PUBLIC_IP
VITE_API_BASE_URL=
```

`JWT_SECRET` must be the same strong random value for all three backend services. One way to generate it on Ubuntu is:

```bash
openssl rand -base64 48
```

Validate the resolved configuration:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod config
```

Because three Maven images are built on a small VM, limit build concurrency:

```bash
export COMPOSE_PARALLEL_LIMIT=1
```

Start the complete stack with the requested command:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Open `http://YOUR_PUBLIC_IP`. HTTPS certificates should not be enabled for a raw IP.

If the first build still exceeds available memory, build sequentially and then start:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod build auth-service
docker compose -f docker-compose.prod.yml --env-file .env.prod build fleet-service
docker compose -f docker-compose.prod.yml --env-file .env.prod build document-service
docker compose -f docker-compose.prod.yml --env-file .env.prod build frontend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

## Inspecting and stopping the stack

```bash
docker ps
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f
```

Stop containers while retaining every named volume:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod down
```

Never use `down -v` unless database, broker, document, and image data are intentionally being deleted.

## Switching to a domain and HTTPS

Create DNS records pointing to the VM public IP:

```text
A  @    PUBLIC_IP
A  www  PUBLIC_IP
```

An `api` record is not required because this deployment uses path-based APIs on the main domain. It can be created later only if the architecture is intentionally changed to `api.example.com`.

Update `.env.prod`:

```dotenv
APP_HOST=example.com
FRONTEND_URL=https://example.com
ALLOWED_ORIGINS=https://example.com,https://www.example.com
ACME_EMAIL=operations@example.com
VITE_API_BASE_URL=
```

Wait until DNS resolves publicly to the VM, then enable the HTTPS override:

```bash
export COMPOSE_PARALLEL_LIMIT=1
docker compose \
  -f docker-compose.prod.yml \
  -f docker-compose.https.yml \
  --env-file .env.prod \
  up -d --build
```

The override redirects domain-based HTTP requests to HTTPS and asks Let's Encrypt for certificates through the HTTP challenge. Direct-IP HTTP remains available for diagnosis. Port 80 must stay reachable for certificate issuance and renewal.

Use both Compose files for later HTTPS management commands, including `logs`, `ps`, and `down`.

## Persistent data and backups

Production uses separate named volumes for:

- Auth PostgreSQL;
- Fleet PostgreSQL;
- Document PostgreSQL;
- RabbitMQ;
- profile images;
- vehicle images;
- uploaded documents;
- Let's Encrypt state.

With project name `doccufleet-prod`, Docker normally prefixes volume names with `doccufleet-prod_`. Verify exact names with:

```bash
docker volume ls
```

Create database dumps regularly:

```bash
mkdir -p backups
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T auth-postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > backups/auth.sql
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T fleet-postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > backups/fleet.sql
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T document-postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > backups/document.sql
```

Database dumps do not contain uploaded files. Back up the profile-image, vehicle-image, and document-file volumes separately. Store copies outside the VM or in Azure Storage. Test restore procedures before relying on the backups.

The current three-database architecture is deliberately preserved. A single PostgreSQL container with three logical databases could save memory, but it increases migration and operational risk and is therefore only an optional future optimization.

## Environment variables

The complete safe template is `.env.prod.example`. Major groups are:

- public origin: `FRONTEND_URL`, `ALLOWED_ORIGINS`, optional `VITE_API_BASE_URL`;
- domain/TLS: `APP_HOST`, `ACME_EMAIL`;
- security: `JWT_SECRET`, bootstrap administrator credentials;
- three PostgreSQL database names, users, and passwords;
- SMTP connection and sender;
- OpenAI endpoint, model, timeout, token limit, and API key;
- RabbitMQ credentials;
- persistent storage paths;
- OCR language and DPI.

Do not put real secrets in source code, Compose labels, Docker build arguments, or committed environment examples.

## Known production risks

- The services currently use `spring.jpa.hibernate.ddl-auto=update`. This is retained to avoid changing database behavior, but controlled migrations such as Flyway are safer for a mature production system.
- Swagger/OpenAPI routes remain publicly reachable through their existing API prefixes because the current Spring Security rules permit them. Restrict them before storing sensitive production data if public API documentation is not desired.
- Traefik's Docker provider requires read access to `/var/run/docker.sock`. The mount is read-only, but access to the Docker API is still security-sensitive. Keep Traefik patched and do not run untrusted containers on the VM.
- A 4 GiB VM is suitable for a demonstration or light workload, not heavy concurrent OCR. Monitor memory and request latency.
- Published outbox records and RabbitMQ dead-letter messages require operational retention/cleanup policies as traffic grows.
- SMTP delivery depends on the provider and Azure outbound-network policy. Verify it separately before relying on invitation emails.

## Troubleshooting

### Frontend calls localhost

Inspect the browser Network tab. For this deployment, request URLs should begin with `/api/...` on the current origin. Ensure `VITE_API_BASE_URL` is empty, then rebuild `frontend` because Vite variables are embedded at build time.

### CORS errors

Set `ALLOWED_ORIGINS` to exact origins including scheme and port, separated by commas. Do not add paths or trailing slashes. Recreate the backend services after changing it. Same-origin requests through Traefik normally do not require CORS.

### Backend cannot connect to PostgreSQL

Check database health and confirm the backend URL uses the Compose service name, not `localhost`:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
docker compose -f docker-compose.prod.yml --env-file .env.prod logs auth-postgres auth-service
```

Changing a PostgreSQL password in `.env.prod` does not modify an already initialized database volume. Update the database role deliberately or restore into a newly initialized volume; do not delete a production volume as a shortcut.

### Traefik returns 404

Check that the target container is healthy and has `traefik.enable=true` labels:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod logs traefik
docker inspect doccufleet-prod-frontend-1
```

Container suffixes can vary; use `docker ps` to find the exact name.

### Ports 80 or 443 are unreachable

Check the Azure Network Security Group, the Ubuntu firewall, and whether another process already owns the port:

```bash
sudo ss -lntp | grep -E ':80|:443'
sudo ufw status
```

### RabbitMQ or parsing is unavailable

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod ps rabbitmq document-service
docker compose -f docker-compose.prod.yml --env-file .env.prod logs rabbitmq document-service
```

Messages that fail all retries are retained in `doccufleet.document.parsing.dlq` and should be inspected before redelivery.

### Insufficient memory

```bash
free -h
docker stats
docker inspect CONTAINER_NAME --format '{{.State.OOMKilled}} {{.State.ExitCode}}'
```

Exit code 137 or `OOMKilled=true` indicates memory pressure. Stop unrelated workloads, build images sequentially, or resize the VM. Do not blindly increase Java heap sizes on a 4 GiB host.
