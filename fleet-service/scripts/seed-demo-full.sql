-- Full demo fleet seed for PostgreSQL.
--
-- Apply after auth-service/scripts/seed-demo-full.sql.
-- The business_id and assigned_user_id values match the auth demo ids.

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
    (1004, 101, 'B-104-ATL', 'WF0EXXTTGE1101004', 'Ford', 'Transit Custom', 2019, 'VAN', 'DIESEL', 'OWNED', 'IN_SERVICE', 'Maintenance', 1005, 'Atlas Mechanic', 151200, NOW(), NOW()),
    (1005, 101, 'B-105-ATL', 'KMHLC41UBLU101005', 'Hyundai', 'Kona Electric', 2023, 'CAR', 'ELECTRIC', 'LEASED', 'ACTIVE', 'Sales', 1004, 'Atlas Driver', 21400, NOW(), NOW()),
    (1006, 101, 'B-106-ATL', 'UU1DJF00267101006', 'Dacia', 'Duster', 2021, 'CAR', 'LPG', 'OWNED', 'ACTIVE', 'Field Service', 1002, 'Atlas Employee', 67400, NOW(), NOW()),
    (1007, 101, 'B-107-ATL', 'WDB9076331P101007', 'Mercedes-Benz', 'Sprinter', 2022, 'VAN', 'DIESEL', 'RENTED', 'ACTIVE', 'Distribution', NULL, NULL, 55800, NOW(), NOW()),
    (1008, 101, 'B-108-ATL', 'VF3M45GFRNS101008', 'Peugeot', '3008', 2022, 'CAR', 'HYBRID', 'LEASED', 'INACTIVE', 'Sales', NULL, NULL, 33800, NOW(), NOW()),

    (1011, 102, 'CJ-201-NOV', 'WBA3D31040J102011', 'BMW', '320d', 2019, 'CAR', 'DIESEL', 'OWNED', 'ACTIVE', 'Site Management', 1012, 'Nova Employee', 93400, NOW(), NOW()),
    (1012, 102, 'CJ-202-NOV', 'ZFA2500000N102012', 'Fiat', 'Ducato', 2021, 'VAN', 'DIESEL', 'RENTED', 'IN_SERVICE', 'Construction', 1013, 'Nova Site', 71100, NOW(), NOW()),
    (1013, 102, 'CJ-203-NOV', 'TMAJ3815GLJ102013', 'Hyundai', 'Tucson', 2020, 'CAR', 'DIESEL', 'LEASED', 'ACTIVE', 'Field Service', 1014, 'Nova Fleet', 81240, NOW(), NOW()),
    (1014, 102, 'CJ-204-NOV', 'WMA06XZZ1MM102014', 'MAN', 'TGE', 2021, 'VAN', 'DIESEL', 'OWNED', 'ACTIVE', 'Logistics', NULL, NULL, 78100, NOW(), NOW()),
    (1015, 102, 'CJ-205-NOV', 'LRWYGCEK5LC102015', 'Tesla', 'Model 3', 2022, 'CAR', 'ELECTRIC', 'LEASED', 'ACTIVE', 'Management', 1012, 'Nova Employee', 41200, NOW(), NOW()),
    (1016, 102, 'CJ-206-NOV', 'VF7ACBHZHNY102016', 'Citroen', 'C5 Aircross', 2022, 'CAR', 'DIESEL', 'LEASED', 'ACTIVE', 'Operations', 1013, 'Nova Site', 42150, NOW(), NOW()),
    (1017, 102, 'CJ-207-NOV', 'WV1ZZZ7HZKH102017', 'Volkswagen', 'Transporter', 2018, 'VAN', 'DIESEL', 'OWNED', 'IN_SERVICE', 'Construction', NULL, NULL, 158900, NOW(), NOW()),
    (1018, 102, 'CJ-208-NOV', 'SB1K93BE20E102018', 'Toyota', 'Camry Hybrid', 2022, 'CAR', 'HYBRID', 'LEASED', 'INACTIVE', 'Management', NULL, NULL, 38400, NOW(), NOW()),

    (1021, 103, 'IS-301-MED', 'VF3YCBMFC12103021', 'Peugeot', 'Boxer', 2022, 'VAN', 'DIESEL', 'LEASED', 'ACTIVE', 'Courier', 1022, 'Medline Employee', 59400, NOW(), NOW()),
    (1022, 103, 'IS-302-MED', 'KMHLC41UBL1U03022', 'Hyundai', 'i30', 2023, 'CAR', 'HYBRID', 'OWNED', 'ACTIVE', 'Operations', 1024, 'Medline Driver', 19200, NOW(), NOW()),
    (1023, 103, 'IS-303-MED', 'WF0DXXTTRD1103023', 'Ford', 'Kuga', 2020, 'CAR', 'DIESEL', 'LEASED', 'ACTIVE', 'Field Service', 1023, 'Medline Dispatch', 74920, NOW(), NOW()),
    (1024, 103, 'IS-304-MED', 'VF1FL000668103024', 'Renault', 'Trafic', 2019, 'VAN', 'DIESEL', 'OWNED', 'ACTIVE', 'Courier', 1022, 'Medline Employee', 139600, NOW(), NOW()),
    (1025, 103, 'IS-305-MED', 'JTDKARFPXK3103025', 'Toyota', 'Prius', 2019, 'CAR', 'HYBRID', 'OWNED', 'ACTIVE', 'Administration', NULL, NULL, 88940, NOW(), NOW()),
    (1026, 103, 'IS-306-MED', 'WDB4476031P103026', 'Mercedes-Benz', 'Vito', 2020, 'VAN', 'DIESEL', 'OWNED', 'IN_SERVICE', 'Logistics', 1024, 'Medline Driver', 112300, NOW(), NOW()),
    (1027, 103, 'IS-307-MED', 'SJNFAAJ11U1103027', 'Nissan', 'Qashqai', 2019, 'CAR', 'PETROL', 'OWNED', 'INACTIVE', 'Sales', NULL, NULL, 96500, NOW(), NOW()),
    (1028, 103, 'IS-308-MED', 'VF1RJA00768103028', 'Renault', 'Captur', 2023, 'CAR', 'HYBRID', 'LEASED', 'ACTIVE', 'Sales', 1023, 'Medline Dispatch', 14200, NOW(), NOW())
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
