-- ============================================================================
-- V3__seed_fleet_extra_vehicles.sql  (baza: fleet_deploy)
-- 30 vehicule demo suplimentare: cate 6 pentru organizatiile 101, 102, 103,
-- 9001 si 9002. assigned_user_id 9201-9205 corespund utilizatorilor creati de
-- V3__seed_auth_extra_users.sql.
--
-- Idempotent: ON CONFLICT DO NOTHING; ID-uri rezervate 9201-9230.
-- ============================================================================

BEGIN;

INSERT INTO vehicles (
  id, business_id, license_plate, vin, brand, model, manufacture_year,
  vehicle_type, fuel_type, ownership_type, status, department,
  assigned_user_id, assigned_driver_name, current_mileage, created_at, updated_at
) VALUES
  -- Atlas Logistics SRL (101)
  (9201, 101, 'B-701-ATL', 'WDFDFM00000009201', 'Mercedes-Benz', 'Sprinter', 2022, 'VAN',   'DIESEL',   'LEASED', 'ACTIVE',     'Distributie', 9201, 'Radu Pavel',  68400, now(), now()),
  (9202, 101, 'B-702-ATL', 'WDFDFM00000009202', 'Ford',          'Transit',  2021, 'VAN',   'DIESEL',   'OWNED',  'ACTIVE',     'Distributie', 9201, 'Radu Pavel',  91750, now(), now()),
  (9203, 101, 'B-703-ATL', 'WDFDFM00000009203', 'Iveco',         'Daily',    2020, 'TRUCK', 'DIESEL',   'OWNED',  'IN_SERVICE', 'Logistica',   NULL, NULL,          132800, now(), now()),
  (9204, 101, 'B-704-ATL', 'WDFDFM00000009204', 'Renault',       'Master',   2023, 'VAN',   'DIESEL',   'RENTED', 'ACTIVE',     'Curierat',    9201, 'Radu Pavel',  38800, now(), now()),
  (9205, 101, 'B-705-ATL', 'WDFDFM00000009205', 'Volkswagen',    'Caddy',    2019, 'VAN',   'DIESEL',   'OWNED',  'ACTIVE',     'Operatiuni',  9201, 'Radu Pavel', 157300, now(), now()),
  (9206, 101, 'B-706-ATL', 'WDFDFM00000009206', 'Dacia',         'Duster',   2020, 'CAR',   'PETROL',   'OWNED',  'INACTIVE',   'Administrativ', NULL, NULL,       104200, now(), now()),

  -- Nova Construct SRL (102)
  (9207, 102, 'IF-701-NVC', 'WDFDFM00000009207', 'Toyota',     'Hilux',    2022, 'TRUCK', 'DIESEL', 'LEASED', 'ACTIVE',     'Santier',      9202, 'Sorin Matei',  75200, now(), now()),
  (9208, 102, 'IF-702-NVC', 'WDFDFM00000009208', 'Ford',       'Ranger',   2021, 'TRUCK', 'DIESEL', 'OWNED',  'ACTIVE',     'Santier',      9202, 'Sorin Matei',  98400, now(), now()),
  (9209, 102, 'IF-703-NVC', 'WDFDFM00000009209', 'Volkswagen', 'Crafter',  2020, 'VAN',   'DIESEL', 'OWNED',  'ACTIVE',     'Aprovizionare',9202, 'Sorin Matei', 126700, now(), now()),
  (9210, 102, 'IF-704-NVC', 'WDFDFM00000009210', 'Renault',    'Master',   2019, 'VAN',   'DIESEL', 'RENTED', 'IN_SERVICE', 'Mentenanta',   NULL, NULL,            181900, now(), now()),
  (9211, 102, 'IF-705-NVC', 'WDFDFM00000009211', 'Dacia',      'Duster',   2023, 'CAR',   'PETROL', 'OWNED',  'ACTIVE',     'Supervizare',  9202, 'Sorin Matei',  29500, now(), now()),
  (9212, 102, 'IF-706-NVC', 'WDFDFM00000009212', 'Skoda',      'Octavia',  2018, 'CAR',   'DIESEL', 'OWNED',  'INACTIVE',   'Administrativ',NULL, NULL,            214500, now(), now()),

  -- Medline Courier SRL (103)
  (9213, 103, 'B-701-MED', 'WDFDFM00000009213', 'Renault',    'Kangoo',   2022, 'VAN', 'DIESEL',   'LEASED', 'ACTIVE',     'Livrari medicale', 9203, 'Bianca Dobre',  59400, now(), now()),
  (9214, 103, 'B-702-MED', 'WDFDFM00000009214', 'Peugeot',    'Partner',  2021, 'VAN', 'DIESEL',   'OWNED',  'ACTIVE',     'Livrari medicale', 9203, 'Bianca Dobre',  88600, now(), now()),
  (9215, 103, 'B-703-MED', 'WDFDFM00000009215', 'Citroen',    'Berlingo', 2020, 'VAN', 'DIESEL',   'OWNED',  'IN_SERVICE', 'Depozit',           NULL, NULL,             119800, now(), now()),
  (9216, 103, 'B-704-MED', 'WDFDFM00000009216', 'Volkswagen', 'Caddy',    2023, 'VAN', 'DIESEL',   'RENTED', 'ACTIVE',     'Urgente',          9203, 'Bianca Dobre',  31700, now(), now()),
  (9217, 103, 'B-705-MED', 'WDFDFM00000009217', 'Toyota',     'Corolla',  2022, 'CAR', 'HYBRID',   'LEASED', 'ACTIVE',     'Vanzari',          9203, 'Bianca Dobre',  46200, now(), now()),
  (9218, 103, 'B-706-MED', 'WDFDFM00000009218', 'Nissan',     'Leaf',     2020, 'CAR', 'ELECTRIC', 'OWNED',  'INACTIVE',   'Administrativ',    NULL, NULL,              77900, now(), now()),

  -- Transil Logistics SRL (9001)
  (9219, 9001, 'CJ-701-TRL', 'WDFDFM00000009219', 'Volvo',       'FH',       2021, 'TRUCK', 'DIESEL', 'LEASED', 'ACTIVE',     'Cursa lunga', 9204, 'Vlad Muresan', 184300, now(), now()),
  (9220, 9001, 'CJ-702-TRL', 'WDFDFM00000009220', 'Scania',      'R450',     2020, 'TRUCK', 'DIESEL', 'OWNED',  'ACTIVE',     'Cursa lunga', 9204, 'Vlad Muresan', 267800, now(), now()),
  (9221, 9001, 'CJ-703-TRL', 'WDFDFM00000009221', 'MAN',         'TGX',      2019, 'TRUCK', 'DIESEL', 'OWNED',  'IN_SERVICE', 'Cursa lunga', NULL, NULL,           342100, now(), now()),
  (9222, 9001, 'CJ-704-TRL', 'WDFDFM00000009222', 'Mercedes-Benz','Actros',  2022, 'TRUCK', 'DIESEL', 'RENTED', 'ACTIVE',     'International',9204, 'Vlad Muresan', 129500, now(), now()),
  (9223, 9001, 'CJ-705-TRL', 'WDFDFM00000009223', 'Volkswagen',  'Crafter',  2021, 'VAN',   'DIESEL', 'OWNED',  'ACTIVE',     'Distributie', 9204, 'Vlad Muresan',  73500, now(), now()),
  (9224, 9001, 'CJ-706-TRL', 'WDFDFM00000009224', 'Skoda',       'Superb',   2018, 'CAR',   'DIESEL', 'OWNED',  'INACTIVE',   'Management',  NULL, NULL,           198600, now(), now()),

  -- AgroTrans Muntenia SRL (9002)
  (9225, 9002, 'AG-701-AGM', 'WDFDFM00000009225', 'Iveco',      'Daily',    2022, 'VAN',   'DIESEL', 'LEASED', 'ACTIVE',     'Livrari',     9205, 'Andreea Tudor',  61200, now(), now()),
  (9226, 9002, 'AG-702-AGM', 'WDFDFM00000009226', 'Fiat',       'Ducato',   2021, 'VAN',   'DIESEL', 'OWNED',  'ACTIVE',     'Livrari',     9205, 'Andreea Tudor',  94700, now(), now()),
  (9227, 9002, 'AG-703-AGM', 'WDFDFM00000009227', 'Renault',    'Master',   2020, 'TRUCK', 'DIESEL', 'OWNED',  'IN_SERVICE', 'Depozit',     NULL, NULL,              137900, now(), now()),
  (9228, 9002, 'AG-704-AGM', 'WDFDFM00000009228', 'Ford',       'Transit',  2023, 'VAN',   'DIESEL', 'RENTED', 'ACTIVE',     'Distributie', 9205, 'Andreea Tudor',  35400, now(), now()),
  (9229, 9002, 'AG-705-AGM', 'WDFDFM00000009229', 'Dacia',      'Duster',   2022, 'CAR',   'PETROL', 'OWNED',  'ACTIVE',     'Agenti teren',9205, 'Andreea Tudor',  48900, now(), now()),
  (9230, 9002, 'AG-706-AGM', 'WDFDFM00000009230', 'Volkswagen', 'Passat',   2018, 'CAR',   'DIESEL', 'OWNED',  'INACTIVE',   'Administrativ',NULL, NULL,             206300, now(), now())
ON CONFLICT DO NOTHING;

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
