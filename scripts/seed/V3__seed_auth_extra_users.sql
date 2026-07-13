-- ============================================================================
-- V3__seed_auth_extra_users.sql  (baza: auth_deploy)
-- 5 angajati demo suplimentari: cate unul pentru organizatiile seed 101, 102,
-- 103, 9001 si 9002.
--
-- Noii utilizatori primesc acelasi hash de parola ca utilizatorul demo 9001
-- (ana.pop). Astfel, parola este cea folosita la rularea seed-ului V2 si nu
-- trebuie salvata in clar in repository.
--
-- Idempotent: ON CONFLICT DO NOTHING; ID-uri rezervate 9201-9205.
-- ============================================================================

BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM credentials WHERE credential_id = 9001) THEN
    RAISE EXCEPTION 'Lipseste credentialul demo 9001. Ruleaza mai intai V2__seed_auth_demo_data.sql.';
  END IF;

  IF (SELECT count(*) FROM businesses WHERE id IN (101, 102, 103, 9001, 9002)) <> 5 THEN
    RAISE EXCEPTION 'Lipsesc una sau mai multe organizatii necesare: 101, 102, 103, 9001, 9002.';
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
ON CONFLICT DO NOTHING;

INSERT INTO user_data (
  user_id, credential_id, email, first_name, last_name, phone, address,
  business_id, created_at, updated_at
) VALUES
  (9201, 9201, 'radu.pavel@example.com',    'Radu',    'Pavel',   '+40740109201', 'Bucuresti',    101,  now(), now()),
  (9202, 9202, 'sorin.matei@example.com',   'Sorin',   'Matei',   '+40740109202', 'Voluntari',    102,  now(), now()),
  (9203, 9203, 'bianca.dobre@example.com',  'Bianca',  'Dobre',   '+40740109203', 'Bucuresti',    103,  now(), now()),
  (9204, 9204, 'vlad.muresan@example.com',  'Vlad',    'Muresan', '+40740109204', 'Cluj-Napoca', 9001, now(), now()),
  (9205, 9205, 'andreea.tudor@example.com', 'Andreea', 'Tudor',   '+40740109205', 'Pitesti',     9002, now(), now())
ON CONFLICT DO NOTHING;

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
