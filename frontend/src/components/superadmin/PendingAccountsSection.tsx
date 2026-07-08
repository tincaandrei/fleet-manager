import { useState } from 'react';
import { assignUnassignedUser } from '../../api/authApi';
import type { Business, UserProfile } from '../../types/auth';
import DataState from '../ui/DataState';
import ResponsiveTable from '../ui/ResponsiveTable';
import Pagination from '../ui/Pagination';
import { usePagedList } from '../ui/usePagedList';
import { getApiErrorMessage } from '../../utils/apiError';

interface PendingAccountsSectionProps {
  businesses: Business[];
  users: UserProfile[];
  onAssigned: (userId: number) => void;
}

export default function PendingAccountsSection({ businesses, users, onAssigned }: PendingAccountsSectionProps) {
  const activeBusinesses = businesses.filter((business) => business.active);
  const { page, pageCount, pageItems, setPage } = usePagedList(users);

  return (
    <section className="management-section">
      <div className="section-header">
        <h2>Pending Accounts</h2>
        <span>{users.length} waiting</span>
      </div>

      {users.length === 0 && (
        <DataState>No accounts are waiting for organization assignment.</DataState>
      )}

      {users.length > 0 && activeBusinesses.length === 0 && (
        <DataState type="error">Create or activate an organization before assigning accounts.</DataState>
      )}

      {users.length > 0 && activeBusinesses.length > 0 && (
        <>
          <ResponsiveTable ariaLabel="Pending accounts">
            <thead>
              <tr>
                <th>Username</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Assign Organization</th>
              </tr>
            </thead>
            <tbody>
              {pageItems.map((user) => (
                <PendingAccountRow
                  key={user.userId}
                  user={user}
                  businesses={activeBusinesses}
                  onAssigned={onAssigned}
                />
              ))}
            </tbody>
          </ResponsiveTable>
          <Pagination
            page={page}
            pageCount={pageCount}
            onPageChange={setPage}
            summary={`${users.length} pending accounts`}
          />
        </>
      )}
    </section>
  );
}

interface PendingAccountRowProps {
  user: UserProfile;
  businesses: Business[];
  onAssigned: (userId: number) => void;
}

function PendingAccountRow({ user, businesses, onAssigned }: PendingAccountRowProps) {
  const [businessId, setBusinessId] = useState(businesses[0]?.id ?? 0);
  const [role, setRole] = useState<'BUSINESS_ADMIN' | 'EMPLOYEE'>('EMPLOYEE');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAssign = async () => {
    if (!businessId) return;
    setSaving(true);
    setError(null);
    try {
      await assignUnassignedUser(user.userId, { businessId, role });
      onAssigned(user.userId);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to assign account.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <tr>
      <td data-label="Username">{user.username || '-'}</td>
      <td data-label="Email">{user.email}</td>
      <td data-label="Phone">{user.phone || '-'}</td>
      <td data-label="Assign Organization">
        <div className="inline-assignment">
          <select
            value={businessId}
            onChange={(event) => setBusinessId(Number(event.target.value))}
            disabled={saving}
            aria-label={`Organization for ${user.username || user.email}`}
          >
            {businesses.map((business) => (
              <option key={business.id} value={business.id}>{business.name}</option>
            ))}
          </select>
          <select
            value={role}
            onChange={(event) => setRole(event.target.value as 'BUSINESS_ADMIN' | 'EMPLOYEE')}
            disabled={saving}
            aria-label={`Role for ${user.username || user.email}`}
          >
            <option value="EMPLOYEE">Employee</option>
            <option value="BUSINESS_ADMIN">Organization Admin</option>
          </select>
          <button type="button" className="btn btn-sm" onClick={handleAssign} disabled={saving}>
            {saving ? 'Assigning...' : 'Assign'}
          </button>
          {error && <span className="inline-error">{error}</span>}
        </div>
      </td>
    </tr>
  );
}
