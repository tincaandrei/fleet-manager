import { createContext } from 'react';
import type { Role } from '../types/auth';

export interface AuthContextType {
  token: string | null;
  username: string | null;
  role: Role | null;
  userId: number | null;
  businessId: number | null;
  businessName: string | null;
  /** True for SUPERADMIN and BUSINESS_ADMIN (can manage fleet). */
  isAdmin: boolean;
  isSuperAdmin: boolean;
  isBusinessAdmin: boolean;
  isEmployee: boolean;
  login: (
    token: string,
    username: string,
    role: Role,
    userId: number,
    businessId: number | null,
    businessName: string | null,
  ) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);
