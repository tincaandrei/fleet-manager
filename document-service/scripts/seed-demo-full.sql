-- Full demo seed for the document database.
-- Idempotent and non-destructive: only fixed demo UUID ranges are upserted.
-- Files are metadata-only; downloads can fail unless matching PDFs exist in storage.

BEGIN;

CREATE TEMP TABLE seed_demo_documents (
    document_id uuid PRIMARY KEY,
    draft_id uuid NOT NULL,
    approved_id uuid,
    attribute_id uuid,
    vehicle_id bigint NOT NULL,
    business_id bigint NOT NULL,
    uploaded_by_user_id bigint,
    approved_by_user_id bigint,
    license_plate varchar(32),
    vin varchar(64),
    document_type varchar(64) NOT NULL,
    subtype varchar(64),
    document_status varchar(64) NOT NULL,
    parser_status varchar(64) NOT NULL,
    confidence numeric(5,4),
    valid_from date,
    valid_until date,
    approved_status varchar(64),
    original_file_name varchar(255) NOT NULL,
    stored_file_name varchar(255) NOT NULL,
    file_size bigint NOT NULL,
    review_comment text,
    error_code varchar(128),
    error_message text,
    warnings jsonb NOT NULL DEFAULT '[]'::jsonb,
    insurer varchar(128),
    provider varchar(128),
    amount numeric(12,2)
) ON COMMIT DROP;

