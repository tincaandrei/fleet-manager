-- ============================================================================
-- V2__seed_fleet_demo_data.sql  (baza: fleet_deploy)
-- 6 vehicule demo (fara imagini), 3 per organizatie.
-- business_id / assigned_user_id corespund ID-urilor din V2__seed_auth_demo_data.sql
-- (organizatii 9001/9002, angajati 9002/9003/9005).
-- Idempotent: ON CONFLICT DO NOTHING.
-- ============================================================================

BEGIN;

INSERT INTO vehicles (
  id, business_id, license_plate, vin, brand, model, manufacture_year,
  vehicle_type, fuel_type, ownership_type, status, department,
  assigned_user_id, assigned_driver_name, current_mileage, created_at, updated_at
) VALUES
  (9001, 9001, 'B-101-TRL',  'WVWZZZ1KZAW000001', 'Volkswagen', 'Crafter',   2021, 'VAN',   'DIESEL',   'OWNED',  'ACTIVE', 'Distributie',   9002, 'Mihai Ionescu', 148200, now(), now()),
  (9002, 9001, 'B-102-TRL',  'WDB9066571S000002', 'Mercedes',   'Sprinter',  2019, 'VAN',   'DIESEL',   'LEASED', 'ACTIVE', 'Distributie',   9003, 'Elena Rusu',    231500, now(), now()),
  (9003, 9001, 'CJ-201-TRL', 'VF1MAF4XH53000003', 'Renault',    'Master',    2022, 'TRUCK', 'DIESEL',   'OWNED',  'ACTIVE', 'Cursa lunga',   NULL, NULL,             87400, now(), now()),
  (9004, 9002, 'AG-301-AGM', 'UU1LSDAB453000004', 'Dacia',      'Duster',    2023, 'CAR',   'PETROL',   'OWNED',  'ACTIVE', 'Agenti teren',  9005, 'Ioana Stan',     41200, now(), now()),
  (9005, 9002, 'AG-302-AGM', 'ZFA25000002000005', 'Fiat',       'Ducato',    2020, 'VAN',   'DIESEL',   'RENTED', 'ACTIVE', 'Livrari',       NULL, NULL,            175300, now(), now()),
  (9006, 9002, 'AG-303-AGM', 'TMBJJ7NE2H0000006', 'Skoda',      'Octavia',   2018, 'CAR',   'HYBRID',   'OWNED',  'ACTIVE', 'Administrativ', NULL, NULL,            198750, now(), now())
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('vehicles', 'id'), (SELECT COALESCE(MAX(id), 1) FROM vehicles))
WHERE pg_get_serial_sequence('vehicles', 'id') IS NOT NULL;

COMMIT;

-- Verificare rapida
SELECT business_id, count(*) AS vehicles FROM vehicles WHERE id BETWEEN 9001 AND 9006 GROUP BY business_id;
