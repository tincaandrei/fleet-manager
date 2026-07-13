import api from './axios';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserProfile,
  Business,
  BusinessRequest,
  BusinessUser,
  AssignBusinessUserRequest,
  InviteUserRequest,
  AdminUserResponse,
  AcceptInviteRequest,
  InviteValidationResponse,
  UpdateRoleRequest,
  UpdateUserRequest,
  UserStatus,
} from '../types/auth';

// ── Auth ──────────────────────────────────────────────────────────────────────

export const login = (data: LoginRequest) =>
  api.post<LoginResponse>('/api/auth/login', data);

export const register = (data: RegisterRequest) =>
  api.post('/api/auth/register', data);

export const validateInvitation = (token: string) =>
  api.get<InviteValidationResponse>('/api/auth/invitations/validate', { params: { token } });

export const acceptInvitation = (data: AcceptInviteRequest) =>
  api.post('/api/auth/accept-invite', data);

export const getMe = () =>
  api.get<UserProfile>('/api/auth/users/me');

export const updateMe = (data: UpdateUserRequest) =>
  api.put<UserProfile>('/api/auth/users/me', data);

export const uploadMyProfileImage = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post<UserProfile>('/api/auth/users/me/profile-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const getProfileImage = (imageUrl: string) =>
  api.get<Blob>(imageUrl, { responseType: 'blob' });

export const deleteMyProfileImage = () =>
  api.delete<UserProfile>('/api/auth/users/me/profile-image');

export const updateUser = (userId: number, data: UpdateUserRequest) =>
  api.put<UserProfile>(`/api/auth/users/${userId}`, data);

export const listUnassignedUsers = () =>
  api.get<UserProfile[]>('/api/auth/users/unassigned');

export const assignUnassignedUser = (userId: number, data: AssignBusinessUserRequest) =>
  api.put<UserProfile>(`/api/auth/users/${userId}/assignment`, data);

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

export const inviteUser = (data: InviteUserRequest) =>
  api.post<AdminUserResponse>('/api/auth/admin/users', data);

export const resendUserInvite = (userId: number) =>
  api.post<AdminUserResponse>(`/api/auth/admin/users/${userId}/resend-invite`);

export const sendPasswordResetLink = (userId: number) =>
  api.post<AdminUserResponse>(`/api/auth/admin/users/${userId}/password-reset-link`);

export const updateAdminUserStatus = (userId: number, status: UserStatus) =>
  api.patch<AdminUserResponse>(`/api/auth/admin/users/${userId}/status`, { status });

export const updateBusinessUser = (businessId: number, userId: number, data: UpdateUserRequest) =>
  api.put<BusinessUser>(`/api/auth/businesses/${businessId}/users/${userId}`, data);

export const updateBusinessUserRole = (businessId: number, userId: number, data: UpdateRoleRequest) =>
  api.put<BusinessUser>(`/api/auth/businesses/${businessId}/users/${userId}/role`, data);
