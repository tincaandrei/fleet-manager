export type DocumentType = 'INSPECTION' | 'INSURANCE' | 'INVOICE' | 'REGISTRATION' | 'OTHER';
export type DocumentStatus = 'NEEDS_REVIEW' | 'VALIDATED' | 'REJECTED' | 'ARCHIVED';
export type ApprovedDataStatus = 'ACTIVE' | 'SUPERSEDED' | 'ARCHIVED';

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
  approvedData: ApprovedDataResponse | null;
}

export interface ReviewDocumentRequest {
  decision: 'APPROVE' | 'REJECT';
  approvedData?: Record<string, unknown>;
  comment?: string;
}
