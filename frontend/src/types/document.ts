export type DocumentType = 'INSPECTION' | 'INSURANCE' | 'INVOICE' | 'REGISTRATION' | 'OTHER';
export type DocumentStatus = 'PARSING' | 'NEEDS_REVIEW' | 'VALIDATED' | 'REJECTED' | 'FAILED_PARSING' | 'ARCHIVED';
export type ExtractionStatus = 'PARSED' | 'FAILED' | 'APPROVED' | 'REJECTED';
export type ApprovedDataStatus = 'ACTIVE' | 'SUPERSEDED' | 'ARCHIVED';

export interface ExtractionResponse {
  id: string;
  extractionStatus: ExtractionStatus;
  parserName: string | null;
  parserVersion: string | null;
  rawExtractedData: Record<string, unknown> | null;
  extractionConfidence: number | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApprovedDataResponse {
  id: string;
  approvedData: Record<string, unknown>;
  approvedByUserId: number;
  approvedAt: string;
  reviewComment: string | null;
  status: ApprovedDataStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentResponse {
  id: string;
  vehicleId: number;
  documentType: DocumentType;
  status: DocumentStatus;
  originalFileName: string;
  storedFileName: string;
  contentType: string;
  fileSize: number;
  storagePath: string;
  uploadedByUserId: number;
  createdAt: string;
  updatedAt: string;
  extraction: ExtractionResponse | null;
  approvedData: ApprovedDataResponse | null;
}

export interface ReviewDocumentRequest {
  decision: 'APPROVE' | 'REJECT';
  approvedData?: Record<string, unknown>;
  comment?: string;
}