INSERT INTO seed_demo_documents (
    document_id,
    draft_id,
    approved_id,
    attribute_id,
    vehicle_id,
    business_id,
    uploaded_by_user_id,
    approved_by_user_id,
    license_plate,
    vin,
    document_type,
    subtype,
    document_status,
    parser_status,
    confidence,
    valid_from,
    valid_until,
    approved_status,
    original_file_name,
    stored_file_name,
    file_size,
    review_comment,
    error_code,
    error_message,
    warnings,
    insurer,
    provider,
    amount
) VALUES
    ('00000000-0000-0000-0000-000000200001', '00000000-0000-0000-0000-000000500001', '00000000-0000-0000-0000-000000300001', '00000000-0000-0000-0000-000000400001', 1001, 101, 1002, 1001, 'B-101-ATL', 'WVWZZZ1KZAP100101', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9820, CURRENT_DATE - 375, CURRENT_DATE - 10, 'ACTIVE', 'b-101-atl-rca-current.pdf', 'demo-b-101-atl-rca-current.pdf', 184220, 'Expired RCA kept active for alerts demo.', NULL, NULL, '[]'::jsonb, 'Omniasig Vienna Insurance Group', NULL, NULL),
    ('00000000-0000-0000-0000-000000200002', '00000000-0000-0000-0000-000000500002', '00000000-0000-0000-0000-000000300002', '00000000-0000-0000-0000-000000400002', 1001, 101, 1002, 1001, 'B-101-ATL', 'WVWZZZ1KZAP100101', 'TECHNICAL_INSPECTION', 'ITP', 'VALIDATED', 'PARSED', 0.9670, CURRENT_DATE - 345, CURRENT_DATE + 20, 'ACTIVE', 'b-101-atl-itp-current.pdf', 'demo-b-101-atl-itp-current.pdf', 136540, 'ITP expires soon.', NULL, NULL, '[]'::jsonb, NULL, 'RAR Bucuresti', NULL),
    ('00000000-0000-0000-0000-000000200003', '00000000-0000-0000-0000-000000500003', '00000000-0000-0000-0000-000000300003', '00000000-0000-0000-0000-000000400003', 1001, 101, 1002, 1001, 'B-101-ATL', 'WVWZZZ1KZAP100101', 'ROAD_TAX', 'ROVINIETA', 'VALIDATED', 'PARSED', 0.9540, CURRENT_DATE - 185, CURRENT_DATE + 180, 'ACTIVE', 'b-101-atl-rovinieta-current.pdf', 'demo-b-101-atl-rovinieta-current.pdf', 94500, NULL, NULL, NULL, '[]'::jsonb, NULL, 'CNAIR', NULL),
    ('00000000-0000-0000-0000-000000200004', '00000000-0000-0000-0000-000000500004', '00000000-0000-0000-0000-000000300004', '00000000-0000-0000-0000-000000400004', 1002, 101, 1003, 1001, 'B-102-ATL', 'VF1MA000000100102', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9710, CURRENT_DATE - 358, CURRENT_DATE + 7, 'ACTIVE', 'b-102-atl-rca-current.pdf', 'demo-b-102-atl-rca-current.pdf', 172830, 'RCA expires in one week.', NULL, NULL, '[]'::jsonb, 'Groupama Asigurari', NULL, NULL),
    ('00000000-0000-0000-0000-000000200005', '00000000-0000-0000-0000-000000500005', '00000000-0000-0000-0000-000000300005', '00000000-0000-0000-0000-000000400005', 1002, 101, 1003, 1001, 'B-102-ATL', 'VF1MA000000100102', 'TECHNICAL_INSPECTION', 'ITP', 'VALIDATED', 'PARSED', 0.9480, CURRENT_DATE - 395, CURRENT_DATE - 30, 'ACTIVE', 'b-102-atl-itp-current.pdf', 'demo-b-102-atl-itp-current.pdf', 128450, 'Expired ITP for compliance demo.', NULL, NULL, '[]'::jsonb, NULL, 'RAR Bucuresti', NULL),
    ('00000000-0000-0000-0000-000000200006', '00000000-0000-0000-0000-000000500006', '00000000-0000-0000-0000-000000300006', '00000000-0000-0000-0000-000000400006', 1002, 101, 1003, 1001, 'B-102-ATL', 'VF1MA000000100102', 'ROAD_TAX', 'ROVINIETA', 'VALIDATED', 'PARSED', 0.9590, CURRENT_DATE - 305, CURRENT_DATE + 60, 'ACTIVE', 'b-102-atl-rovinieta-current.pdf', 'demo-b-102-atl-rovinieta-current.pdf', 88220, NULL, NULL, NULL, '[]'::jsonb, NULL, 'CNAIR', NULL),
    ('00000000-0000-0000-0000-000000200007', '00000000-0000-0000-0000-000000500007', '00000000-0000-0000-0000-000000300007', '00000000-0000-0000-0000-000000400007', 1003, 101, 1004, 1001, 'B-103-ATL', 'WBA8E91020K100103', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9880, CURRENT_DATE - 5, CURRENT_DATE + 365, 'ACTIVE', 'b-103-atl-rca-current.pdf', 'demo-b-103-atl-rca-current.pdf', 190010, NULL, NULL, NULL, '[]'::jsonb, 'Allianz-Tiriac Asigurari', NULL, NULL),
    ('00000000-0000-0000-0000-000000200008', '00000000-0000-0000-0000-000000500008', '00000000-0000-0000-0000-000000300008', '00000000-0000-0000-0000-000000400008', 1003, 101, 1004, 1001, 'B-103-ATL', 'WBA8E91020K100103', 'EXPENSE_INVOICE', NULL, 'VALIDATED', 'PARSED', 0.9330, CURRENT_DATE - 12, NULL, 'ACTIVE', 'b-103-atl-service-invoice.pdf', 'demo-b-103-atl-service-invoice.pdf', 211300, 'Service invoice approved for expense history.', NULL, NULL, '[]'::jsonb, NULL, 'Auto Total Service', 1840.50),
    ('00000000-0000-0000-0000-000000200009', '00000000-0000-0000-0000-000000500009', '00000000-0000-0000-0000-000000300009', '00000000-0000-0000-0000-000000400009', 1004, 101, 1005, 1001, 'B-104-ATL', 'ZFA2500000P100104', 'ROAD_TAX', 'ROVINIETA', 'VALIDATED', 'PARSED', 0.9460, CURRENT_DATE - 350, CURRENT_DATE + 15, 'ACTIVE', 'b-104-atl-rovinieta-current.pdf', 'demo-b-104-atl-rovinieta-current.pdf', 91840, 'Rovinieta expires within 15 days.', NULL, NULL, '[]'::jsonb, NULL, 'CNAIR', NULL),
    ('00000000-0000-0000-0000-000000200010', '00000000-0000-0000-0000-000000500010', '00000000-0000-0000-0000-000000300010', '00000000-0000-0000-0000-000000400010', 1005, 101, 1004, 1001, 'B-105-ATL', 'TMBJJ7NE0P0100105', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9690, CURRENT_DATE - 335, CURRENT_DATE + 30, 'ACTIVE', 'b-105-atl-rca-current.pdf', 'demo-b-105-atl-rca-current.pdf', 176120, NULL, NULL, NULL, '[]'::jsonb, 'Asirom Vienna Insurance Group', NULL, NULL),
    ('00000000-0000-0000-0000-000000200011', '00000000-0000-0000-0000-000000500011', '00000000-0000-0000-0000-000000300011', '00000000-0000-0000-0000-000000400011', 1006, 101, 1005, 1001, 'B-106-ATL', 'WF0XXXTTGXN100106', 'TECHNICAL_INSPECTION', 'ITP', 'VALIDATED', 'PARSED', 0.9420, CURRENT_DATE - 275, CURRENT_DATE + 90, 'ACTIVE', 'b-106-atl-itp-current.pdf', 'demo-b-106-atl-itp-current.pdf', 125760, NULL, NULL, NULL, '[]'::jsonb, NULL, 'RAR Bucuresti', NULL),
    ('00000000-0000-0000-0000-000000200012', '00000000-0000-0000-0000-000000500012', '00000000-0000-0000-0000-000000300012', '00000000-0000-0000-0000-000000400012', 1007, 101, 1003, 1001, 'B-107-ATL', 'NMTBB3JE90R100107', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9510, CURRENT_DATE - 370, CURRENT_DATE - 5, 'ACTIVE', 'b-107-atl-rca-current.pdf', 'demo-b-107-atl-rca-current.pdf', 169880, 'Expired RCA on inactive demo vehicle.', NULL, NULL, '[]'::jsonb, 'Generali Romania', NULL, NULL),

    ('00000000-0000-0000-0000-000000200013', '00000000-0000-0000-0000-000000500013', '00000000-0000-0000-0000-000000300013', '00000000-0000-0000-0000-000000400013', 1011, 102, 1012, 1011, 'CJ-201-NOV', 'WVWZZZAUZNP200201', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9770, CURRENT_DATE - 353, CURRENT_DATE + 12, 'ACTIVE', 'cj-201-nov-rca-current.pdf', 'demo-cj-201-nov-rca-current.pdf', 181600, 'RCA expires soon.', NULL, NULL, '[]'::jsonb, 'Allianz-Tiriac Asigurari', NULL, NULL),
    ('00000000-0000-0000-0000-000000200014', '00000000-0000-0000-0000-000000500014', '00000000-0000-0000-0000-000000300014', '00000000-0000-0000-0000-000000400014', 1011, 102, 1012, 1011, 'CJ-201-NOV', 'WVWZZZAUZNP200201', 'TECHNICAL_INSPECTION', 'ITP', 'VALIDATED', 'PARSED', 0.9560, CURRENT_DATE - 65, CURRENT_DATE + 300, 'ACTIVE', 'cj-201-nov-itp-current.pdf', 'demo-cj-201-nov-itp-current.pdf', 132900, NULL, NULL, NULL, '[]'::jsonb, NULL, 'RAR Cluj', NULL),
    ('00000000-0000-0000-0000-000000200015', '00000000-0000-0000-0000-000000500015', '00000000-0000-0000-0000-000000300015', '00000000-0000-0000-0000-000000400015', 1012, 102, 1013, 1011, 'CJ-202-NOV', 'VF1RFB00873200202', 'ROAD_TAX', 'ROVINIETA', 'VALIDATED', 'PARSED', 0.9610, CURRENT_DATE - 366, CURRENT_DATE - 1, 'ACTIVE', 'cj-202-nov-rovinieta-current.pdf', 'demo-cj-202-nov-rovinieta-current.pdf', 90480, 'Rovinieta expired yesterday.', NULL, NULL, '[]'::jsonb, NULL, 'CNAIR', NULL),
    ('00000000-0000-0000-0000-000000200016', '00000000-0000-0000-0000-000000500016', '00000000-0000-0000-0000-000000300016', '00000000-0000-0000-0000-000000400016', 1013, 102, 1014, 1011, 'CJ-203-NOV', 'ZFA2500000P200203', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9730, CURRENT_DATE - 320, CURRENT_DATE + 45, 'ACTIVE', 'cj-203-nov-rca-current.pdf', 'demo-cj-203-nov-rca-current.pdf', 179210, NULL, NULL, NULL, '[]'::jsonb, 'Groupama Asigurari', NULL, NULL),
    ('00000000-0000-0000-0000-000000200017', '00000000-0000-0000-0000-000000500017', '00000000-0000-0000-0000-000000300017', '00000000-0000-0000-0000-000000400017', 1014, 102, 1012, 1011, 'CJ-204-NOV', 'WDB9066331S200204', 'TECHNICAL_INSPECTION', 'ITP', 'VALIDATED', 'PARSED', 0.9490, CURRENT_DATE - 340, CURRENT_DATE + 25, 'ACTIVE', 'cj-204-nov-itp-current.pdf', 'demo-cj-204-nov-itp-current.pdf', 141260, 'ITP expires within 30 days.', NULL, NULL, '[]'::jsonb, NULL, 'RAR Cluj', NULL),
    ('00000000-0000-0000-0000-000000200018', '00000000-0000-0000-0000-000000500018', '00000000-0000-0000-0000-000000300018', '00000000-0000-0000-0000-000000400018', 1015, 102, 1013, 1011, 'CJ-205-NOV', 'TMBJJ7NE0P0200205', 'ROAD_TAX', 'ROVINIETA', 'VALIDATED', 'PARSED', 0.9580, CURRENT_DATE - 1, CURRENT_DATE + 365, 'ACTIVE', 'cj-205-nov-rovinieta-current.pdf', 'demo-cj-205-nov-rovinieta-current.pdf', 87440, NULL, NULL, NULL, '[]'::jsonb, NULL, 'CNAIR', NULL),
    ('00000000-0000-0000-0000-000000200019', '00000000-0000-0000-0000-000000500019', '00000000-0000-0000-0000-000000300019', '00000000-0000-0000-0000-000000400019', 1016, 102, 1014, 1011, 'CJ-206-NOV', 'WF0XXXTTGXN200206', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9440, CURRENT_DATE - 410, CURRENT_DATE - 45, 'ACTIVE', 'cj-206-nov-rca-current.pdf', 'demo-cj-206-nov-rca-current.pdf', 168700, 'Expired insurance for service vehicle.', NULL, NULL, '[]'::jsonb, 'Omniasig Vienna Insurance Group', NULL, NULL),
    ('00000000-0000-0000-0000-000000200020', '00000000-0000-0000-0000-000000500020', '00000000-0000-0000-0000-000000300020', '00000000-0000-0000-0000-000000400020', 1017, 102, 1013, 1011, 'CJ-207-NOV', 'NMTBB3JE90R200207', 'EXPENSE_INVOICE', NULL, 'VALIDATED', 'PARSED', 0.9370, CURRENT_DATE - 20, NULL, 'ACTIVE', 'cj-207-nov-service-invoice.pdf', 'demo-cj-207-nov-service-invoice.pdf', 198220, NULL, NULL, NULL, '[]'::jsonb, NULL, 'Auto Check Center', 1225.99),

    ('00000000-0000-0000-0000-000000200021', '00000000-0000-0000-0000-000000500021', '00000000-0000-0000-0000-000000300021', '00000000-0000-0000-0000-000000400021', 1021, 103, 1022, 1021, 'TM-301-MED', 'WVWZZZ1KZAP300301', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9750, CURRENT_DATE - 357, CURRENT_DATE + 8, 'ACTIVE', 'tm-301-med-rca-current.pdf', 'demo-tm-301-med-rca-current.pdf', 182910, 'RCA expires soon.', NULL, NULL, '[]'::jsonb, 'Asirom Vienna Insurance Group', NULL, NULL),
    ('00000000-0000-0000-0000-000000200022', '00000000-0000-0000-0000-000000500022', '00000000-0000-0000-0000-000000300022', '00000000-0000-0000-0000-000000400022', 1021, 103, 1022, 1021, 'TM-301-MED', 'WVWZZZ1KZAP300301', 'TECHNICAL_INSPECTION', 'ITP', 'VALIDATED', 'PARSED', 0.9520, CURRENT_DATE - 385, CURRENT_DATE - 20, 'ACTIVE', 'tm-301-med-itp-current.pdf', 'demo-tm-301-med-itp-current.pdf', 130050, 'Expired ITP for delivery car.', NULL, NULL, '[]'::jsonb, NULL, 'RAR Timis', NULL),
    ('00000000-0000-0000-0000-000000200023', '00000000-0000-0000-0000-000000500023', '00000000-0000-0000-0000-000000300023', '00000000-0000-0000-0000-000000400023', 1022, 103, 1023, 1021, 'TM-302-MED', 'VF1RFB00873300302', 'ROAD_TAX', 'ROVINIETA', 'VALIDATED', 'PARSED', 0.9630, CURRENT_DATE - 245, CURRENT_DATE + 120, 'ACTIVE', 'tm-302-med-rovinieta-current.pdf', 'demo-tm-302-med-rovinieta-current.pdf', 86570, NULL, NULL, NULL, '[]'::jsonb, NULL, 'CNAIR', NULL),
    ('00000000-0000-0000-0000-000000200024', '00000000-0000-0000-0000-000000500024', '00000000-0000-0000-0000-000000300024', '00000000-0000-0000-0000-000000400024', 1023, 103, 1024, 1021, 'TM-303-MED', 'WBA8E91020K300303', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9860, CURRENT_DATE - 20, CURRENT_DATE + 400, 'ACTIVE', 'tm-303-med-rca-current.pdf', 'demo-tm-303-med-rca-current.pdf', 188440, NULL, NULL, NULL, '[]'::jsonb, 'Allianz-Tiriac Asigurari', NULL, NULL),
    ('00000000-0000-0000-0000-000000200025', '00000000-0000-0000-0000-000000500025', '00000000-0000-0000-0000-000000300025', '00000000-0000-0000-0000-000000400025', 1024, 103, 1022, 1021, 'TM-304-MED', 'ZFA2500000P300304', 'TECHNICAL_INSPECTION', 'ITP', 'VALIDATED', 'PARSED', 0.9470, CURRENT_DATE - 336, CURRENT_DATE + 29, 'ACTIVE', 'tm-304-med-itp-current.pdf', 'demo-tm-304-med-itp-current.pdf', 139780, 'ITP expires within 30 days.', NULL, NULL, '[]'::jsonb, NULL, 'RAR Timis', NULL),
    ('00000000-0000-0000-0000-000000200026', '00000000-0000-0000-0000-000000500026', '00000000-0000-0000-0000-000000300026', '00000000-0000-0000-0000-000000400026', 1025, 103, 1023, 1021, 'TM-305-MED', 'TMBJJ7NE0P0300305', 'ROAD_TAX', 'ROVINIETA', 'VALIDATED', 'PARSED', 0.9410, CURRENT_DATE - 425, CURRENT_DATE - 60, 'ACTIVE', 'tm-305-med-rovinieta-current.pdf', 'demo-tm-305-med-rovinieta-current.pdf', 84330, 'Expired road tax for alert list.', NULL, NULL, '[]'::jsonb, NULL, 'CNAIR', NULL),
    ('00000000-0000-0000-0000-000000200027', '00000000-0000-0000-0000-000000500027', '00000000-0000-0000-0000-000000300027', '00000000-0000-0000-0000-000000400027', 1026, 103, 1024, 1021, 'TM-306-MED', 'WF0XXXTTGXN300306', 'INSURANCE', 'RCA', 'VALIDATED', 'PARSED', 0.9530, CURRENT_DATE - 305, CURRENT_DATE + 60, 'ACTIVE', 'tm-306-med-rca-current.pdf', 'demo-tm-306-med-rca-current.pdf', 171390, NULL, NULL, NULL, '[]'::jsonb, 'Generali Romania', NULL, NULL),
    ('00000000-0000-0000-0000-000000200028', '00000000-0000-0000-0000-000000500028', '00000000-0000-0000-0000-000000300028', '00000000-0000-0000-0000-000000400028', 1028, 103, 1023, 1021, 'TM-308-MED', 'NMTBB3JE90R300308', 'EXPENSE_INVOICE', NULL, 'VALIDATED', 'PARSED', 0.9290, CURRENT_DATE - 18, NULL, 'ACTIVE', 'tm-308-med-tires-invoice.pdf', 'demo-tm-308-med-tires-invoice.pdf', 205810, NULL, NULL, NULL, '[]'::jsonb, NULL, 'Pneumatic Center Timisoara', 2675.00),

    ('00000000-0000-0000-0000-000000200029', '00000000-0000-0000-0000-000000500029', NULL, NULL, 1008, 101, 1005, NULL, 'B-108-ATL', 'WVWZZZAUZNP100108', 'INSURANCE', 'RCA', 'NEEDS_REVIEW', 'PARSED', 0.7130, CURRENT_DATE - 40, CURRENT_DATE + 325, NULL, 'b-108-atl-rca-review.pdf', 'demo-b-108-atl-rca-review.pdf', 162720, 'Policy number is partially unreadable; needs manual confirmation.', NULL, NULL, '["policyNumber: Low OCR confidence"]'::jsonb, 'Omniasig Vienna Insurance Group', NULL, NULL),
    ('00000000-0000-0000-0000-000000200030', '00000000-0000-0000-0000-000000500030', NULL, NULL, 1018, 102, 1014, NULL, 'CJ-208-NOV', 'WVWZZZAUZNP200208', 'ROAD_TAX', 'ROVINIETA', 'NEEDS_REVIEW', 'PARSED', 0.6880, CURRENT_DATE - 2, CURRENT_DATE + 363, NULL, 'cj-208-nov-rovinieta-review.pdf', 'demo-cj-208-nov-rovinieta-review.pdf', 79920, 'License plate detection differs from vehicle record.', NULL, NULL, '["licensePlate: Detected CJ-280-NOV, expected CJ-208-NOV"]'::jsonb, NULL, 'CNAIR', NULL),
    ('00000000-0000-0000-0000-000000200031', '00000000-0000-0000-0000-000000500031', NULL, NULL, 1027, 103, 1024, NULL, 'TM-307-MED', 'WVWZZZAUZNP300307', 'TECHNICAL_INSPECTION', 'ITP', 'PARSING_FAILED', 'FAILED', NULL, NULL, NULL, NULL, 'tm-307-med-itp-failed.pdf', 'demo-tm-307-med-itp-failed.pdf', 73420, 'Unreadable scan kept in parsing failed queue.', 'OCR_NO_TEXT', 'The parser could not extract readable text from the uploaded file.', '[]'::jsonb, NULL, 'RAR Timis', NULL),
    ('00000000-0000-0000-0000-000000200032', '00000000-0000-0000-0000-000000500032', NULL, NULL, 1004, 101, 1005, NULL, 'B-104-ATL', 'ZFA2500000P100104', 'EXPENSE_INVOICE', NULL, 'REJECTED', 'PARSED', 0.8420, CURRENT_DATE - 33, NULL, NULL, 'b-104-atl-rejected-invoice.pdf', 'demo-b-104-atl-rejected-invoice.pdf', 221440, 'Rejected: invoice belongs to another supplier contract.', NULL, NULL, '["supplier: Supplier not allowed for this organization"]'::jsonb, NULL, 'Unknown Service SRL', 920.00),

    ('00000000-0000-0000-0000-000000200033', '00000000-0000-0000-0000-000000500033', '00000000-0000-0000-0000-000000300033', '00000000-0000-0000-0000-000000400033', 1001, 101, 1002, 1001, 'B-101-ATL', 'WVWZZZ1KZAP100101', 'INSURANCE', 'RCA', 'ARCHIVED', 'PARSED', 0.9650, CURRENT_DATE - 745, CURRENT_DATE - 380, 'SUPERSEDED', 'b-101-atl-rca-archive.pdf', 'demo-b-101-atl-rca-archive.pdf', 169340, 'Superseded by newer RCA document.', NULL, NULL, '[]'::jsonb, 'Groupama Asigurari', NULL, NULL),
    ('00000000-0000-0000-0000-000000200034', '00000000-0000-0000-0000-000000500034', '00000000-0000-0000-0000-000000300034', '00000000-0000-0000-0000-000000400034', 1011, 102, 1012, 1011, 'CJ-201-NOV', 'WVWZZZAUZNP200201', 'ROAD_TAX', 'ROVINIETA', 'ARCHIVED', 'PARSED', 0.9520, CURRENT_DATE - 565, CURRENT_DATE - 200, 'ARCHIVED', 'cj-201-nov-rovinieta-archive.pdf', 'demo-cj-201-nov-rovinieta-archive.pdf', 84440, 'Archived road tax proof from previous year.', NULL, NULL, '[]'::jsonb, NULL, 'CNAIR', NULL),
    ('00000000-0000-0000-0000-000000200035', '00000000-0000-0000-0000-000000500035', '00000000-0000-0000-0000-000000300035', '00000000-0000-0000-0000-000000400035', 1021, 103, 1022, 1021, 'TM-301-MED', 'WVWZZZ1KZAP300301', 'INSURANCE', 'RCA', 'ARCHIVED', 'PARSED', 0.9680, CURRENT_DATE - 785, CURRENT_DATE - 420, 'SUPERSEDED', 'tm-301-med-rca-archive.pdf', 'demo-tm-301-med-rca-archive.pdf', 170880, 'Superseded by current insurance policy.', NULL, NULL, '[]'::jsonb, 'Generali Romania', NULL, NULL);

