-- ============================================================================
-- V2__seed_document_demo_data.sql  (baza: document_deploy)
-- Documente demo VALIDATE (RCA / ITP / rovinieta / facturi) pentru vehiculele
-- 9001-9006, cu date de expirare RELATIVE la ziua rularii:
--   * RCA B-101-TRL  expira in 12 zile   -> apare in alerte + notificari
--   * ITP B-102-TRL  expira in 25 zile   -> apare in alerte
--   * RCA CJ-201-TRL expirat de 5 zile   -> apare ca "Expired"
--   * RCA AG-301-AGM expira in 8 zile    -> apare in alerte
--   * Rovinieta AG-303-AGM expira in 20 zile
--   * restul: valabile / facturi cu sume (apar in raportul de costuri)
-- Fisierele fizice NU exista (fara imagini/PDF-uri) - butonul de download va
-- da eroare pentru aceste documente demo; listele, alertele, rapoartele si
-- notificarile functioneaza normal.
-- Idempotent: ON CONFLICT DO NOTHING (UUID-uri fixe).
-- ============================================================================

BEGIN;

INSERT INTO documents (
  id, vehicle_id, business_id, document_type, document_subtype, status,
  original_file_name, stored_file_name, content_type, file_size, storage_path,
  uploaded_by_user_id, created_at, updated_at
) VALUES
  ('dddd0000-0000-4000-8000-000000009101', 9001, 9001, 'INSURANCE',            'RCA',         'VALIDATED', 'rca-b101trl.pdf',                  'seed-9101.pdf', 'application/pdf', 84211, 'seed/seed-9101.pdf', 9002, now() - interval '20 days', now()),
  ('dddd0000-0000-4000-8000-000000009102', 9001, 9001, 'EXPENSE_INVOICE',      'Service',     'VALIDATED', 'factura-service-b101trl.pdf',      'seed-9102.pdf', 'application/pdf', 66120, 'seed/seed-9102.pdf', 9002, now() - interval '19 days', now()),
  ('dddd0000-0000-4000-8000-000000009103', 9002, 9001, 'TECHNICAL_INSPECTION', 'ITP',         'VALIDATED', 'itp-b102trl.pdf',                  'seed-9103.pdf', 'application/pdf', 51377, 'seed/seed-9103.pdf', 9003, now() - interval '15 days', now()),
  ('dddd0000-0000-4000-8000-000000009104', 9002, 9001, 'INSURANCE',            'RCA',         'VALIDATED', 'rca-b102trl.pdf',                  'seed-9104.pdf', 'application/pdf', 83950, 'seed/seed-9104.pdf', 9003, now() - interval '14 days', now()),
  ('dddd0000-0000-4000-8000-000000009105', 9003, 9001, 'INSURANCE',            'RCA',         'VALIDATED', 'rca-cj201trl.pdf',                 'seed-9105.pdf', 'application/pdf', 82044, 'seed/seed-9105.pdf', 9002, now() - interval '40 days', now()),
  ('dddd0000-0000-4000-8000-000000009106', 9003, 9001, 'EXPENSE_INVOICE',      'Anvelope',    'VALIDATED', 'factura-anvelope-cj201trl.pdf',    'seed-9106.pdf', 'application/pdf', 71230, 'seed/seed-9106.pdf', 9002, now() - interval '45 days', now()),
  ('dddd0000-0000-4000-8000-000000009107', 9004, 9002, 'INSURANCE',            'RCA',         'VALIDATED', 'rca-ag301agm.pdf',                 'seed-9107.pdf', 'application/pdf', 85512, 'seed/seed-9107.pdf', 9005, now() - interval '18 days', now()),
  ('dddd0000-0000-4000-8000-000000009108', 9005, 9002, 'TECHNICAL_INSPECTION', 'ITP',         'VALIDATED', 'itp-ag302agm.pdf',                 'seed-9108.pdf', 'application/pdf', 50780, 'seed/seed-9108.pdf', 9005, now() - interval '12 days', now()),
  ('dddd0000-0000-4000-8000-000000009109', 9005, 9002, 'EXPENSE_INVOICE',      'Combustibil', 'VALIDATED', 'factura-combustibil-ag302agm.pdf', 'seed-9109.pdf', 'application/pdf', 44890, 'seed/seed-9109.pdf', 9005, now() - interval '10 days', now()),
  ('dddd0000-0000-4000-8000-000000009110', 9006, 9002, 'ROAD_TAX',             'Rovinieta',   'VALIDATED', 'rovinieta-ag303agm.pdf',           'seed-9110.pdf', 'application/pdf', 39655, 'seed/seed-9110.pdf', 9005, now() - interval '9 days',  now())
