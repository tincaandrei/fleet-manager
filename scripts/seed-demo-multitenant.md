# Demo Multi-Tenant Seed

These scripts populate demo data for the current multi-tenant model:

- 3 businesses
- business admins and employees for each business
- vehicles assigned to those businesses and some employees
- no documents

All seeded demo users use this password:

```text
Password123!
```

Run the auth seed first, then the fleet seed.

```powershell
docker cp auth-service/scripts/seed-demo-multitenant.sql auth-postgres:/tmp/seed-demo-multitenant.sql
docker exec -it auth-postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f /tmp/seed-demo-multitenant.sql'

docker cp fleet-service/scripts/seed-demo-multitenant.sql fleet-postgres:/tmp/seed-demo-multitenant.sql
docker exec -it fleet-postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f /tmp/seed-demo-multitenant.sql'
```

Seeded logins:

```text
atlas.admin / Password123!
atlas.employee / Password123!
atlas.dispatch / Password123!
nova.admin / Password123!
nova.employee / Password123!
medline.admin / Password123!
medline.employee / Password123!
```
