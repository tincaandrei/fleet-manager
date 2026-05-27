import type { Role } from '../types/auth';

const ROLES: Role[] = ['SUPERADMIN', 'BUSINESS_ADMIN', 'EMPLOYEE'];

export function normalizeRole(value: unknown): Role | null {
  if (typeof value !== 'string') {
    return null;
  }
  const normalized = value.trim().toUpperCase() as Role;
  return ROLES.includes(normalized) ? normalized : null;
}

export function homeForRole(role: Role | null): string {
  return role === 'SUPERADMIN' ? '/businesses' : '/vehicles';
}
