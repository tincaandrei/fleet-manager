-- Full demo auth seed for PostgreSQL.
--
-- Password for all demo users: Password123!
--
-- Apply after auth-service has started once and Hibernate has created/updated
-- the schema. This script is idempotent and only writes the fixed demo ids
-- declared below.

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
    (1001, 'atlas.admin', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'BUSINESS_ADMIN')),
    (1002, 'atlas.employee', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1003, 'atlas.dispatch', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1004, 'atlas.driver', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1005, 'atlas.mechanic', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1011, 'nova.admin', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'BUSINESS_ADMIN')),
    (1012, 'nova.employee', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1013, 'nova.site', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1014, 'nova.fleet', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1021, 'medline.admin', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'BUSINESS_ADMIN')),
    (1022, 'medline.employee', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1023, 'medline.dispatch', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1024, 'medline.driver', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1091, 'pending.alex', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1092, 'pending.irina', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE')),
    (1093, 'pending.vlad', '$2a$10$T.coiOKNFs0cdqoQu0SLueiolqzybAYRBZObtv/CZd9i.TI9G7ivy', (SELECT role_id FROM roles WHERE role_name = 'EMPLOYEE'))
ON CONFLICT (credential_id) DO UPDATE SET
    username = EXCLUDED.username,
    password_hash = EXCLUDED.password_hash,
    role_id = EXCLUDED.role_id;

INSERT INTO user_data (user_id, credential_id, email, phone, address, business_id)
VALUES
    (1001, 1001, 'admin@atlas-logistics.test', '+40722000101', 'Bucuresti', 101),
    (1002, 1002, 'employee@atlas-logistics.test', '+40722000102', 'Bucuresti', 101),
    (1003, 1003, 'dispatch@atlas-logistics.test', '+40722000103', 'Bucuresti', 101),
    (1004, 1004, 'driver@atlas-logistics.test', '+40722000104', 'Ilfov', 101),
    (1005, 1005, 'mechanic@atlas-logistics.test', '+40722000105', 'Bucuresti', 101),
    (1011, 1011, 'admin@nova-construct.test', '+40722000111', 'Cluj-Napoca', 102),
    (1012, 1012, 'employee@nova-construct.test', '+40722000112', 'Cluj-Napoca', 102),
    (1013, 1013, 'site@nova-construct.test', '+40722000113', 'Turda', 102),
    (1014, 1014, 'fleet@nova-construct.test', '+40722000114', 'Cluj-Napoca', 102),
    (1021, 1021, 'admin@medline-courier.test', '+40722000121', 'Iasi', 103),
    (1022, 1022, 'employee@medline-courier.test', '+40722000122', 'Iasi', 103),
    (1023, 1023, 'dispatch@medline-courier.test', '+40722000123', 'Iasi', 103),
    (1024, 1024, 'driver@medline-courier.test', '+40722000124', 'Pascani', 103),
    (1091, 1091, 'alex.pending@example.test', '+40722000191', 'Bucuresti', NULL),
    (1092, 1092, 'irina.pending@example.test', '+40722000192', 'Cluj-Napoca', NULL),
    (1093, 1093, 'vlad.pending@example.test', '+40722000193', 'Iasi', NULL)
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