UPDATE seed_demo_documents AS s
SET
    license_plate = v.license_plate,
    vin = v.vin
FROM (
    VALUES
        (1001, 'B-101-ATL', 'WVWZZZ1KZ8W101001'),
        (1002, 'B-102-ATL', 'VF1RFB00368101002'),
        (1003, 'B-103-ATL', 'TMBJJ7NE8M0101003'),
        (1004, 'B-104-ATL', 'WF0EXXTTGE1101004'),
        (1005, 'B-105-ATL', 'KMHLC41UBLU101005'),
        (1006, 'B-106-ATL', 'UU1DJF00267101006'),
        (1007, 'B-107-ATL', 'WDB9076331P101007'),
        (1008, 'B-108-ATL', 'VF3M45GFRNS101008'),
        (1011, 'CJ-201-NOV', 'WBA3D31040J102011'),
        (1012, 'CJ-202-NOV', 'ZFA2500000N102012'),
        (1013, 'CJ-203-NOV', 'TMAJ3815GLJ102013'),
        (1014, 'CJ-204-NOV', 'WMA06XZZ1MM102014'),
        (1015, 'CJ-205-NOV', 'LRWYGCEK5LC102015'),
        (1016, 'CJ-206-NOV', 'VF7ACBHZHNY102016'),
        (1017, 'CJ-207-NOV', 'WV1ZZZ7HZKH102017'),
        (1018, 'CJ-208-NOV', 'SB1K93BE20E102018'),
        (1021, 'IS-301-MED', 'VF3YCBMFC12103021'),
        (1022, 'IS-302-MED', 'KMHLC41UBL1U03022'),
        (1023, 'IS-303-MED', 'WF0DXXTTRD1103023'),
        (1024, 'IS-304-MED', 'VF1FL000668103024'),
        (1025, 'IS-305-MED', 'JTDKARFPXK3103025'),
        (1026, 'IS-306-MED', 'WDB4476031P103026'),
        (1027, 'IS-307-MED', 'SJNFAAJ11U1103027'),
        (1028, 'IS-308-MED', 'VF1RJA00768103028')
) AS v(vehicle_id, license_plate, vin)
WHERE s.vehicle_id = v.vehicle_id;

