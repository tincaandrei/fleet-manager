import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listVehicleDocumentAttributes } from '../api/documentApi';
import {
  computeComplianceStatus,
  daysUntil,
} from '../types/document';
import type {
  VehicleDocumentAttributeResponse,
  ComplianceStatus,
} from '../types/document';

// ── Card ordering ─────────────────────────────────────────────────────────────
// Canonical types only — CIV / REGISTRATION must never appear in the UI.

interface DocCategory {
  /** Matches documentType from the backend (case-insensitive). */
  key: string;
  label: string;
  subtypeLabel?: string;
}

const CATEGORIES: DocCategory[] = [
  { key: 'INSURANCE',            label: 'Insurance',            subtypeLabel: 'RCA' },
  { key: 'TECHNICAL_INSPECTION', label: 'Technical Inspection', subtypeLabel: 'ITP' },
  { key: 'ROAD_TAX',             label: 'Road Tax',             subtypeLabel: 'Rovinieta' },
  { key: 'EXPENSE_INVOICE',      label: 'Expense Invoice' },
  { key: 'OTHER',                label: 'Other' },
];

// ── Helpers ───────────────────────────────────────────────────────────────────

function statusLabel(s: ComplianceStatus): string {
  switch (s) {
    case 'valid':    return 'Valid';
    case 'expiring': return 'Expiring soon';
    case 'expired':  return 'Expired';
    case 'missing':  return 'Missing';
  }
}

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

function daysLabel(days: number | null): string {
  if (days === null) return '';
  if (days === 0) return 'Expires today';
  if (days < 0) return `${Math.abs(days)}d overdue`;
  return `${days}d left`;
}

// ── Sub-components ────────────────────────────────────────────────────────────

interface CardProps {
  category: DocCategory;
  attr: VehicleDocumentAttributeResponse | null;
  vehicleId: number;
}

function ComplianceCard({ category, attr, vehicleId }: CardProps) {
  const status = computeComplianceStatus(attr?.validUntil);
  const days = daysUntil(attr?.validUntil);
  const subtype = attr?.subtype ?? category.subtypeLabel ?? null;

  return (
    <div className={`compliance-card compliance-card--${status}`}>
      <div className="compliance-card-header">
        <span className="compliance-card-title">
          {category.label}
          {subtype && <span className="compliance-card-subtype">{subtype}</span>}
        </span>
        <span className={`status-badge compliance-status-${status}`}>
          {statusLabel(status)}
        </span>
      </div>

      {attr ? (
        <div className="compliance-card-body">
          {attr.licensePlate && (
            <span className="compliance-meta">Plate: {attr.licensePlate}</span>
          )}
          <span className="compliance-meta">
            Valid until: <strong>{formatDate(attr.validUntil)}</strong>
          </span>
          {days !== null && (
            <span className={`compliance-days compliance-days--${status}`}>
              {daysLabel(days)}
            </span>
          )}
        </div>
      ) : (
        <div className="compliance-card-body">
          <span className="compliance-meta compliance-meta--muted">
            No approved document on record.
          </span>
          <Link
            to={`/vehicles/${vehicleId}?tab=documents`}
            className="compliance-upload-cta"
          >
            Upload document →
          </Link>
        </div>
      )}
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────────────────

interface Props {
  vehicleId: number;
}

export default function ComplianceSection({ vehicleId }: Props) {
  const [attrs, setAttrs] = useState<VehicleDocumentAttributeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    listVehicleDocumentAttributes(vehicleId)
      .then((res) => {
        // For each documentType, keep only the most recent by validUntil
        const byType = new Map<string, VehicleDocumentAttributeResponse>();
        const sorted = [...res.data].sort((a, b) => {
          if (!a.validUntil) return 1;
          if (!b.validUntil) return -1;
          return new Date(b.validUntil).getTime() - new Date(a.validUntil).getTime();
        });
        for (const row of sorted) {
          const key = (row.documentType ?? '').toUpperCase();
          if (!byType.has(key)) byType.set(key, row);
        }
        setAttrs([...byType.values()]);
      })
      .catch((err: unknown) => {
        const e = err as { response?: { data?: { message?: string } } };
        setError(e?.response?.data?.message ?? 'Failed to load compliance data.');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load(); // eslint-disable-line react-hooks/set-state-in-effect
  }, [vehicleId]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) return <p className="doc-empty">Loading compliance data…</p>;
  if (error) return <p className="error">{error}</p>;

  // Build a lookup: documentType → attribute row (or null if missing)
  const lookup = new Map(attrs.map((a) => [(a.documentType ?? '').toUpperCase(), a]));

  return (
    <section className="compliance-section">
      <div className="compliance-grid">
        {CATEGORIES.map((cat) => (
          <ComplianceCard
            key={cat.key}
            category={cat}
            attr={lookup.get(cat.key) ?? null}
            vehicleId={vehicleId}
          />
        ))}
      </div>
    </section>
  );
}
