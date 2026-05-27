import api from './axios';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserProfile,
  Business,
  BusinessRequest,
  BusinessUser,
  CreateBusinessUserRequest,
  UpdateRoleRequest,
  UpdateUserRequest,
} from '../types/auth';

// ── Auth ──────────────────────────────────────────────────────────────────────

export const login = (data: LoginRequest) =>
  api.post<LoginResponse>('/api/auth/login', data);

export const register = (data: RegisterRequest) =>
  api.post('/api/auth/register', data);

export const getMe = () =>
  api.get<UserProfile>('/api/auth/users/me');

export const updateMe = (data: UpdateUserRequest) =>
  api.put<UserProfile>('/api/auth/users/me', data);

export const updateUser = (userId: number, data: UpdateUserRequest) =>
  api.put<UserProfile>(`/api/auth/users/${userId}`, data);

// ── Business management (SUPERADMIN) ─────────────────────────────────────────

export const listBusinesses = () =>
  api.get<Business[]>('/api/auth/businesses');

export const getBusiness = (businessId: number) =>
  api.get<Business>(`/api/auth/businesses/${businessId}`);

export const createBusiness = (data: BusinessRequest) =>
  api.post<Business>('/api/auth/businesses', data);

export const updateBusiness = (businessId: number, data: BusinessRequest) =>
  api.put<Business>(`/api/auth/businesses/${businessId}`, data);

// ── Business user management (SUPERADMIN + BUSINESS_ADMIN for own business) ───

export const listBusinessUsers = (businessId: number) =>
  api.get<BusinessUser[]>(`/api/auth/businesses/${businessId}/users`);

export const createBusinessUser = (businessId: number, data: CreateBusinessUserRequest) =>
  api.post<BusinessUser>(`/api/auth/businesses/${businessId}/users`, data);

export const updateBusinessUser = (businessId: number, userId: number, data: UpdateUserRequest) =>
  api.put<BusinessUser>(`/api/auth/businesses/${businessId}/users/${userId}`, data);

export const updateBusinessUserRole = (businessId: number, userId: number, data: UpdateRoleRequest) =>
  api.put<BusinessUser>(`/api/auth/businesses/${businessId}/users/${userId}/role`, data);
