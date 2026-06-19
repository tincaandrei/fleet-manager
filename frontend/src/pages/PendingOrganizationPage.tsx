import { Link, Navigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { useAuth } from '../auth/AuthContext';
import { homeForRole } from '../auth/roleHome';

export default function PendingOrganizationPage() {
  const { role, businessId, username, logout } = useAuth();

  if (role === 'SUPERADMIN' || businessId != null) {
    return <Navigate to={homeForRole(role, businessId)} replace />;
  }

  return (
    <>
      <Navbar />
      <main className="page">
        <section className="pending-panel">
          <div>
            <h1>Organization Assignment Pending</h1>
            <p>
              Your account{username ? ` (${username})` : ''} was created, but it is not assigned to an organization yet.
              A super admin must assign your organization and role before you can use fleet features.
            </p>
          </div>

          <div className="pending-actions">
            <Link to="/profile" className="btn btn-secondary">View Profile</Link>
            <button type="button" className="btn" onClick={logout}>
              Sign Out
            </button>
          </div>

          <p className="info-note">
            After an admin assigns your account, sign out and sign in again to refresh your access.
          </p>
        </section>
      </main>
    </>
  );
}
