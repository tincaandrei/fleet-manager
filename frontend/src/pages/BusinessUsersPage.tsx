import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { listBusinessUsers, createBusinessUser, updateBusinessUserRole } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import type { BusinessUser, CreateBusinessUserRequest } from '../types/auth';
import PageShell from '../components/ui/PageShell';
import DataState from '../components/ui/DataState';
import ResponsiveTable from '../components/ui/ResponsiveTable';

function apiMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

const ROLE_OPTIONS: Array<{ value: 'BUSINESS_ADMIN' | 'EMPLOYEE'; label: string }> = [
  { value: 'BUSINESS_ADMIN', label: 'Organization Admin' },
  { value: 'EMPLOYEE',       label: 'Employee' },
];

// ── Add-user form ─────────────────────────────────────────────────────────────

interface AddUserFormProps {
  businessId: number;
  onCreated: (user: BusinessUser) => void;
}

function AddUserForm({ businessId, onCreated }: AddUserFormProps) {
  const [form, setForm] = useState<CreateBusinessUserRequest>({
    username: '',
    email: '',
    password: '',
    role: 'EMPLOYEE',
    phone: '',
    address: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleChange = (field: keyof CreateBusinessUserRequest, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setSuccess(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);
    setLoading(true);
    try {
      const res = await createBusinessUser(businessId, {
        ...form,
        phone: form.phone?.trim() || null,
        address: form.address?.trim() || null,
      });
      onCreated(res.data);
      setForm({ username: '', email: '', password: '', role: 'EMPLOYEE', phone: '', address: '' });
      setSuccess(true);
    } catch (err: unknown) {
      setError(apiMessage(err, 'Failed to create user.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="business-add-user-form">
      <h3>Add User</h3>
      {error && <p className="error">{error}</p>}
      {success && <p className="success-note">User created successfully.</p>}

      <label>
        Username
        <input
          value={form.username}
          onChange={(e) => handleChange('username', e.target.value)}
          required
          autoComplete="off"
        />
      </label>
      <label>
        Email
        <input
          type="email"
          value={form.email}
          onChange={(e) => handleChange('email', e.target.value)}
          required
          autoComplete="off"
        />
      </label>
      <label>
        Password
        <input
          type="password"
          value={form.password}
          onChange={(e) => handleChange('password', e.target.value)}
          required
          autoComplete="new-password"
        />
      </label>
      <label>
        Phone
        <input
          value={form.phone ?? ''}
          onChange={(e) => handleChange('phone', e.target.value)}
          autoComplete="off"
        />
      </label>
      <label>
        Address
        <input
          value={form.address ?? ''}
          onChange={(e) => handleChange('address', e.target.value)}
          autoComplete="off"
        />
      </label>
      <label>
        Role
        <select
          value={form.role}
          onChange={(e) => handleChange('role', e.target.value)}
        >
          {ROLE_OPTIONS.map((r) => (
            <option key={r.value} value={r.value}>{r.label}</option>
          ))}
        </select>
      </label>

      <button type="submit" disabled={loading}>
        {loading ? 'Creating…' : 'Create User'}
      </button>
    </form>
  );
}

// ── Role-change inline dropdown ────────────────────────────────────────────────

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
      // Toast shown by interceptor
    } finally {
      setSaving(false);
    }
  };

  return (
    <select value={user.role} onChange={handleChange} disabled={saving}>
      {ROLE_OPTIONS.map((r) => (
        <option key={r.value} value={r.value}>{r.label}</option>
      ))}
    </select>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function BusinessUsersPage() {
  const { id } = useParams<{ id: string }>();
  const businessId = Number(id);
  const { isSuperAdmin, isBusinessAdmin, businessId: myBusinessId } = useAuth();

  const [users, setUsers] = useState<BusinessUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // BUSINESS_ADMIN can only see their own organization users
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
  }, [businessId]);

  const handleCreated = (user: BusinessUser) => {
    setUsers((prev) => [...prev, user]);
  };

  const handleUpdated = (updated: BusinessUser) => {
    setUsers((prev) => prev.map((u) => u.userId === updated.userId ? updated : u));
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
          {isSuperAdmin && (
            <Link to="/businesses">Back to Organizations</Link>
          )}
        </div>

        <p className="info-note">Organization ID: {businessId}</p>

        {loading && <DataState type="loading">Loading users...</DataState>}
        {!loading && error && <DataState type="error">{error}</DataState>}

        {!loading && !error && (
          <>
            {users.length === 0 ? (
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
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.userId}>
                      <td data-label="ID">{u.userId}</td>
                      <td data-label="Username">{u.username}</td>
                      <td data-label="Email">{u.email}</td>
                      <td data-label="Phone">{u.phone || '-'}</td>
                      <td data-label="Role">
                        <RoleCell
                          user={u}
                          businessId={businessId}
                          canChangeRole={canManage}
                          onUpdated={handleUpdated}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </ResponsiveTable>
            )}

            <AddUserForm businessId={businessId} onCreated={handleCreated} />
          </>
        )}
    </PageShell>
  );
}
