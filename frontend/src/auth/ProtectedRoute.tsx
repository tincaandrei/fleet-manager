import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './useAuth';
import type { Role } from '../types/auth';
import { homeForRole } from './roleHome';

interface Props {
  children: ReactNode;
  /** If provided, user must have one of these roles. */
  requiredRoles?: Role[];
}

export default function ProtectedRoute({ children, requiredRoles }: Props) {
  const { token, role, businessId } = useAuth();
  const location = useLocation();

  if (!token) return <Navigate to="/login" replace />;

  const isAllowedPendingPath = location.pathname === '/pending-organization' || location.pathname === '/profile';
  if (role !== 'SUPERADMIN' && businessId == null && !isAllowedPendingPath) {
    return <Navigate to="/pending-organization" replace />;
  }

  if (requiredRoles && (!role || !requiredRoles.includes(role))) {
    return <Navigate to={homeForRole(role, businessId)} replace />;
  }

  return <>{children}</>;
}