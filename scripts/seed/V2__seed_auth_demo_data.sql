-- ============================================================================
-- V2__seed_auth_demo_data.sql  (baza: auth_deploy)
-- Date demo: 2 organizatii + 5 utilizatori (2 admini, 3 angajati).
-- Idempotent: poate fi rulat de mai multe ori (ON CONFLICT DO NOTHING).
-- NECESITA variabila psql "pwhash" (hash bcrypt pentru parola demo):
--   psql ... -v pwhash="$PWHASH" -f V2__seed_auth_demo_data.sql
-- ID-urile sunt in intervalul 9001+ ca sa nu se ciocneasca cu datele existente
-- si ca sa poata fi referite determinist din fleet_deploy / document_deploy.
-- ============================================================================

BEGIN;

-- Rolurile exista de obicei deja (create la bootstrap); ne asiguram doar.
INSERT INTO roles (role_name)
SELECT v.r
FROM (VALUES ('SUPERADMIN'), ('BUSINESS_ADMIN'), ('EMPLOYEE')) AS v(r)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = v.r);

-- Organizatii
INSERT INTO businesses (id, name, registration_number, contact_email, phone, address, active, created_at, updated_at) VALUES
  (9001, 'Transil Logistics SRL',  'J12/3456/2019', 'office@transil-logistics.ro',  '+40745123456', 'Str. Depozitelor 12, Cluj-Napoca', true, now(), now()),
  (9002, 'AgroTrans Muntenia SRL', 'J03/789/2021',  'contact@agrotrans-muntenia.ro', '+40723987654', 'Sos. Campulung 88, Pitesti',       true, now(), now())
ON CONFLICT DO NOTHING;

-- Credentiale (parola: cea din care ai generat $PWHASH; ex. Fleet123!)
INSERT INTO credentials (credential_id, username, email, password_hash, status, enabled, password_change_required, role_id) VALUES
  (9001, 'ana.pop',       'admin.transil@example.com',   :'pwhash', 'ACTIVE', true, false, (SELECT role_id FROM roles WHERE role_name = 'BUSINESS_ADMIN')),
  (9002, 'mihai.ionescu', 'mihai.ionescu@example.com',   :'pwhash', 'ACTIVE', true, false, (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
  (9003, 'elena.rusu',    'elena.rusu@example.com',      :'pwhash', 'ACTIVE', true, false, (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
  (9004, 'dan.marin',     'admin.agrotrans@example.com', :'pwhash', 'ACTIVE', true, false, (SELECT role_id FROM roles WHERE role_name = 'BUSINESS_ADMIN')),
  (9005, 'ioana.stan',    'ioana.stan@example.com',      :'pwhash', 'ACTIVE', true, false, (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE'))
ON CONFLICT DO NOTHING;

-- Profil utilizatori, legati de organizatii
INSERT INTO user_data (user_id, credential_id, email, first_name, last_name, phone, address, business_id, created_at, updated_at) VALUES
  (9001, 9001, 'admin.transil@example.com',   'Ana',   'Pop',     '+40745111222', 'Cluj-Napoca', 9001, now(), now()),
  (9002, 9002, 'mihai.ionescu@example.com',   'Mihai', 'Ionescu', '+40745333444', 'Cluj-Napoca', 9001, now(), now()),
  (9003, 9003, 'elena.rusu@example.com',      'Elena', 'Rusu',    '+40745555666', 'Turda',       9001, now(), now()),
  (9004, 9004, 'admin.agrotrans@example.com', 'Dan',   'Marin',   '+40723111222', 'Pitesti',     9002, now(), now()),
  (9005, 9005, 'ioana.stan@example.com',      'Ioana', 'Stan',    '+40723333444', 'Mioveni',     9002, now(), now())
ON CONFLICT DO NOTHING;

-- Realiniem secventele identity dupa insert-urile cu ID explicit.
SELECT setval(pg_get_serial_sequence('businesses', 'id'),            (SELECT COALESCE(MAX(id), 1)            FROM businesses))
WHERE pg_get_serial_sequence('businesses', 'id') IS NOT NULL;
SELECT setval(pg_get_serial_sequence('credentials', 'credential_id'),(SELECT COALESCE(MAX(credential_id), 1) FROM credentials))
WHERE pg_get_serial_sequence('credentials', 'credential_id') IS NOT NULL;
SELECT setval(pg_get_serial_sequence('user_data', 'user_id'),        (SELECT COALESCE(MAX(user_id), 1)       FROM user_data))
WHERE pg_get_serial_sequence('user_data', 'user_id') IS NOT NULL;
SELECT setval(pg_get_serial_sequence('roles', 'role_id'),            (SELECT COALESCE(MAX(role_id), 1)       FROM roles))
WHERE pg_get_serial_sequence('roles', 'role_id') IS NOT NULL;

COMMIT;

-- Verificare rapida
SELECT b.id, b.name,
       (SELECT count(*) FROM user_data u WHERE u.business_id = b.id) AS users
FROM businesses b WHERE b.id IN (9001, 9002);