ON CONFLICT DO NOTHING;

INSERT INTO approved_document_data (
  id, document_id, vehicle_id, business_id, document_type, subtype,
  approved_data, valid_from, valid_until, approved_by_user_id, approved_at,
  review_comment, status, created_at, updated_at
) VALUES
  ('adad0000-0000-4000-8000-000000009101', 'dddd0000-0000-4000-8000-000000009101', 9001, 9001, 'INSURANCE', 'RCA',
   jsonb_build_object('documentType','INSURANCE','subtype','RCA','policyNumber','RO/12/458821','licensePlate','B-101-TRL','totalAmount',890.00,'currency','RON','validFrom',to_char(CURRENT_DATE - 353,'YYYY-MM-DD'),'validUntil',to_char(CURRENT_DATE + 12,'YYYY-MM-DD')),
   CURRENT_DATE - 353, CURRENT_DATE + 12, 9001, now(), NULL, 'ACTIVE', now(), now()),
  ('adad0000-0000-4000-8000-000000009102', 'dddd0000-0000-4000-8000-000000009102', 9001, 9001, 'EXPENSE_INVOICE', 'Service',
   jsonb_build_object('documentType','EXPENSE_INVOICE','subtype','Service','invoiceNumber','SRV-2026-0187','expenseCategory','Service','licensePlate','B-101-TRL','totalAmount',1450.50,'currency','RON','invoiceDate',to_char(CURRENT_DATE - 20,'YYYY-MM-DD')),
   NULL, NULL, 9001, now(), NULL, 'ACTIVE', now(), now()),
  ('adad0000-0000-4000-8000-000000009103', 'dddd0000-0000-4000-8000-000000009103', 9002, 9001, 'TECHNICAL_INSPECTION', 'ITP',
   jsonb_build_object('documentType','TECHNICAL_INSPECTION','subtype','ITP','inspectionNumber','ITP-2025-88412','licensePlate','B-102-TRL','totalAmount',150.00,'currency','RON','validFrom',to_char(CURRENT_DATE - 340,'YYYY-MM-DD'),'validUntil',to_char(CURRENT_DATE + 25,'YYYY-MM-DD')),
   CURRENT_DATE - 340, CURRENT_DATE + 25, 9001, now(), NULL, 'ACTIVE', now(), now()),
  ('adad0000-0000-4000-8000-000000009104', 'dddd0000-0000-4000-8000-000000009104', 9002, 9001, 'INSURANCE', 'RCA',
   jsonb_build_object('documentType','INSURANCE','subtype','RCA','policyNumber','RO/12/501277','licensePlate','B-102-TRL','totalAmount',905.00,'currency','RON','validFrom',to_char(CURRENT_DATE - 115,'YYYY-MM-DD'),'validUntil',to_char(CURRENT_DATE + 250,'YYYY-MM-DD')),
   CURRENT_DATE - 115, CURRENT_DATE + 250, 9001, now(), NULL, 'ACTIVE', now(), now()),
  ('adad0000-0000-4000-8000-000000009105', 'dddd0000-0000-4000-8000-000000009105', 9003, 9001, 'INSURANCE', 'RCA',
   jsonb_build_object('documentType','INSURANCE','subtype','RCA','policyNumber','RO/12/377145','licensePlate','CJ-201-TRL','totalAmount',870.00,'currency','RON','validFrom',to_char(CURRENT_DATE - 370,'YYYY-MM-DD'),'validUntil',to_char(CURRENT_DATE - 5,'YYYY-MM-DD')),
   CURRENT_DATE - 370, CURRENT_DATE - 5, 9001, now(), NULL, 'ACTIVE', now(), now()),
  ('adad0000-0000-4000-8000-000000009106', 'dddd0000-0000-4000-8000-000000009106', 9003, 9001, 'EXPENSE_INVOICE', 'Anvelope',
   jsonb_build_object('documentType','EXPENSE_INVOICE','subtype','Anvelope','invoiceNumber','ANV-2026-0032','expenseCategory','Piese si consumabile','licensePlate','CJ-201-TRL','totalAmount',320.00,'currency','EUR','invoiceDate',to_char(CURRENT_DATE - 45,'YYYY-MM-DD')),
   NULL, NULL, 9001, now(), NULL, 'ACTIVE', now(), now()),
  ('adad0000-0000-4000-8000-000000009107', 'dddd0000-0000-4000-8000-000000009107', 9004, 9002, 'INSURANCE', 'RCA',
   jsonb_build_object('documentType','INSURANCE','subtype','RCA','policyNumber','RO/03/912236','licensePlate','AG-301-AGM','totalAmount',1120.00,'currency','RON','validFrom',to_char(CURRENT_DATE - 357,'YYYY-MM-DD'),'validUntil',to_char(CURRENT_DATE + 8,'YYYY-MM-DD')),
   CURRENT_DATE - 357, CURRENT_DATE + 8, 9004, now(), NULL, 'ACTIVE', now(), now()),
  ('adad0000-0000-4000-8000-000000009108', 'dddd0000-0000-4000-8000-000000009108', 9005, 9002, 'TECHNICAL_INSPECTION', 'ITP',
   jsonb_build_object('documentType','TECHNICAL_INSPECTION','subtype','ITP','inspectionNumber','ITP-2026-10233','licensePlate','AG-302-AGM','totalAmount',160.00,'currency','RON','validFrom',to_char(CURRENT_DATE - 65,'YYYY-MM-DD'),'validUntil',to_char(CURRENT_DATE + 300,'YYYY-MM-DD')),
   CURRENT_DATE - 65, CURRENT_DATE + 300, 9004, now(), NULL, 'ACTIVE', now(), now()),
  ('adad0000-0000-4000-8000-000000009109', 'dddd0000-0000-4000-8000-000000009109', 9005, 9002, 'EXPENSE_INVOICE', 'Combustibil',
   jsonb_build_object('documentType','EXPENSE_INVOICE','subtype','Combustibil','invoiceNumber','FUEL-2026-0455','expenseCategory','Combustibil','licensePlate','AG-302-AGM','totalAmount',780.25,'currency','RON','invoiceDate',to_char(CURRENT_DATE - 10,'YYYY-MM-DD')),
   NULL, NULL, 9004, now(), NULL, 'ACTIVE', now(), now()),
  ('adad0000-0000-4000-8000-000000009110', 'dddd0000-0000-4000-8000-000000009110', 9006, 9002, 'ROAD_TAX', 'Rovinieta',
   jsonb_build_object('documentType','ROAD_TAX','subtype','Rovinieta','documentTitle','Rovinieta 12 luni','licensePlate','AG-303-AGM','totalAmount',128.00,'currency','RON','validFrom',to_char(CURRENT_DATE - 345,'YYYY-MM-DD'),'validUntil',to_char(CURRENT_DATE + 20,'YYYY-MM-DD')),
   CURRENT_DATE - 345, CURRENT_DATE + 20, 9004, now(), NULL, 'ACTIVE', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO vehicle_document_attributes (
  id, vehicle_id, business_id, document_id, approved_data_id, document_type,
  subtype, license_plate, vin, valid_from, valid_until, source_data, status,
  created_at, updated_at
)
SELECT
  ('aaaa0000-0000-4000-8000-' || lpad(right(a.id::text, 6), 12, '0'))::uuid,
  a.vehicle_id, a.business_id, a.document_id, a.id, a.document_type,
  a.subtype, a.approved_data ->> 'licensePlate', NULL, a.valid_from,
  a.valid_until, a.approved_data, 'ACTIVE', now(), now()
FROM approved_document_data a
WHERE a.id::text LIKE 'adad0000-%'
ON CONFLICT DO NOTHING;

COMMIT;

-- Verificare rapida: ce va aparea in alerte (expira in <=30 zile sau expirat)
SELECT license_plate, document_type, subtype, valid_until,
       (valid_until - CURRENT_DATE) AS days_left
FROM vehicle_document_attributes
WHERE status = 'ACTIVE' AND valid_until IS NOT NULL
  AND valid_until <= CURRENT_DATE + 30
ORDER BY valid_until;
