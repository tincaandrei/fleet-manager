import { createContext, useContext, useState } from 'react';
import type { ReactNode } from 'react';
import type { Role } from '../types/auth';
import { normalizeRole } from './roleHome';

interface AuthContextType {
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

const AuthContext = createContext<AuthContextType | null>(null);

function storedNumber(key: string): number | null {
  const v = localStorage.getItem(key);
  if (v === null || v === 'null') return null;
  const n = Number(v);
  return isNaN(n) ? null : n;
}

function storedString(key: string): string | null {
  const v = localStorage.getItem(key);
  return v === 'null' || v === null ? null : v;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'));
  const [username, setUsername] = useState<string | null>(() => storedString('username'));
  const [role, setRole] = useState<Role | null>(() => normalizeRole(storedString('role')));
  const [userId, setUserId] = useState<number | null>(() => storedNumber('userId'));
  const [businessId, setBusinessId] = useState<number | null>(() => storedNumber('businessId'));
  const [businessName, setBusinessName] = useState<string | null>(() => storedString('businessName'));

  const login = (
    newToken: string,
    newUsername: string,
    newRole: Role,
    newUserId: number,
    newBusinessId: number | null,
    newBusinessName: string | null,
  ) => {
    const normalizedRole = normalizeRole(newRole);
    if (!normalizedRole) {
      throw new Error(`Unsupported role: ${newRole}`);
    }
    localStorage.setItem('token', newToken);
    localStorage.setItem('username', newUsername);
    localStorage.setItem('role', normalizedRole);
    localStorage.setItem('userId', String(newUserId));
    localStorage.setItem('businessId', String(newBusinessId));
    localStorage.setItem('businessName', String(newBusinessName));
    setToken(newToken);
    setUsername(newUsername);
    setRole(normalizedRole);
    setUserId(newUserId);
    setBusinessId(newBusinessId);
    setBusinessName(newBusinessName);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
    localStorage.removeItem('businessId');
    localStorage.removeItem('businessName');
    setToken(null);
    setUsername(null);
    setRole(null);
    setUserId(null);
    setBusinessId(null);
    setBusinessName(null);
  };

  const isSuperAdmin = role === 'SUPERADMIN';
  const isBusinessAdmin = role === 'BUSINESS_ADMIN';
  const isEmployee = role === 'EMPLOYEE';
  const isAdmin = isSuperAdmin || isBusinessAdmin;

  return (
    <AuthContext.Provider
      value={{
        token,
        username,
        role,
        userId,
        businessId,
        businessName,
        isAdmin,
        isSuperAdmin,
        isBusinessAdmin,
        isEmployee,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
