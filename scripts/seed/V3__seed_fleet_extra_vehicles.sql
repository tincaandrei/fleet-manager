-- ============================================================================
-- V3__seed_fleet_extra_vehicles.sql  (baza: fleet_deploy)
-- 30 vehicule demo suplimentare: 15 pentru Transil Logistics (9001) si 15
-- pentru AgroTrans Muntenia (9002). assigned_user_id 9201-9205 corespund
-- utilizatorilor creati de V3__seed_auth_extra_users.sql.
--
-- Idempotent: ID-uri rezervate 9201-9230; rerularea sincronizeaza aceste date.
-- ============================================================================

BEGIN;

WITH vehicle_seed AS (
  SELECT
    id,
    CASE WHEN id <= 9215 THEN 9001::bigint ELSE 9002::bigint END AS business_id,
    CASE
      WHEN id <= 9215 THEN format('CJ-%s-TRL', 701 + id - 9201)
      ELSE format('AG-%s-AGM', 701 + id - 9216)
    END AS license_plate,
    'WDFDFM' || lpad(id::text, 11, '0') AS vin,
    (ARRAY['Mercedes-Benz', 'Ford', 'Iveco', 'Renault', 'Volkswagen',
           'Dacia', 'Toyota', 'Skoda', 'Fiat', 'Volvo'])[((id - 9201) % 10) + 1] AS brand,
    (ARRAY['Sprinter', 'Transit', 'Daily', 'Master', 'Crafter',
           'Duster', 'Hilux', 'Octavia', 'Ducato', 'FH'])[((id - 9201) % 10) + 1] AS model,
    2018 + ((id - 9201) % 6)::integer AS manufacture_year,
    (ARRAY['VAN', 'VAN', 'TRUCK', 'VAN', 'TRUCK', 'CAR'])[((id - 9201) % 6) + 1] AS vehicle_type,
    (ARRAY['DIESEL', 'DIESEL', 'DIESEL', 'DIESEL', 'HYBRID', 'PETROL'])[((id - 9201) % 6) + 1] AS fuel_type,
    (ARRAY['OWNED', 'LEASED', 'OWNED', 'RENTED'])[((id - 9201) % 4) + 1] AS ownership_type,
    CASE
      WHEN (id - 9201) % 6 = 4 THEN 'IN_SERVICE'
      WHEN (id - 9201) % 6 = 5 THEN 'INACTIVE'
      ELSE 'ACTIVE'
    END AS status,
    (ARRAY['Distributie', 'Logistica', 'Cursa lunga', 'Livrari',
           'Operatiuni', 'Administrativ'])[((id - 9201) % 6) + 1] AS department,
    CASE
      WHEN (id - 9201) % 5 = 4 THEN NULL
      WHEN id <= 9215 THEN 9201 + ((id - 9201) % 3)
      ELSE 9204 + ((id - 9216) % 2)
    END::bigint AS assigned_user_id,
    28000::bigint + (id - 9201) * 7350::bigint AS current_mileage
  FROM generate_series(9201::bigint, 9230::bigint) AS generated(id)
), normalized_seed AS (
  SELECT
    vehicle_seed.*,
    CASE assigned_user_id
      WHEN 9201 THEN 'Radu Pavel'
      WHEN 9202 THEN 'Sorin Matei'
      WHEN 9203 THEN 'Bianca Dobre'
      WHEN 9204 THEN 'Vlad Muresan'
      WHEN 9205 THEN 'Andreea Tudor'
      ELSE NULL
    END AS assigned_driver_name
  FROM vehicle_seed
)
INSERT INTO vehicles (
  id, business_id, license_plate, vin, brand, model, manufacture_year,
  vehicle_type, fuel_type, ownership_type, status, department,
  assigned_user_id, assigned_driver_name, current_mileage, created_at, updated_at
)
SELECT
  id, business_id, license_plate, vin, brand, model, manufacture_year,
  vehicle_type, fuel_type, ownership_type, status, department,
  assigned_user_id, assigned_driver_name, current_mileage, now(), now()
FROM normalized_seed
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
  updated_at = now();

SELECT setval(
  pg_get_serial_sequence('vehicles', 'id'),
  (SELECT COALESCE(MAX(id), 1) FROM vehicles)
)
WHERE pg_get_serial_sequence('vehicles', 'id') IS NOT NULL;

COMMIT;

-- Verificare rapida
SELECT business_id,
       count(*) AS vehicles_added,
       count(*) FILTER (WHERE status = 'ACTIVE') AS active,
       count(*) FILTER (WHERE assigned_user_id IS NOT NULL) AS assigned
FROM vehicles
WHERE id BETWEEN 9201 AND 9230
GROUP BY business_id
ORDER BY business_id;
