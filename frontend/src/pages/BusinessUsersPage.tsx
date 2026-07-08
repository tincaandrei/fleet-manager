import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import PageShell from '../components/ui/PageShell';
import DataState from '../components/ui/DataState';
import OrgUsersPanel from '../components/superadmin/OrgUsersPanel';

export default function BusinessUsersPage() {
  const { id } = useParams<{ id: string }>();
  const businessId = Number(id);
  const { isSuperAdmin, isBusinessAdmin, businessId: myBusinessId } = useAuth();

  const canManage = isSuperAdmin || (isBusinessAdmin && myBusinessId === businessId);

  if (!canManage) {
    return (
      <PageShell>
        <DataState type="error">Access denied. You can only manage users in your own organization.</DataState>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <div className="page-header">
        <h1>Organization Users</h1>
        {isSuperAdmin && <Link to={`/superadmin?org=${businessId}&tab=users`}>Open in Console</Link>}
      </div>

      <p className="info-note">Organization ID: {businessId}</p>

      <OrgUsersPanel businessId={businessId} />
    </PageShell>
  );
}
