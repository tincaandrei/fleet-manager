import { Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';
import type { Role } from '../types/auth';
import { homeForRole } from './roleHome';

interface Props {
  children: ReactNode;
  /** If provided, user must have one of these roles. */
  requiredRoles?: Role[];
}

export default function ProtectedRoute({ children, requiredRoles }: Props) {
  const { token, role } = useAuth();

  if (!token) return <Navigate to="/login" replace />;

  if (requiredRoles && (!role || !requiredRoles.includes(role))) {
    return <Navigate to={homeForRole(role)} replace />;
  }

  return <>{children}</>;
}