UPDATE seed_demo_documents
SET
    original_file_name = replace(original_file_name, 'tm-', 'is-'),
    stored_file_name = replace(stored_file_name, 'tm-', 'is-')
WHERE business_id = 103;

INSERT INTO documents (
    id,
    vehicle_id,
    business_id,
    document_type,
    document_subtype,
    status,
    original_file_name,
    stored_file_name,
    content_type,
    file_size,
    storage_path,
    uploaded_by_user_id,
    created_at,
    updated_at
)
SELECT
    document_id,
    vehicle_id,
    business_id,
    document_type,
    subtype,
    document_status,
    original_file_name,
    stored_file_name,
    'application/pdf',
    file_size,
    '/app/document-storage/demo/' || stored_file_name,
    uploaded_by_user_id,
    NOW() - INTERVAL '10 days',
    NOW()
FROM seed_demo_documents
ON CONFLICT (id) DO UPDATE SET
    vehicle_id = EXCLUDED.vehicle_id,
    business_id = EXCLUDED.business_id,
    document_type = EXCLUDED.document_type,
    document_subtype = EXCLUDED.document_subtype,
    status = EXCLUDED.status,
    original_file_name = EXCLUDED.original_file_name,
    stored_file_name = EXCLUDED.stored_file_name,
    content_type = EXCLUDED.content_type,
    file_size = EXCLUDED.file_size,
    storage_path = EXCLUDED.storage_path,
    uploaded_by_user_id = EXCLUDED.uploaded_by_user_id,
    updated_at = NOW();

