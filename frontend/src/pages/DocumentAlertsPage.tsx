import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { listVehicleAlerts } from '../api/documentApi';
import { computeComplianceStatus, daysUntil } from '../types/document';
import type { VehicleAlertGroup, VehicleDocumentAttributeResponse, ComplianceStatus } from '../types/document';
import PageShell from '../components/ui/PageShell';
import DataState from '../components/ui/DataState';

// ── Helpers ───────────────────────────────────────────────────────────────────

const DAYS_OPTIONS = [7, 15, 30, 60] as const;

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

function daysCell(validUntil: string | null | undefined): string {
  const d = daysUntil(validUntil);
  if (d === null) return '—';
  if (d < 0) return `${Math.abs(d)}d ago`;
  if (d === 0) return 'Today';
  return `${d}d`;
}

function statusLabel(s: ComplianceStatus): string {
  switch (s) {
    case 'valid':    return 'Valid';
    case 'expiring': return 'Expiring';
    case 'expired':  return 'Expired';
    case 'missing':  return 'Missing';
  }
}

function apiMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

// ── Alert row ─────────────────────────────────────────────────────────────────

function AlertRow({ alert, vehicleId }: { alert: VehicleDocumentAttributeResponse; vehicleId: number }) {
  const status = computeComplianceStatus(alert.validUntil);
  return (
    <tr>
      <td data-label="Doc type">{alert.documentType}</td>
      <td data-label="Subtype">{alert.subtype ?? '—'}</td>
      <td data-label="Valid until">{formatDate(alert.validUntil)}</td>
      <td
        data-label="Days"
        className={`alerts-days alerts-days--${status}`}
      >
        {daysCell(alert.validUntil)}
      </td>
      <td data-label="Status">
        <span className={`status-badge compliance-status-${status}`}>
          {statusLabel(status)}
        </span>
      </td>
      <td data-label="">
        <Link to={`/vehicles/${vehicleId}`} className="btn btn-sm">
          View
        </Link>
      </td>
    </tr>
  );
}

// ── Vehicle group card ────────────────────────────────────────────────────────

function VehicleGroupCard({ group }: { group: VehicleAlertGroup }) {
  const plate = group.licensePlate ?? group.vin ?? `Vehicle ${group.vehicleId}`;
  const title = group.brand && group.model
    ? `${group.brand} ${group.model} — ${plate}`
    : plate;

  return (
    <div className="alerts-vehicle-group">
      <div className="alerts-vehicle-group-header">
        <Link to={`/vehicles/${group.vehicleId}`} className="alerts-vehicle-title">
          {title}
        </Link>
        <span className="alerts-vehicle-count">
          {group.alerts.length} alert{group.alerts.length !== 1 ? 's' : ''}
        </span>
      </div>

      <div className="alerts-table-wrapper">
        <table className="alerts-table">
          <thead>
            <tr>
              <th>Doc type</th>
              <th>Subtype</th>
              <th>Valid until</th>
              <th>Days</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {group.alerts.map((alert) => (
              <AlertRow key={alert.id} alert={alert} vehicleId={group.vehicleId} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function DocumentAlertsPage() {
  const { isAdmin } = useAuth();

  const [days, setDays]                   = useState<number>(30);
  const [includeExpired, setIncludeExpired] = useState(true);
  const [groups, setGroups]               = useState<VehicleAlertGroup[]>([]);
  const [loading, setLoading]             = useState(false);
  const [error, setError]                 = useState<string | null>(null);

  const totalAlerts = groups.reduce((sum, g) => sum + g.alerts.length, 0);

  const load = () => {
    if (!isAdmin) return;
    setLoading(true);
    setError(null);
    listVehicleAlerts(days, includeExpired)
      .then((res) => setGroups(res.data))
      .catch((err: unknown) => setError(apiMessage(err, 'Failed to load alerts.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timeoutId = window.setTimeout(() => load(), 0);
    return () => window.clearTimeout(timeoutId);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [days, includeExpired, isAdmin]);

  return (
    <PageShell>
        <div className="page-header">
          <h1>Document Alerts</h1>
          <Link to="/vehicles">← Vehicles</Link>
        </div>

        {/* ── Filters ── */}
        <div className="filters alerts-filters">
          <label className="alerts-filter-label">
            Expiring within
            <select value={days} onChange={(e) => setDays(Number(e.target.value))}>
              {DAYS_OPTIONS.map((d) => (
                <option key={d} value={d}>{d} days</option>
              ))}
            </select>
          </label>

          <label className="alerts-checkbox-label">
            <input
              type="checkbox"
              checked={includeExpired}
              onChange={(e) => setIncludeExpired(e.target.checked)}
            />
            Include already expired
          </label>
        </div>

        {/* ── Status line ── */}
        {loading && <DataState type="loading">Loading alerts...</DataState>}
        {!loading && error && <DataState type="error">{error}</DataState>}
        {!loading && !error && groups.length === 0 && (
          <DataState>
            No document alerts within {days} days
            {includeExpired ? ' (including already expired)' : ''}.
          </DataState>
        )}

        {!loading && !error && groups.length > 0 && (
          <>
            <p className="alerts-summary">
              {totalAlerts} alert{totalAlerts !== 1 ? 's' : ''} across{' '}
              {groups.length} vehicle{groups.length !== 1 ? 's' : ''}
            </p>

            {/* ── Grouped by vehicle ── */}
            <div className="alerts-groups">
              {groups.map((group) => (
                <VehicleGroupCard key={group.vehicleId} group={group} />
              ))}
            </div>
          </>
        )}
    </PageShell>
  );
}
