import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AcceptInvitePage from './pages/AcceptInvitePage';
import ProfilePage from './pages/ProfilePage';
import VehiclesPage from './pages/VehiclesPage';
import VehicleDetailsPage from './pages/VehicleDetailsPage';
import VehicleCreatePage from './pages/VehicleCreatePage';
import VehicleEditPage from './pages/VehicleEditPage';
import DocumentAlertsPage from './pages/DocumentAlertsPage';
import DocumentHistoryPage from './pages/DocumentHistoryPage';
import BusinessesPage from './pages/BusinessesPage';
import BusinessCreateEditPage from './pages/BusinessCreateEditPage';
import BusinessUsersPage from './pages/BusinessUsersPage';
import SuperAdminConsolePage from './pages/SuperAdminConsolePage';
import PendingOrganizationPage from './pages/PendingOrganizationPage';
import ToastProvider from './components/ToastProvider';
import { useAuth } from './auth/useAuth';
import { homeForRole } from './auth/roleHome';

function RoleHomeRedirect() {
  const { token, role, businessId } = useAuth();
  return <Navigate to={token ? homeForRole(role, businessId) : '/login'} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <Routes>
            {/* Public */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/accept-invite" element={<AcceptInvitePage />} />
            <Route path="/" element={<RoleHomeRedirect />} />

            {/* All authenticated users */}
            <Route path="/pending-organization" element={<ProtectedRoute><PendingOrganizationPage /></ProtectedRoute>} />
            <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
            <Route path="/vehicles" element={<ProtectedRoute><VehiclesPage /></ProtectedRoute>} />
            <Route path="/vehicles/:id" element={<ProtectedRoute><VehicleDetailsPage /></ProtectedRoute>} />
            <Route path="/documents/history" element={<ProtectedRoute><DocumentHistoryPage /></ProtectedRoute>} />

            {/* SUPERADMIN + BUSINESS_ADMIN only */}
            <Route
              path="/vehicles/new"
              element={
                <ProtectedRoute requiredRoles={['SUPERADMIN', 'BUSINESS_ADMIN']}>
                  <VehicleCreatePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/vehicles/:id/edit"
              element={
                <ProtectedRoute requiredRoles={['SUPERADMIN', 'BUSINESS_ADMIN']}>
                  <VehicleEditPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/alerts/documents"
              element={
                <ProtectedRoute requiredRoles={['SUPERADMIN', 'BUSINESS_ADMIN']}>
                  <DocumentAlertsPage />
                </ProtectedRoute>
              }
            />

            {/* SUPERADMIN only */}
            <Route
              path="/superadmin"
              element={
                <ProtectedRoute requiredRoles={['SUPERADMIN']}>
                  <SuperAdminConsolePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/businesses"
              element={
                <ProtectedRoute requiredRoles={['SUPERADMIN']}>
                  <BusinessesPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/businesses/new"
              element={
                <ProtectedRoute requiredRoles={['SUPERADMIN']}>
                  <BusinessCreateEditPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/businesses/:id/edit"
              element={
                <ProtectedRoute requiredRoles={['SUPERADMIN']}>
                  <BusinessCreateEditPage />
                </ProtectedRoute>
              }
            />

            {/* SUPERADMIN + BUSINESS_ADMIN (backend enforces own-business for BUSINESS_ADMIN) */}
            <Route
              path="/businesses/:id/users"
              element={
                <ProtectedRoute requiredRoles={['SUPERADMIN', 'BUSINESS_ADMIN']}>
                  <BusinessUsersPage />
                </ProtectedRoute>
              }
            />

            {/* Catch-all */}
            <Route path="*" element={<RoleHomeRedirect />} />
          </Routes>
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