INSERT INTO document_extraction_draft (
    id,
    document_id,
    detected_document_type,
    detected_subtype,
    confidence,
    extracted_data,
    warnings,
    parser_name,
    parser_version,
    parser_status,
    error_code,
    error_message,
    created_at,
    updated_at
)
SELECT
    draft_id,
    document_id,
    document_type,
    subtype,
    confidence,
    jsonb_strip_nulls(jsonb_build_object(
        'documentType', document_type,
        'subtype', subtype,
        'licensePlate', license_plate,
        'vin', vin,
        'validFrom', valid_from,
        'validUntil', valid_until,
        'insurer', insurer,
        'provider', provider,
        'amount', amount,
        'source', 'demo-seed'
    )),
    warnings,
    CASE
        WHEN document_type = 'EXPENSE_INVOICE' THEN 'invoice-demo-parser'
        ELSE 'romanian-vehicle-doc-demo-parser'
    END,
    '1.0.0',
    parser_status,
    error_code,
    error_message,
    NOW() - INTERVAL '10 days',
    NOW()
FROM seed_demo_documents
ON CONFLICT (document_id) DO UPDATE SET
    id = EXCLUDED.id,
    detected_document_type = EXCLUDED.detected_document_type,
    detected_subtype = EXCLUDED.detected_subtype,
    confidence = EXCLUDED.confidence,
    extracted_data = EXCLUDED.extracted_data,
    warnings = EXCLUDED.warnings,
    parser_name = EXCLUDED.parser_name,
    parser_version = EXCLUDED.parser_version,
    parser_status = EXCLUDED.parser_status,
    error_code = EXCLUDED.error_code,
    error_message = EXCLUDED.error_message,
    updated_at = NOW();

