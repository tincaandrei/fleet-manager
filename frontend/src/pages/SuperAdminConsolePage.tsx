import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { listBusinesses, listBusinessUsers, listUnassignedUsers } from '../api/authApi';
import { getVehicles } from '../api/vehicleApi';
import { listVehicleAlerts } from '../api/documentApi';
import type { Business, BusinessUser, UserProfile } from '../types/auth';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import { Button } from '../components/ui/Button';
import DataState from '../components/ui/DataState';
import OrganizationForm from '../components/superadmin/OrganizationForm';
import PendingAccountsSection from '../components/superadmin/PendingAccountsSection';
import OrgUsersPanel from '../components/superadmin/OrgUsersPanel';
import OrgVehiclesPanel from '../components/superadmin/OrgVehiclesPanel';
import OrgReportsPanel from '../components/superadmin/OrgReportsPanel';
import { getApiErrorMessage } from '../utils/apiError';
import { showToast } from '../utils/toast';

const TABS = ['overview', 'users', 'vehicles', 'reports', 'settings'] as const;
type WorkspaceTab = (typeof TABS)[number];

const TAB_LABELS: Record<WorkspaceTab, string> = {
  overview: 'Overview',
  users: 'Users',
  vehicles: 'Vehicles',
  reports: 'Reports',
  settings: 'Settings',
};

function normalizeTab(value: string | null): WorkspaceTab {
  return (TABS as readonly string[]).includes(value ?? '') ? (value as WorkspaceTab) : 'overview';
}

