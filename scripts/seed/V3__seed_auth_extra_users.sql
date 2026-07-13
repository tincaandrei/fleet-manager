-- ============================================================================
-- V3__seed_auth_extra_users.sql  (baza: auth_deploy)
-- 5 angajati demo suplimentari distribuiti intre organizatiile reale create
-- de seed-ul V2: 3 pentru Transil Logistics (9001) si 2 pentru AgroTrans
-- Muntenia (9002). Organizatiile tehnice, precum MOCK ORG, sunt ignorate.
--
-- Noii utilizatori primesc acelasi hash de parola ca utilizatorul demo 9001
-- (ana.pop). Parola ramane astfel cea folosita la rularea seed-ului V2 si nu
-- este salvata in clar in repository.
--
-- Idempotent: ID-uri rezervate 9201-9205; rerularea sincronizeaza aceste date.
-- ============================================================================

BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM credentials WHERE credential_id = 9001) THEN
    RAISE EXCEPTION 'Lipseste credentialul demo 9001. Ruleaza mai intai V2__seed_auth_demo_data.sql.';
  END IF;

  IF (SELECT count(*) FROM businesses WHERE id IN (9001, 9002)) <> 2 THEN
    RAISE EXCEPTION 'Lipsesc organizatiile demo 9001 si/sau 9002. Ruleaza mai intai V2__seed_auth_demo_data.sql.';
  END IF;
END $$;

INSERT INTO credentials (
  credential_id, username, email, password_hash, status, enabled,
  password_change_required, role_id
)
SELECT
  seed.credential_id,
  seed.username,
  seed.email,
  source.password_hash,
  'ACTIVE',
  true,
  false,
  employee_role.role_id
FROM (VALUES
  (9201::bigint, 'radu.pavel',    'radu.pavel@example.com'),
  (9202::bigint, 'sorin.matei',   'sorin.matei@example.com'),
  (9203::bigint, 'bianca.dobre',  'bianca.dobre@example.com'),
  (9204::bigint, 'vlad.muresan',  'vlad.muresan@example.com'),
  (9205::bigint, 'andreea.tudor', 'andreea.tudor@example.com')
) AS seed(credential_id, username, email)
CROSS JOIN (SELECT password_hash FROM credentials WHERE credential_id = 9001) source
CROSS JOIN (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE') employee_role
ON CONFLICT (credential_id) DO UPDATE SET
  username = EXCLUDED.username,
  email = EXCLUDED.email,
  password_hash = EXCLUDED.password_hash,
  status = EXCLUDED.status,
  enabled = EXCLUDED.enabled,
  password_change_required = EXCLUDED.password_change_required,
  role_id = EXCLUDED.role_id;

INSERT INTO user_data (
  user_id, credential_id, email, first_name, last_name, phone, address,
  business_id, created_at, updated_at
) VALUES
  (9201, 9201, 'radu.pavel@example.com',    'Radu',    'Pavel',   '+40740109201', 'Cluj-Napoca', 9001, now(), now()),
  (9202, 9202, 'sorin.matei@example.com',   'Sorin',   'Matei',   '+40740109202', 'Turda',       9001, now(), now()),
  (9203, 9203, 'bianca.dobre@example.com',  'Bianca',  'Dobre',   '+40740109203', 'Cluj-Napoca', 9001, now(), now()),
  (9204, 9204, 'vlad.muresan@example.com',  'Vlad',    'Muresan', '+40740109204', 'Pitesti',     9002, now(), now()),
  (9205, 9205, 'andreea.tudor@example.com', 'Andreea', 'Tudor',   '+40740109205', 'Mioveni',     9002, now(), now())
ON CONFLICT (user_id) DO UPDATE SET
  credential_id = EXCLUDED.credential_id,
  email = EXCLUDED.email,
  first_name = EXCLUDED.first_name,
  last_name = EXCLUDED.last_name,
  phone = EXCLUDED.phone,
  address = EXCLUDED.address,
  business_id = EXCLUDED.business_id,
  updated_at = now();

SELECT setval(
  pg_get_serial_sequence('credentials', 'credential_id'),
  (SELECT COALESCE(MAX(credential_id), 1) FROM credentials)
)
WHERE pg_get_serial_sequence('credentials', 'credential_id') IS NOT NULL;

SELECT setval(
  pg_get_serial_sequence('user_data', 'user_id'),
  (SELECT COALESCE(MAX(user_id), 1) FROM user_data)
)
WHERE pg_get_serial_sequence('user_data', 'user_id') IS NOT NULL;

COMMIT;

-- Verificare rapida
SELECT u.user_id, u.first_name, u.last_name, u.business_id, c.username,
       r.role_name, c.status
FROM user_data u
JOIN credentials c ON c.credential_id = u.credential_id
JOIN roles r ON r.role_id = c.role_id
WHERE u.user_id BETWEEN 9201 AND 9205
ORDER BY u.user_id;