INSERT INTO approved_document_data (
    id,
    document_id,
    vehicle_id,
    business_id,
    document_type,
    subtype,
    approved_data,
    valid_from,
    valid_until,
    approved_by_user_id,
    approved_at,
    review_comment,
    status,
    created_at,
    updated_at
)
SELECT
    approved_id,
    document_id,
    vehicle_id,
    business_id,
    document_type,
    subtype,
    jsonb_strip_nulls(jsonb_build_object(
        'documentType', document_type,
        'subtype', subtype,
        'licensePlate', license_plate,
        'vin', vin,
        'validFrom', valid_from,
        'validUntil', valid_until,
        'insurer', insurer,
        'provider', provider,
        'amount', amount,
        'reviewComment', review_comment,
        'source', 'demo-seed'
    )),
    valid_from,
    valid_until,
    approved_by_user_id,
    NOW() - INTERVAL '8 days',
    review_comment,
    approved_status,
    NOW() - INTERVAL '8 days',
    NOW()
FROM seed_demo_documents
WHERE approved_id IS NOT NULL
ON CONFLICT (document_id) DO UPDATE SET
    id = EXCLUDED.id,
    vehicle_id = EXCLUDED.vehicle_id,
    business_id = EXCLUDED.business_id,
    document_type = EXCLUDED.document_type,
    subtype = EXCLUDED.subtype,
    approved_data = EXCLUDED.approved_data,
    valid_from = EXCLUDED.valid_from,
    valid_until = EXCLUDED.valid_until,
    approved_by_user_id = EXCLUDED.approved_by_user_id,
    approved_at = EXCLUDED.approved_at,
    review_comment = EXCLUDED.review_comment,
    status = EXCLUDED.status,
    updated_at = NOW();

