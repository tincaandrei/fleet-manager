import api from './axios';
import type { DocumentResponse, ReviewDocumentRequest } from '../types/document';

export const listDocumentsByVehicle = (vehicleId: number) =>
  api.get<DocumentResponse[]>('/api/documents', { params: { vehicleId } });

export const uploadDocument = (vehicleId: number, documentType: string, file: File) => {
  const form = new FormData();
  form.append('file', file);
  form.append('vehicleId', String(vehicleId));
  form.append('documentType', documentType);
  return api.post<DocumentResponse>('/api/documents', form);
};

export const downloadDocument = (documentId: string) =>
  api.get<Blob>(`/api/documents/${documentId}/download`, { responseType: 'blob' });

export const reviewDocument = (documentId: string, payload: ReviewDocumentRequest) =>
  api.post<DocumentResponse>(`/api/documents/${documentId}/review`, payload);

export const archiveDocument = (documentId: string) =>
  api.patch<DocumentResponse>(`/api/documents/${documentId}/archive`);
