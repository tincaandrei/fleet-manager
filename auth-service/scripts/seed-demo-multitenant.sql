-- Demo multi-tenant auth seed.
-- Password for all demo users: Password123!
--
-- This script is idempotent and uses fixed ids so fleet-service demo vehicles
-- can reference the seeded auth users by assigned_user_id.

BEGIN;

INSERT INTO roles (role_name)
VALUES
    ('SUPERADMIN'),
    ('BUSINESS_ADMIN'),
    ('EMPLOYEE')
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO businesses (id, name, registration_number, contact_email, phone, address, active, created_at, updated_at)
VALUES
    (101, 'Atlas Logistics SRL', 'RO10000001', 'office@atlas-logistics.test', '+40721000101', 'Bucuresti, Str. Industriilor 10', true, NOW(), NOW()),
    (102, 'Nova Construct SRL', 'RO10000002', 'office@nova-construct.test', '+40721000102', 'Cluj-Napoca, Str. Fabricii 22', true, NOW(), NOW()),
    (103, 'Medline Courier SRL', 'RO10000003', 'office@medline-courier.test', '+40721000103', 'Iasi, Sos. Pacurari 45', true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    registration_number = EXCLUDED.registration_number,
    contact_email = EXCLUDED.contact_email,
    phone = EXCLUDED.phone,
    address = EXCLUDED.address,
    active = EXCLUDED.active,
    updated_at = NOW();

INSERT INTO credentials (credential_id, username, password_hash, role_id)
VALUES
    (1001, 'atlas.admin', '$2a$10$s3dPORHJFwvztGBuz8U/eumotwIbcUvWB7pTDR7JMwoho2/V9Ab1G', (SELECT role_id FROM roles WHERE role_name = 'BUSINESS_ADMIN')),
    (1002, 'atlas.employee', '$2a$10$s3dPORHJFwvztGBuz8U/eumotwIbcUvWB7pTDR7JMwoho2/V9Ab1G', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1003, 'atlas.dispatch', '$2a$10$s3dPORHJFwvztGBuz8U/eumotwIbcUvWB7pTDR7JMwoho2/V9Ab1G', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1011, 'nova.admin', '$2a$10$s3dPORHJFwvztGBuz8U/eumotwIbcUvWB7pTDR7JMwoho2/V9Ab1G', (SELECT role_id FROM roles WHERE role_name = 'BUSINESS_ADMIN')),
    (1012, 'nova.employee', '$2a$10$s3dPORHJFwvztGBuz8U/eumotwIbcUvWB7pTDR7JMwoho2/V9Ab1G', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1021, 'medline.admin', '$2a$10$s3dPORHJFwvztGBuz8U/eumotwIbcUvWB7pTDR7JMwoho2/V9Ab1G', (SELECT role_id FROM roles WHERE role_name = 'BUSINESS_ADMIN')),
    (1022, 'medline.employee', '$2a$10$s3dPORHJFwvztGBuz8U/eumotwIbcUvWB7pTDR7JMwoho2/V9Ab1G', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE'))
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    role_id = EXCLUDED.role_id;

INSERT INTO user_data (user_id, credential_id, email, phone, address, business_id)
VALUES
    (1001, (SELECT credential_id FROM credentials WHERE username = 'atlas.admin'), 'admin@atlas-logistics.test', '+40722000101', 'Bucuresti', 101),
    (1002, (SELECT credential_id FROM credentials WHERE username = 'atlas.employee'), 'employee@atlas-logistics.test', '+40722000102', 'Bucuresti', 101),
    (1003, (SELECT credential_id FROM credentials WHERE username = 'atlas.dispatch'), 'dispatch@atlas-logistics.test', '+40722000103', 'Bucuresti', 101),
    (1011, (SELECT credential_id FROM credentials WHERE username = 'nova.admin'), 'admin@nova-construct.test', '+40722000111', 'Cluj-Napoca', 102),
    (1012, (SELECT credential_id FROM credentials WHERE username = 'nova.employee'), 'employee@nova-construct.test', '+40722000112', 'Cluj-Napoca', 102),
    (1021, (SELECT credential_id FROM credentials WHERE username = 'medline.admin'), 'admin@medline-courier.test', '+40722000121', 'Iasi', 103),
    (1022, (SELECT credential_id FROM credentials WHERE username = 'medline.employee'), 'employee@medline-courier.test', '+40722000122', 'Iasi', 103)
ON CONFLICT (user_id) DO UPDATE SET
    credential_id = EXCLUDED.credential_id,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    address = EXCLUDED.address,
    business_id = EXCLUDED.business_id;

SELECT setval(pg_get_serial_sequence('roles', 'role_id'), GREATEST((SELECT MAX(role_id) FROM roles), 1), true);
SELECT setval(pg_get_serial_sequence('businesses', 'id'), GREATEST((SELECT MAX(id) FROM businesses), 1), true);
SELECT setval(pg_get_serial_sequence('credentials', 'credential_id'), GREATEST((SELECT MAX(credential_id) FROM credentials), 1), true);
SELECT setval(pg_get_serial_sequence('user_data', 'user_id'), GREATEST((SELECT MAX(user_id) FROM user_data), 1), true);

COMMIT;