INSERT INTO vehicle_document_attributes (
    id,
    vehicle_id,
    business_id,
    document_id,
    approved_data_id,
    document_type,
    subtype,
    license_plate,
    vin,
    valid_from,
    valid_until,
    source_data,
    status,
    created_at,
    updated_at
)
SELECT
    attribute_id,
    vehicle_id,
    business_id,
    document_id,
    approved_id,
    document_type,
    subtype,
    license_plate,
    vin,
    valid_from,
    valid_until,
    jsonb_strip_nulls(jsonb_build_object(
        'documentType', document_type,
        'subtype', subtype,
        'licensePlate', license_plate,
        'vin', vin,
        'validFrom', valid_from,
        'validUntil', valid_until,
        'insurer', insurer,
        'provider', provider,
        'amount', amount,
        'source', 'demo-seed'
    )),
    approved_status,
    NOW() - INTERVAL '8 days',
    NOW()
FROM seed_demo_documents
WHERE approved_id IS NOT NULL
ON CONFLICT (document_id) DO UPDATE SET
    id = EXCLUDED.id,
    vehicle_id = EXCLUDED.vehicle_id,
    business_id = EXCLUDED.business_id,
    approved_data_id = EXCLUDED.approved_data_id,
    document_type = EXCLUDED.document_type,
    subtype = EXCLUDED.subtype,
    license_plate = EXCLUDED.license_plate,
    vin = EXCLUDED.vin,
    valid_from = EXCLUDED.valid_from,
    valid_until = EXCLUDED.valid_until,
    source_data = EXCLUDED.source_data,
    status = EXCLUDED.status,
    updated_at = NOW();

COMMIT;
