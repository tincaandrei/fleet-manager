// ── Roles ─────────────────────────────────────────────────────────────────────

export type Role = 'SUPERADMIN' | 'BUSINESS_ADMIN' | 'EMPLOYEE';
export type UserStatus = 'INVITED' | 'ACTIVE' | 'DISABLED';

// ── Login / Register ──────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string | null;
  email: string;
  role: Role;
  userId: number;
  businessId: number | null;
  businessName: string | null;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  phone: string;
  address: string;
}

// ── User profile ──────────────────────────────────────────────────────────────

export interface UserProfile {
  userId: number;
  username: string | null;
  email: string;
  phone: string | null;
  address: string | null;
  role: Role;
  status?: UserStatus;
  businessId: number | null;
  businessName: string | null;
  profileImageUrl: string | null;
  profileImageOriginalFileName: string | null;
}

// ── Business ──────────────────────────────────────────────────────────────────

export interface Business {
  id: number;
  name: string;
  registrationNumber: string | null;
  contactEmail: string | null;
  phone: string | null;
  address: string | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface BusinessRequest {
  name: string;
  registrationNumber?: string | null;
  contactEmail?: string | null;
  phone?: string | null;
  address?: string | null;
  active?: boolean;
}

// ── Business users ────────────────────────────────────────────────────────────

export interface BusinessUser {
  userId: number;
  username: string | null;
  email: string;
  phone: string | null;
  address: string | null;
  role: 'BUSINESS_ADMIN' | 'EMPLOYEE';
  status?: UserStatus;
  businessId: number;
  businessName: string | null;
  profileImageUrl?: string | null;
  profileImageOriginalFileName?: string | null;
}

export interface InviteUserRequest {
  businessId?: number | null;
  email: string;
  firstName: string;
  lastName: string;
  roles: Array<'BUSINESS_ADMIN' | 'EMPLOYEE'>;
}

export interface AdminUserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: Array<'BUSINESS_ADMIN' | 'EMPLOYEE' | 'SUPERADMIN'>;
  status: UserStatus;
  enabled: boolean;
  passwordChangeRequired: boolean;
  businessId: number | null;
}

export interface AcceptInviteRequest {
  token: string;
  newPassword: string;
}

export interface InviteValidationResponse {
  valid: boolean;
  email: string | null;
  message: string;
}

export interface UpdateRoleRequest {
  role: 'BUSINESS_ADMIN' | 'EMPLOYEE';
}

export interface AssignBusinessUserRequest {
  businessId: number;
  role: 'BUSINESS_ADMIN' | 'EMPLOYEE';
}

export interface UpdateUserRequest {
  username?: string | null;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  password?: string | null;
}