export default function SuperAdminConsolePage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [unassignedUsers, setUnassignedUsers] = useState<UserProfile[]>([]);
  const [totalVehicles, setTotalVehicles] = useState<number | null>(null);
  const [alertVehicleCount, setAlertVehicleCount] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');

  const selectedId = Number(searchParams.get('org')) || null;
  const activeTab = normalizeTab(searchParams.get('tab'));
  const createOpen = searchParams.get('action') === 'new-org';

  const selectedBusiness = useMemo(
    () => businesses.find((b) => b.id === selectedId) ?? null,
    [businesses, selectedId],
  );

  const updateParams = useCallback(
    (updates: Record<string, string | null>) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        Object.entries(updates).forEach(([key, value]) => {
          if (value === null) next.delete(key);
          else next.set(key, value);
        });
        return next;
      }, { replace: true });
    },
    [setSearchParams],
  );

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    Promise.all([listBusinesses(), listUnassignedUsers()])
      .then(([businessRes, unassignedRes]) => {
        setBusinesses(businessRes.data);
        setUnassignedUsers(unassignedRes.data);
      })
      .catch((err: unknown) => setError(getApiErrorMessage(err, 'Failed to load organizations.')))
      .finally(() => setLoading(false));

    // Optional metrics: never break the page when one of them fails.
    getVehicles()
      .then((res) => setTotalVehicles(res.data.length))
      .catch(() => setTotalVehicles(null));
    listVehicleAlerts(30, true)
      .then((res) => setAlertVehicleCount(res.data.length))
      .catch(() => setAlertVehicleCount(null));
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => load(), 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const filteredBusinesses = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return businesses;
    return businesses.filter((business) =>
      [business.name, business.registrationNumber, business.contactEmail, String(business.id)]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(query)),
    );
  }, [businesses, search]);

  const activeCount = businesses.filter((b) => b.active).length;

  const handleSelect = (business: Business) => {
    updateParams({ org: String(business.id), action: null });
  };

  const handleCreated = (created: Business) => {
    setBusinesses((prev) => [...prev, created]);
    updateParams({ org: String(created.id), tab: 'overview', action: null });
    showToast({ type: 'success', message: `Organization "${created.name}" created.` });
  };

  const handleUpdated = (updated: Business) => {
    setBusinesses((prev) => prev.map((b) => (b.id === updated.id ? updated : b)));
    showToast({ type: 'success', message: 'Organization updated.' });
  };

  const handleAssigned = (userId: number) => {
    setUnassignedUsers((current) => current.filter((user) => user.userId !== userId));
    showToast({ type: 'success', message: 'Account assigned to organization.' });
  };

  const handleInviteUser = () => {
    if (selectedBusiness) {
      updateParams({ tab: 'users', action: null });
    } else {
      showToast({ type: 'info', message: 'Select an organization first to invite a user.' });
    }
  };

  const handleNewVehicle = () => {
    navigate(selectedBusiness ? `/vehicles/new?businessId=${selectedBusiness.id}` : '/vehicles/new');
  };

  const handleDownloadReports = () => {
    if (selectedBusiness) {
      updateParams({ tab: 'reports', action: null });
    } else {
      showToast({ type: 'info', message: 'Global downloads are shown in the workspace below.' });
    }
  };

  return (
    <PageShell className="superadmin-console">
      <PageHeader
        title="Superadmin Console"
        description="Manage organizations, users, vehicles, and reports across the platform."
        actions={(
          <>
            <Button onClick={() => updateParams({ action: createOpen ? null : 'new-org' })}>
              New Organization
            </Button>
            <Button variant="secondary" onClick={handleInviteUser}>Invite User</Button>
            <Button variant="secondary" onClick={handleNewVehicle}>New Vehicle</Button>
            <Button variant="secondary" onClick={handleDownloadReports}>Download Reports</Button>
          </>
        )}
      />

      <section className="vehicle-metrics" aria-label="Platform summary">
        <div className="metric-card">
          <span className="metric-label">Organizations</span>
          <span className="metric-value">{loading ? '—' : businesses.length}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Active organizations</span>
          <span className="metric-value">{loading ? '—' : activeCount}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Pending accounts</span>
          <span className="metric-value">{loading ? '—' : unassignedUsers.length}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Total vehicles</span>
          <span className="metric-value">{totalVehicles ?? '—'}</span>
        </div>
        {alertVehicleCount !== null && (
          <div className="metric-card">
            <span className="metric-label">Vehicles with doc alerts</span>
            <span className="metric-value">{alertVehicleCount}</span>
          </div>
        )}
      </section>

      {createOpen && (
        <section className="management-section console-create-panel">
          <div className="section-header">
            <h2>New Organization</h2>
          </div>
          <OrganizationForm
            onSaved={handleCreated}
            onCancel={() => updateParams({ action: null })}
          />
        </section>
      )}

      {loading && <DataState type="loading">Loading organizations...</DataState>}
      {!loading && error && <DataState type="error">{error}</DataState>}

      {!loading && !error && (
        <>
          {unassignedUsers.length > 0 && (
            <PendingAccountsSection
              businesses={businesses}
              users={unassignedUsers}
              onAssigned={handleAssigned}
            />
          )}

          <div className="console-layout">
            <aside className="console-org-browser" aria-label="Organization browser">
              <div className="section-header">
                <h2>Organizations</h2>
                <span>{filteredBusinesses.length} of {businesses.length}</span>
              </div>
              <input
                type="search"
                className="console-org-search"
                placeholder="Search by name, registration, email..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                aria-label="Search organizations"
              />

              {businesses.length === 0 && (
                <DataState>No organizations yet. Create the first one.</DataState>
              )}
              {businesses.length > 0 && filteredBusinesses.length === 0 && (
                <DataState>No organizations match your search.</DataState>
              )}

              <ul className="console-org-list">
                {filteredBusinesses.map((business) => (
                  <li key={business.id}>
                    <button
                      type="button"
                      className={`console-org-item${business.id === selectedId ? ' console-org-item--selected' : ''}`}
                      onClick={() => handleSelect(business)}
                      aria-pressed={business.id === selectedId}
                    >
                      <span className="console-org-name">{business.name}</span>
                      <span className="console-org-meta">
                        <span className={`status-badge status-${business.active ? 'ACTIVE' : 'DISABLED'}`}>
                          {business.active ? 'Active' : 'Inactive'}
                        </span>
                        <span className="console-org-id">#{business.id}</span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </aside>

            <section className="console-workspace" aria-label="Organization workspace">
              {!selectedBusiness && (
                <>
                  <DataState>
                    Select an organization to manage its users, vehicles, reports, and settings.
                  </DataState>
                  <OrgReportsPanel />
                </>
              )}

              {selectedBusiness && (
                <>
                  <div className="console-workspace-header">
                    <div>
                      <h2>{selectedBusiness.name}</h2>
                      <p className="console-workspace-subtitle">
                        #{selectedBusiness.id}
                        {selectedBusiness.registrationNumber ? ` · ${selectedBusiness.registrationNumber}` : ''}
                        {' · '}
                        {selectedBusiness.active ? 'Active' : 'Inactive'}
                      </p>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => updateParams({ org: null, tab: null })}
                    >
                      Clear selection
                    </Button>
                  </div>

                  <nav className="mini-tabs console-tabs" aria-label="Organization sections">
                    {TABS.map((tab) => (
                      <Button
                        key={tab}
                        variant={activeTab === tab ? 'primary' : 'secondary'}
                        size="sm"
                        onClick={() => updateParams({ tab })}
                      >
                        {TAB_LABELS[tab]}
                      </Button>
                    ))}
                  </nav>

                  {activeTab === 'overview' && (
                    <OrgOverviewPanel key={selectedBusiness.id} business={selectedBusiness} />
                  )}
                  {activeTab === 'users' && <OrgUsersPanel businessId={selectedBusiness.id} />}
                  {activeTab === 'vehicles' && <OrgVehiclesPanel businessId={selectedBusiness.id} />}
                  {activeTab === 'reports' && (
                    <OrgReportsPanel
                      businessId={selectedBusiness.id}
                      businessName={selectedBusiness.name}
                    />
                  )}
                  {activeTab === 'settings' && (
                    <OrganizationForm
                      key={selectedBusiness.id}
                      business={selectedBusiness}
                      onSaved={handleUpdated}
                    />
                  )}
                </>
              )}
            </section>
          </div>
        </>
      )}
    </PageShell>
  );
}

interface OrgOverviewPanelProps {
  business: Business;
}

/** Rendered with key={business.id} so state resets when the selection changes. */
function OrgOverviewPanel({ business }: OrgOverviewPanelProps) {
  const [users, setUsers] = useState<BusinessUser[] | null>(null);
  const [vehicleCount, setVehicleCount] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    Promise.allSettled([listBusinessUsers(business.id), getVehicles({ businessId: business.id })])
      .then(([usersResult, vehiclesResult]) => {
        if (cancelled) return;
        if (usersResult.status === 'fulfilled') setUsers(usersResult.value.data);
        if (vehiclesResult.status === 'fulfilled') setVehicleCount(vehiclesResult.value.data.length);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [business.id]);

  const invitedCount = users?.filter((u) => (u.status ?? 'ACTIVE') === 'INVITED').length ?? null;
  const disabledCount = users?.filter((u) => (u.status ?? 'ACTIVE') === 'DISABLED').length ?? null;

  return (
    <div className="org-overview-panel">
      <section className="vehicle-metrics" aria-label="Organization summary">
        <div className="metric-card">
          <span className="metric-label">Users</span>
          <span className="metric-value">{loading ? '—' : users?.length ?? '—'}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Invited</span>
          <span className="metric-value">{loading ? '—' : invitedCount ?? '—'}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Disabled</span>
          <span className="metric-value">{loading ? '—' : disabledCount ?? '—'}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Vehicles</span>
          <span className="metric-value">{loading ? '—' : vehicleCount ?? '—'}</span>
        </div>
      </section>

      <dl className="console-org-details">
        <div>
          <dt>Registration number</dt>
          <dd>{business.registrationNumber || '-'}</dd>
        </div>
        <div>
          <dt>Contact email</dt>
          <dd>{business.contactEmail || '-'}</dd>
        </div>
        <div>
          <dt>Phone</dt>
          <dd>{business.phone || '-'}</dd>
        </div>
        <div>
          <dt>Address</dt>
          <dd>{business.address || '-'}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>{business.active ? 'Active' : 'Inactive'}</dd>
        </div>
        {business.createdAt && (
          <div>
            <dt>Created</dt>
            <dd>{new Date(business.createdAt).toLocaleDateString()}</dd>
          </div>
        )}
      </dl>
    </div>
  );
}
