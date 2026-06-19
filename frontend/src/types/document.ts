// ── Canonical document types ───────────────────────────────────────────────────
// These are the only types/subtypes the backend parser produces.
// CIV / REGISTRATION must NOT appear anywhere in the UI.

export type CanonicalDocumentType =
  | 'INSURANCE'
  | 'TECHNICAL_INSPECTION'
  | 'ROAD_TAX'
  | 'EXPENSE_INVOICE'
  | 'OTHER';

export type CanonicalSubtype = 'RCA' | 'ITP' | 'ROVINIETA' | 'UNKNOWN';

// ── Document & status enums ───────────────────────────────────────────────────

export type DocumentStatus =
  | 'PARSING'
  | 'PARSING_FAILED'
  | 'NEEDS_REVIEW'
  | 'VALIDATED'
  | 'REJECTED'
  | 'ARCHIVED';
export type ApprovedDataStatus = 'ACTIVE' | 'SUPERSEDED' | 'ARCHIVED';
export type ParserStatus =
  | 'PARSED'
  | 'FAILED'
  | 'PARTIAL'
  | 'PENDING';

// ── Extraction draft (unapproved parser output) ───────────────────────────────

export interface DocumentExtractionResponse {
  detectedDocumentType: string | null;
  detectedSubtype: string | null;
  confidence: number | null;          // BigDecimal → number
  extractedData: Record<string, unknown> | null;
  warnings: string[] | null;
  parserName: string | null;
  parserVersion: string | null;
  parserStatus: ParserStatus;
  errorCode: string | null;
  errorMessage: string | null;
}

// ── Approved document data (post-review) ──────────────────────────────────────

export interface ApprovedDocumentDataResponse {
  documentType: string | null;
  subtype: string | null;
  validFrom: string | null;           // LocalDate → ISO string "YYYY-MM-DD"
  validUntil: string | null;
  approvedData: Record<string, unknown> | null;
  approvedBy: number | null;
  approvedAt: string | null;          // Instant → ISO string
}

// ── Document ──────────────────────────────────────────────────────────────────

export interface DocumentResponse {
  id: string;
  vehicleId: number;
  /** The parser-determined canonical document type (no manual override at upload). */
  documentType: string;
  status: DocumentStatus;
  originalFileName: string;
  storedFileName: string;
  contentType: string;
  fileSize: number;
  storagePath: string;
  uploadedByUserId: number;
  createdAt: string;
  updatedAt: string;
  extraction: DocumentExtractionResponse | null;
  approvedData: ApprovedDocumentDataResponse | null;
}

export interface ReviewDocumentRequest {
  decision: 'APPROVE' | 'REJECT';
  approvedData?: Record<string, unknown>;
  comment?: string;
}

// ── Vehicle document attributes (post-approval normalised rows) ───────────────

export interface VehicleDocumentAttributeResponse {
  id: string;
  vehicleId: number;
  documentId: string;
  approvedDataId: string;
  documentType: string;
  subtype: string | null;
  licensePlate: string | null;
  vin: string | null;
  validFrom: string | null;           // LocalDate → "YYYY-MM-DD"
  validUntil: string | null;
  sourceData: Record<string, unknown> | null;
  status: ApprovedDataStatus;
  createdAt: string;
  updatedAt: string;
}

// ── Grouped alerts (GET /api/documents/alerts/vehicles) ───────────────────────

export interface VehicleAlertGroup {
  vehicleId: number;
  licensePlate: string | null;
  vin: string | null;
  brand: string | null;
  model: string | null;
  alerts: VehicleDocumentAttributeResponse[];
}

// ── Document info folder ──────────────────────────────────────────────────────

export interface DocumentInfoFolder {
  documentId?: string;
  canonicalFields: Record<string, unknown>;
  extraFields: Record<string, unknown>;
}

// ── Compliance status helpers ─────────────────────────────────────────────────

export type ComplianceStatus = 'valid' | 'expiring' | 'expired' | 'missing';

/**
 * Derives a compliance status from a validUntil ISO date string.
 * "Expiring" means within 30 days from today.
 */
export function computeComplianceStatus(validUntil: string | null | undefined): ComplianceStatus {
  if (!validUntil) return 'missing';
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const until = new Date(validUntil);
  until.setHours(0, 0, 0, 0);
  if (until < today) return 'expired';
  const msIn30Days = 30 * 24 * 60 * 60 * 1000;
  if (until.getTime() - today.getTime() <= msIn30Days) return 'expiring';
  return 'valid';
}

/**
 * Returns the number of days between today and validUntil.
 * Negative means already expired.
 */
export function daysUntil(validUntil: string | null | undefined): number | null {
  if (!validUntil) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const until = new Date(validUntil);
  until.setHours(0, 0, 0, 0);
  return Math.round((until.getTime() - today.getTime()) / (24 * 60 * 60 * 1000));
}
