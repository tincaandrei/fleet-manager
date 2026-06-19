// ── Roles ─────────────────────────────────────────────────────────────────────

export type Role = 'SUPERADMIN' | 'BUSINESS_ADMIN' | 'EMPLOYEE';

// ── Login / Register ──────────────────────────────────────────────────────────

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
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
  username: string;
  email: string;
  phone: string | null;
  address: string | null;
  role: Role;
  businessId: number | null;
  businessName: string | null;
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
  username: string;
  email: string;
  phone: string | null;
  address: string | null;
  role: 'BUSINESS_ADMIN' | 'EMPLOYEE';
  businessId: number;
  businessName: string | null;
}

export interface CreateBusinessUserRequest {
  username: string;
  email: string;
  password: string;
  role: 'BUSINESS_ADMIN' | 'EMPLOYEE';
  phone?: string | null;
  address?: string | null;
}

export interface UpdateRoleRequest {
  role: 'BUSINESS_ADMIN' | 'EMPLOYEE';
}

export interface AssignBusinessUserRequest {
  businessId: number;
  role: 'BUSINESS_ADMIN' | 'EMPLOYEE';
}

export interface UpdateUserRequest {
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  password?: string | null;
}
