-- Demo multi-tenant fleet seed.
--
-- Must be applied after auth-service/scripts/seed-demo-multitenant.sql.
-- The business_id and assigned_user_id values match that auth seed.

BEGIN;

INSERT INTO vehicles (
    id,
    business_id,
    license_plate,
    vin,
    brand,
    model,
    manufacture_year,
    vehicle_type,
    fuel_type,
    ownership_type,
    status,
    department,
    assigned_user_id,
    assigned_driver_name,
    current_mileage,
    created_at,
    updated_at
)
VALUES
    (1001, 101, 'B-101-ATL', 'WVWZZZ1KZ8W101001', 'Volkswagen', 'Caddy', 2021, 'VAN', 'DIESEL', 'OWNED', 'ACTIVE', 'Distribution', 1002, 'Atlas Employee', 84600, NOW(), NOW()),
    (1002, 101, 'B-102-ATL', 'VF1RFB00368101002', 'Renault', 'Master', 2020, 'VAN', 'DIESEL', 'LEASED', 'ACTIVE', 'Distribution', 1003, 'Atlas Dispatch', 126300, NOW(), NOW()),
    (1003, 101, 'B-103-ATL', 'TMBJJ7NE8M0101003', 'Skoda', 'Octavia', 2022, 'CAR', 'PETROL', 'OWNED', 'ACTIVE', 'Management', NULL, NULL, 38500, NOW(), NOW()),
    (1011, 102, 'CJ-201-NOV', 'WBA3D31040J102011', 'BMW', '320d', 2019, 'CAR', 'DIESEL', 'OWNED', 'ACTIVE', 'Site Management', 1012, 'Nova Employee', 93400, NOW(), NOW()),
    (1012, 102, 'CJ-202-NOV', 'ZFA2500000N102012', 'Fiat', 'Ducato', 2021, 'VAN', 'DIESEL', 'RENTED', 'IN_SERVICE', 'Construction', NULL, NULL, 71100, NOW(), NOW()),
    (1021, 103, 'IS-301-MED', 'VF3YCBMFC12103021', 'Peugeot', 'Boxer', 2022, 'VAN', 'DIESEL', 'LEASED', 'ACTIVE', 'Courier', 1022, 'Medline Employee', 59400, NOW(), NOW()),
    (1022, 103, 'IS-302-MED', 'KMHLC41UBL1U03022', 'Hyundai', 'i30', 2023, 'CAR', 'HYBRID', 'OWNED', 'ACTIVE', 'Operations', NULL, NULL, 19200, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    business_id = EXCLUDED.business_id,
    license_plate = EXCLUDED.license_plate,
    vin = EXCLUDED.vin,
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    manufacture_year = EXCLUDED.manufacture_year,
    vehicle_type = EXCLUDED.vehicle_type,
    fuel_type = EXCLUDED.fuel_type,
    ownership_type = EXCLUDED.ownership_type,
    status = EXCLUDED.status,
    department = EXCLUDED.department,
    assigned_user_id = EXCLUDED.assigned_user_id,
    assigned_driver_name = EXCLUDED.assigned_driver_name,
    current_mileage = EXCLUDED.current_mileage,
    updated_at = NOW();

SELECT setval(pg_get_serial_sequence('vehicles', 'id'), GREATEST((SELECT MAX(id) FROM vehicles), 1), true);

COMMIT;
