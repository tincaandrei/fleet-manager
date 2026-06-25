import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  inviteUser,
  listBusinessUsers,
  resendUserInvite,
  updateAdminUserStatus,
  updateBusinessUserRole,
} from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import type { BusinessUser, InviteUserRequest, UserStatus } from '../types/auth';
import PageShell from '../components/ui/PageShell';
import DataState from '../components/ui/DataState';
import ResponsiveTable from '../components/ui/ResponsiveTable';
import { Button } from '../components/ui/Button';

function apiMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

const ROLE_OPTIONS: Array<{ value: 'BUSINESS_ADMIN' | 'EMPLOYEE'; label: string }> = [
  { value: 'BUSINESS_ADMIN', label: 'Organization Admin' },
  { value: 'EMPLOYEE', label: 'Employee' },
];

interface AddUserFormProps {
  businessId: number;
  includeBusinessId: boolean;
  onInvited: () => void;
}

function AddUserForm({ businessId, includeBusinessId, onInvited }: AddUserFormProps) {
  const [form, setForm] = useState<InviteUserRequest>({
    businessId: includeBusinessId ? businessId : null,
    email: '',
    firstName: '',
    lastName: '',
    roles: ['EMPLOYEE'],
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleChange = (field: 'email' | 'firstName' | 'lastName', value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setSuccess(false);
  };

  const handleRoleChange = (role: 'BUSINESS_ADMIN' | 'EMPLOYEE') => {
    setForm((prev) => ({ ...prev, roles: [role] }));
    setSuccess(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);
    setLoading(true);
    try {
      await inviteUser({
        ...form,
        businessId: includeBusinessId ? businessId : null,
        email: form.email.trim(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
      });
      setForm({
        businessId: includeBusinessId ? businessId : null,
        email: '',
        firstName: '',
        lastName: '',
        roles: ['EMPLOYEE'],
      });
      setSuccess(true);
      onInvited();
    } catch (err: unknown) {
      setError(apiMessage(err, 'Failed to send invitation.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="business-add-user-form">
      <h3>Invite User</h3>
      {error && <p className="error">{error}</p>}
      {success && <p className="success-note">Invitation sent successfully.</p>}

      <label>
        First name
        <input
          value={form.firstName}
          onChange={(e) => handleChange('firstName', e.target.value)}
          required
          autoComplete="given-name"
        />
      </label>

      <label>
        Last name
        <input
          value={form.lastName}
          onChange={(e) => handleChange('lastName', e.target.value)}
          required
          autoComplete="family-name"
        />
      </label>

      <label>
        Email
        <input
          type="email"
          value={form.email}
          onChange={(e) => handleChange('email', e.target.value)}
          required
          autoComplete="email"
        />
      </label>

      <label>
        Role
        <select
          value={form.roles[0]}
          onChange={(e) => handleRoleChange(e.target.value as 'BUSINESS_ADMIN' | 'EMPLOYEE')}
        >
          {ROLE_OPTIONS.map((r) => (
            <option key={r.value} value={r.value}>
              {r.label}
            </option>
          ))}
        </select>
      </label>

      <button type="submit" disabled={loading}>
        {loading ? 'Sending...' : 'Send invitation'}
      </button>
    </form>
  );
}

interface RoleCellProps {
  user: BusinessUser;
  businessId: number;
  canChangeRole: boolean;
  onUpdated: (updated: BusinessUser) => void;
}

function RoleCell({ user, businessId, canChangeRole, onUpdated }: RoleCellProps) {
  const [saving, setSaving] = useState(false);

  if (!canChangeRole) {
    const label = ROLE_OPTIONS.find((r) => r.value === user.role)?.label ?? user.role;
    return <span>{label}</span>;
  }

  const handleChange = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newRole = e.target.value as 'BUSINESS_ADMIN' | 'EMPLOYEE';
    setSaving(true);
    try {
      const res = await updateBusinessUserRole(businessId, user.userId, { role: newRole });
      onUpdated(res.data);
    } catch {
      // Toast shown by interceptor.
    } finally {
      setSaving(false);
    }
  };

  return (
    <select value={user.role} onChange={handleChange} disabled={saving}>
      {ROLE_OPTIONS.map((r) => (
        <option key={r.value} value={r.value}>
          {r.label}
        </option>
      ))}
    </select>
  );
}

interface UserActionsProps {
  user: BusinessUser;
  onChanged: () => void;
}

function UserActions({ user, onChanged }: UserActionsProps) {
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const status = user.status;

  const run = async (action: string, task: () => Promise<unknown>) => {
    setBusyAction(action);
    try {
      await task();
      onChanged();
    } catch {
      // Toast shown by interceptor.
    } finally {
      setBusyAction(null);
    }
  };

  return (
    <div className="row-actions">
      {status === 'INVITED' && (
        <Button
          size="sm"
          variant="secondary"
          disabled={busyAction !== null}
          onClick={() => run('resend', () => resendUserInvite(user.userId))}
        >
          {busyAction === 'resend' ? 'Sending...' : 'Resend invite'}
        </Button>
      )}
      {status !== 'DISABLED' && (
        <Button
          size="sm"
          variant="danger"
          disabled={busyAction !== null}
          onClick={() => run('disable', () => updateAdminUserStatus(user.userId, 'DISABLED'))}
        >
          {busyAction === 'disable' ? 'Saving...' : 'Disable'}
        </Button>
      )}
      {status === 'DISABLED' && (
        <Button
          size="sm"
          variant="success"
          disabled={busyAction !== null}
          onClick={() => run('enable', () => updateAdminUserStatus(user.userId, 'ACTIVE'))}
        >
          {busyAction === 'enable' ? 'Saving...' : 'Enable'}
        </Button>
      )}
    </div>
  );
}

function userStatus(user: BusinessUser): UserStatus {
  return user.status ?? 'ACTIVE';
}

export default function BusinessUsersPage() {
  const { id } = useParams<{ id: string }>();
  const businessId = Number(id);
  const { isSuperAdmin, isBusinessAdmin, businessId: myBusinessId } = useAuth();

  const [users, setUsers] = useState<BusinessUser[]>([]);
  const [activeTab, setActiveTab] = useState<'view' | 'add'>('view');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const canManage = isSuperAdmin || (isBusinessAdmin && myBusinessId === businessId);

  const load = () => {
    if (!canManage) return;
    setLoading(true);
    setError(null);
    listBusinessUsers(businessId)
      .then((res) => setUsers(res.data))
      .catch((err: unknown) => setError(apiMessage(err, 'Failed to load users.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timeoutId = window.setTimeout(() => load(), 0);
    return () => window.clearTimeout(timeoutId);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [businessId, canManage]);

  const handleUpdated = (updated: BusinessUser) => {
    setUsers((prev) => prev.map((u) => (u.userId === updated.userId ? updated : u)));
  };

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
        {isSuperAdmin && <Link to="/businesses">Back to Organizations</Link>}
      </div>

      <p className="info-note">Organization ID: {businessId}</p>

      <nav className="mini-tabs" aria-label="Users sections">
        <Button
          variant={activeTab === 'view' ? 'primary' : 'secondary'}
          onClick={() => setActiveTab('view')}
        >
          View users
        </Button>
        <Button
          variant={activeTab === 'add' ? 'primary' : 'secondary'}
          onClick={() => setActiveTab('add')}
        >
          Invite user
        </Button>
      </nav>

      {loading && <DataState type="loading">Loading users...</DataState>}
      {!loading && error && <DataState type="error">{error}</DataState>}

      {!loading && !error && (
        <>
          {activeTab === 'view' && (
            users.length === 0 ? (
              <DataState>No users in this organization yet.</DataState>
            ) : (
              <ResponsiveTable ariaLabel="Organization users">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => {
                    const status = userStatus(u);
                    return (
                      <tr key={u.userId}>
                        <td data-label="ID">{u.userId}</td>
                        <td data-label="Username">{u.username || '-'}</td>
                        <td data-label="Email">{u.email}</td>
                        <td data-label="Phone">{u.phone || '-'}</td>
                        <td data-label="Role">
                          <RoleCell
                            user={u}
                            businessId={businessId}
                            canChangeRole={canManage && status !== 'INVITED'}
                            onUpdated={handleUpdated}
                          />
                        </td>
                        <td data-label="Status">
                          <span className={`status-badge status-${status}`}>
                            {status}
                          </span>
                        </td>
                        <td data-label="Actions">
                          <UserActions user={{ ...u, status }} onChanged={load} />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </ResponsiveTable>
            )
          )}

          {activeTab === 'add' && (
            <AddUserForm
              businessId={businessId}
              includeBusinessId={isSuperAdmin}
              onInvited={load}
            />
          )}
        </>
      )}
    </PageShell>
  );
}
