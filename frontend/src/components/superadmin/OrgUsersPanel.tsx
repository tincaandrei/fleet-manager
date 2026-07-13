import { useEffect, useState } from 'react';
import {
  inviteUser,
  listBusinessUsers,
  resendUserInvite,
  sendPasswordResetLink,
  updateAdminUserStatus,
  updateBusinessUserRole,
} from '../../api/authApi';
import { useAuth } from '../../auth/useAuth';
import type { BusinessUser, InviteUserRequest, UserStatus } from '../../types/auth';
import DataState from '../ui/DataState';
import ResponsiveTable from '../ui/ResponsiveTable';
import { Button } from '../ui/Button';
import Pagination from '../ui/Pagination';
import { usePagedList } from '../ui/usePagedList';
import RowActionMenu from '../ui/RowActionMenu';
import type { RowAction } from '../ui/RowActionMenu';
import { getApiErrorMessage } from '../../utils/apiError';
import { showToast } from '../../utils/toast';

const ROLE_OPTIONS: Array<{ value: 'BUSINESS_ADMIN' | 'EMPLOYEE'; label: string }> = [
  { value: 'BUSINESS_ADMIN', label: 'Organization Admin' },
  { value: 'EMPLOYEE', label: 'Employee' },
];

interface InviteUserFormProps {
  businessId: number;
  includeBusinessId: boolean;
  onInvited: () => void;
  onClose: () => void;
}

function InviteUserForm({ businessId, includeBusinessId, onInvited, onClose }: InviteUserFormProps) {
  const [form, setForm] = useState<InviteUserRequest>({
    businessId: includeBusinessId ? businessId : null,
    email: '',
    firstName: '',
    lastName: '',
    roles: ['EMPLOYEE'],
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (field: 'email' | 'firstName' | 'lastName', value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await inviteUser({
        ...form,
        businessId: includeBusinessId ? businessId : null,
        email: form.email.trim(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
      });
      showToast({ type: 'success', message: `Invitation sent to ${form.email.trim()}.` });
      onInvited();
      onClose();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to send invitation.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="panel-card invite-form">
      <div className="panel-card-header">
        <h3>Invite user</h3>
        <p>They receive an email link to set their password.</p>
      </div>
      {error && <p className="error">{error}</p>}

      <div className="invite-form-grid">
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
            onChange={(e) => setForm((prev) => ({ ...prev, roles: [e.target.value as 'BUSINESS_ADMIN' | 'EMPLOYEE'] }))}
          >
            {ROLE_OPTIONS.map((r) => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="form-actions-row">
        <Button type="submit" disabled={loading}>
          {loading ? 'Sending...' : 'Send invitation'}
        </Button>
        <Button variant="ghost" onClick={onClose} disabled={loading}>
          Cancel
        </Button>
      </div>
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
      showToast({ type: 'success', message: 'Role updated.' });
    } catch {
      // Toast shown by interceptor.
    } finally {
      setSaving(false);
    }
  };

  return (
    <select value={user.role} onChange={handleChange} disabled={saving} aria-label={`Role for ${user.email}`}>
      {ROLE_OPTIONS.map((r) => (
        <option key={r.value} value={r.value}>
          {r.label}
        </option>
      ))}
    </select>
  );
}

function userStatus(user: BusinessUser): UserStatus {
  return user.status ?? 'ACTIVE';
}

interface UserRowActionsProps {
  user: BusinessUser;
  onChanged: () => void;
}

function UserRowActions({ user, onChanged }: UserRowActionsProps) {
  const [busy, setBusy] = useState(false);
  const status = userStatus(user);

  const run = (task: () => Promise<unknown>, successMessage?: string) => {
    setBusy(true);
    task()
      .then(() => {
        if (successMessage) {
          showToast({ type: 'success', message: successMessage });
        }
        onChanged();
      })
      .catch(() => {
        // Toast shown by interceptor.
      })
      .finally(() => setBusy(false));
  };

  const actions: RowAction[] = [];
  if (status === 'INVITED') {
    actions.push({
      label: 'Resend invite',
      onSelect: () => run(() => resendUserInvite(user.userId), 'Invitation email sent.'),
    });
  }
  if (status === 'ACTIVE' || status === 'DISABLED') {
    actions.push({
      label: 'Send reset link',
      onSelect: () => run(() => sendPasswordResetLink(user.userId), 'Password reset link sent.'),
    });
  }
  if (status === 'DISABLED') {
    actions.push({
      label: 'Enable account',
      onSelect: () => run(() => updateAdminUserStatus(user.userId, 'ACTIVE'), 'Account enabled.'),
    });
  } else {
    actions.push({
      label: 'Disable account',
      danger: true,
      onSelect: () => run(() => updateAdminUserStatus(user.userId, 'DISABLED'), 'Account disabled.'),
    });
  }

  return (
    <div className="row-actions row-actions--menu">
      {busy && <span className="row-busy" aria-live="polite">Working...</span>}
      <RowActionMenu actions={actions} label={`Actions for ${user.email}`} disabled={busy} />
    </div>
  );
}

interface OrgUsersPanelProps {
  businessId: number;
}

/**
 * User management for one organization: list, invite, resend invite,
 * reset link, role change, enable/disable.
 * Shared by BusinessUsersPage and the superadmin console Users tab.
 */
export default function OrgUsersPanel({ businessId }: OrgUsersPanelProps) {
  const { isSuperAdmin, isBusinessAdmin, businessId: myBusinessId } = useAuth();

  const [users, setUsers] = useState<BusinessUser[]>([]);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const canManage = isSuperAdmin || (isBusinessAdmin && myBusinessId === businessId);
  const { page, pageCount, pageItems, setPage } = usePagedList(users);

  const load = () => {
    if (!canManage) return;
    setLoading(true);
    setError(null);
    listBusinessUsers(businessId)
      .then((res) => setUsers(res.data))
      .catch((err: unknown) => setError(getApiErrorMessage(err, 'Failed to load users.')))
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
    return <DataState type="error">Access denied. You can only manage users in your own organization.</DataState>;
  }

  return (
    <div className="org-users-panel">
      <div className="panel-toolbar">
        <span className="panel-toolbar-summary">
          {loading ? 'Loading...' : `${users.length} user${users.length === 1 ? '' : 's'}`}
        </span>
        {!inviteOpen && (
          <Button size="sm" onClick={() => setInviteOpen(true)}>
            Invite user
          </Button>
        )}
      </div>

      {inviteOpen && (
        <InviteUserForm
          businessId={businessId}
          includeBusinessId={isSuperAdmin}
          onInvited={load}
          onClose={() => setInviteOpen(false)}
        />
      )}

      {loading && <DataState type="loading">Loading users...</DataState>}
      {!loading && error && <DataState type="error">{error}</DataState>}

      {!loading && !error && (
        users.length === 0 ? (
          <DataState>No users in this organization yet. Invite the first one.</DataState>
        ) : (
          <>
            <ResponsiveTable ariaLabel="Organization users">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th className="actions-col" aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {pageItems.map((u) => {
                  const status = userStatus(u);
                  return (
                    <tr key={u.userId}>
                      <td data-label="User">
                        <span className="cell-primary">{u.username || u.email}</span>
                        {u.username && <span className="cell-secondary">{u.email}</span>}
                      </td>
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
                      <td data-label="Actions" className="actions-col">
                        <UserRowActions user={{ ...u, status }} onChanged={load} />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </ResponsiveTable>
            <Pagination
              page={page}
              pageCount={pageCount}
              onPageChange={setPage}
              summary={`${users.length} users`}
            />
          </>
        )
      )}
    </div>
  );
}
