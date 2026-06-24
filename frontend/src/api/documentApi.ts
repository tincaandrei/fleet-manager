import api from './axios';
import type {
  DocumentResponse,
  DocumentHistoryItem,
  PagedResponse,
  ReviewDocumentRequest,
  VehicleDocumentAttributeResponse,
  VehicleAlertGroup,
  DocumentInfoFolder,
} from '../types/document';

// ── Document CRUD ─────────────────────────────────────────────────────────────

export const listDocumentsByVehicle = (vehicleId: number) =>
  api.get<DocumentResponse[]>('/api/documents', { params: { vehicleId } });

export const listDocumentHistory = (page: number, size: number) =>
  api.get<PagedResponse<DocumentHistoryItem>>('/api/documents/history', {
    params: { page, size },
  });

/**
 * Upload a document for a vehicle.
 * The parser determines documentType and documentSubtype automatically —
 * no documentType param is sent from the client.
 */
export const uploadDocument = (vehicleId: number, file: File) => {
  const form = new FormData();
  form.append('file', file);
  form.append('vehicleId', String(vehicleId));
  return api.post<DocumentResponse>('/api/documents', form);
};

export const downloadDocument = (documentId: string) =>
  api.get<Blob>(`/api/documents/${documentId}/download`, { responseType: 'blob' });

export const reviewDocument = (documentId: string, payload: ReviewDocumentRequest) =>
  api.post<DocumentResponse>(`/api/documents/${documentId}/review`, payload);

export const archiveDocument = (documentId: string) =>
  api.patch<DocumentResponse>(`/api/documents/${documentId}/archive`);

// ── Vehicle document attributes ───────────────────────────────────────────────

/**
 * Returns normalised ACTIVE approved document attributes for a vehicle.
 * Used by the Compliance tab to show per-document-type validity cards.
 * GET /api/documents/vehicles/{vehicleId}/attributes
 */
export const listVehicleDocumentAttributes = (vehicleId: number) =>
  api.get<VehicleDocumentAttributeResponse[]>(
    `/api/documents/vehicles/${vehicleId}/attributes`,
  );

// ── Expiration alerts (grouped by vehicle) ────────────────────────────────────

/**
 * Returns documents that are expired or expiring soon, grouped by vehicle.
 * Requires BUSINESS_ADMIN or SUPERADMIN.
 * GET /api/documents/alerts/vehicles?days=30&includeExpired=true
 */
export const listVehicleAlerts = (days: number, includeExpired: boolean) =>
  api.get<VehicleAlertGroup[]>('/api/documents/alerts/vehicles', {
    params: { days, includeExpired },
  });

// ── Document info folder ──────────────────────────────────────────────────────

/**
 * Returns the canonical and extra extracted fields for a document.
 * GET /api/documents/{id}/info-folder
 */
export const getDocumentInfoFolder = (documentId: string) =>
  api.get<DocumentInfoFolder>(`/api/documents/${documentId}/info-folder`);
