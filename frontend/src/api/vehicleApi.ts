import api from './axios';
import type { Vehicle, VehicleRequest, VehicleFilters } from '../types/vehicle';

export const getVehicles = (filters?: VehicleFilters) =>
  api.get<Vehicle[]>('/api/fleet/vehicles', { params: filters });

export const getVehicle = (id: number) =>
  api.get<Vehicle>(`/api/fleet/vehicles/${id}`);

export const createVehicle = (data: VehicleRequest) =>
  api.post<Vehicle>('/api/fleet/vehicles', data);

export const updateVehicle = (id: number, data: VehicleRequest) =>
  api.put<Vehicle>(`/api/fleet/vehicles/${id}`, data);

export const uploadVehicleImage = (id: number, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post<Vehicle>(`/api/fleet/vehicles/${id}/image`, formData);
};

export const getVehicleImage = (imageUrl: string, cacheKey?: string | number | null) => {
  const separator = imageUrl.includes('?') ? '&' : '?';
  const url = cacheKey ? `${imageUrl}${separator}v=${encodeURIComponent(String(cacheKey))}` : imageUrl;
  return api.get<Blob>(url, { responseType: 'blob' });
};

export const deleteVehicleImage = (id: number) =>
  api.delete<Vehicle>(`/api/fleet/vehicles/${id}/image`);

export const deleteVehicle = (id: number) =>
  api.delete(`/api/fleet/vehicles/${id}`);
