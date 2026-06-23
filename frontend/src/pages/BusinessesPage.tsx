import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { assignUnassignedUser, listBusinesses, listUnassignedUsers } from '../api/authApi';
import type { Business, UserProfile } from '../types/auth';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import { ButtonLink } from '../components/ui/Button';
import DataState from '../components/ui/DataState';
import ResponsiveTable from '../components/ui/ResponsiveTable';

function apiMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

export default function BusinessesPage() {
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [unassignedUsers, setUnassignedUsers] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    Promise.all([listBusinesses(), listUnassignedUsers()])
      .then(([businessRes, unassignedRes]) => {
        setBusinesses(businessRes.data);
        setUnassignedUsers(unassignedRes.data);
      })
      .catch((err: unknown) => setError(apiMessage(err, 'Failed to load organizations.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timeoutId = window.setTimeout(() => load(), 0);
    return () => window.clearTimeout(timeoutId);
  }, []);

  const handleAssigned = (userId: number) => {
    setUnassignedUsers((current) => current.filter((user) => user.userId !== userId));
  };

  return (
    <PageShell>
        <PageHeader
          title="Organizations"
          description="Manage organizations and pending account assignments."
          actions={<ButtonLink to="/businesses/new">New Organization</ButtonLink>}
        />

        {loading && <DataState type="loading">Loading organizations...</DataState>}
        {!loading && error && <DataState type="error">{error}</DataState>}
        {!loading && !error && businesses.length === 0 && (
          <DataState>No organizations yet. Create the first one.</DataState>
        )}

        {!loading && !error && businesses.length > 0 && (
          <>
            <PendingAccountsSection
              businesses={businesses}
              users={unassignedUsers}
              onAssigned={handleAssigned}
            />

            <ResponsiveTable ariaLabel="Organizations">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Registration</th>
                  <th>Contact</th>
                  <th>Phone</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {businesses.map((business) => (
                  <tr key={business.id}>
                    <td data-label="ID">{business.id}</td>
                    <td data-label="Name">{business.name}</td>
                    <td data-label="Registration">{business.registrationNumber || '-'}</td>
                    <td data-label="Contact">{business.contactEmail || '-'}</td>
                    <td data-label="Phone">{business.phone || '-'}</td>
                    <td data-label="Status">{business.active ? 'Active' : 'Inactive'}</td>
                    <td data-label="Actions" className="actions-cell">
                      <Link to={`/businesses/${business.id}/users`}>Users</Link>
                      {' | '}
                      <Link to={`/businesses/${business.id}/edit`}>Edit</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </ResponsiveTable>
          </>
        )}
    </PageShell>
  );
}

interface PendingAccountsSectionProps {
  businesses: Business[];
  users: UserProfile[];
  onAssigned: (userId: number) => void;
}

function PendingAccountsSection({ businesses, users, onAssigned }: PendingAccountsSectionProps) {
  const activeBusinesses = businesses.filter((business) => business.active);

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
            {users.map((user) => (
              <PendingAccountRow
                key={user.userId}
                user={user}
                businesses={activeBusinesses}
                onAssigned={onAssigned}
              />
            ))}
          </tbody>
        </ResponsiveTable>
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
      setError(apiMessage(err, 'Failed to assign account.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <tr>
      <td data-label="Username">{user.username}</td>
      <td data-label="Email">{user.email}</td>
      <td data-label="Phone">{user.phone || '-'}</td>
      <td data-label="Assign Organization">
        <div className="inline-assignment">
          <select
            value={businessId}
            onChange={(event) => setBusinessId(Number(event.target.value))}
            disabled={saving}
            aria-label={`Organization for ${user.username}`}
          >
            {businesses.map((business) => (
              <option key={business.id} value={business.id}>{business.name}</option>
            ))}
          </select>
          <select
            value={role}
            onChange={(event) => setRole(event.target.value as 'BUSINESS_ADMIN' | 'EMPLOYEE')}
            disabled={saving}
            aria-label={`Role for ${user.username}`}
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
