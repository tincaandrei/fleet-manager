import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { deleteVehicle, getVehicles } from '../api/vehicleApi';
import type { Vehicle, VehicleFilters } from '../types/vehicle';
import { useAuth } from '../auth/useAuth';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import { Button, ButtonLink } from '../components/ui/Button';
import DataState from '../components/ui/DataState';
import ResponsiveTable from '../components/ui/ResponsiveTable';
import StatusBadge from '../components/ui/StatusBadge';
import { getApiErrorMessage } from '../utils/apiError';

const STATUSES   = ['ACTIVE', 'IN_SERVICE', 'INACTIVE', 'SOLD', 'DECOMMISSIONED'];
const TYPES      = ['CAR', 'VAN', 'TRUCK', 'MOTORCYCLE', 'OTHER'];
const FUELS      = ['DIESEL', 'PETROL', 'HYBRID', 'ELECTRIC', 'LPG', 'OTHER'];
const OWNERSHIPS = ['OWNED', 'LEASED', 'RENTED', 'OTHER'];

export default function VehiclesPage() {
  const { isAdmin, isSuperAdmin, role, businessId } = useAuth();
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [filters, setFilters]   = useState<VehicleFilters>({});
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState<string | null>(null);

  /**
   * Build the initial filter set based on role:
   * - EMPLOYEE: backend already scopes by assignedUserId, no extra filter needed
   * - BUSINESS_ADMIN: backend scopes by organization, no extra filter needed
   * - SUPERADMIN: no default scope, but may manually filter by businessId
   */
  const load = (f: VehicleFilters = filters) => {
    setLoading(true);
    setError(null);
    getVehicles(f)
      .then((res) => setVehicles(res.data))
      .catch((err: unknown) => setError(getApiErrorMessage(err, 'Failed to load vehicles.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timeoutId = window.setTimeout(() => load({}), 0);
    return () => window.clearTimeout(timeoutId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setFilter =
    (field: keyof VehicleFilters) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setFilters((prev) => ({ ...prev, [field]: e.target.value || undefined }));

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault();
    load(filters);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Delete this vehicle? This cannot be undone.')) return;
    try {
      await deleteVehicle(id);
      setVehicles((prev) => prev.filter((v) => v.id !== id));
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to delete vehicle.'));
    }
  };

  const activeCount = vehicles.filter((vehicle) => vehicle.status === 'ACTIVE').length;
  const serviceCount = vehicles.filter((vehicle) => vehicle.status === 'IN_SERVICE').length;
  const inactiveCount = vehicles.filter((vehicle) => vehicle.status === 'INACTIVE').length;

  return (
    <PageShell>
      <PageHeader
        title={role === 'EMPLOYEE' ? 'My Vehicles' : 'Fleet'}
        description={
          role === 'EMPLOYEE'
            ? 'Vehicles assigned to your account.'
            : 'Search, filter, and manage fleet vehicles.'
        }
        actions={isAdmin && <ButtonLink to="/vehicles/new">+ New Vehicle</ButtonLink>}
      />

      <section className="vehicle-metrics" aria-label="Fleet summary">
        <div className="metric-card">
          <span className="metric-label">Total</span>
          <span className="metric-value">{vehicles.length}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Active</span>
          <span className="metric-value">{activeCount}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">In service</span>
          <span className="metric-value">{serviceCount}</span>
        </div>
        <div className="metric-card">
          <span className="metric-label">Inactive</span>
          <span className="metric-value">{inactiveCount}</span>
        </div>
      </section>

        {/* ── Filters ── */}
      <form onSubmit={handleFilter} className="filters filters-modern">
          <input
            placeholder="License plate"
            value={filters.licensePlate ?? ''}
            onChange={setFilter('licensePlate')}
          />
          <select value={filters.status ?? ''} onChange={setFilter('status')}>
            <option value="">All statuses</option>
            {STATUSES.map((s) => <option key={s}>{s}</option>)}
          </select>
          <select value={filters.vehicleType ?? ''} onChange={setFilter('vehicleType')}>
            <option value="">All types</option>
            {TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
          <select value={filters.fuelType ?? ''} onChange={setFilter('fuelType')}>
            <option value="">All fuels</option>
            {FUELS.map((f) => <option key={f}>{f}</option>)}
          </select>
          <select value={filters.ownershipType ?? ''} onChange={setFilter('ownershipType')}>
            <option value="">All ownerships</option>
            {OWNERSHIPS.map((o) => <option key={o}>{o}</option>)}
          </select>
          <input
            placeholder="Department"
            value={filters.department ?? ''}
            onChange={setFilter('department')}
          />

          {/* SUPERADMIN can filter across organizations */}
          {isSuperAdmin && (
            <input
              type="number"
              placeholder="Organization ID"
              value={filters.businessId ?? ''}
              onChange={(e) =>
                setFilters((prev) => ({
                  ...prev,
                  businessId: e.target.value ? Number(e.target.value) : undefined,
                }))
              }
            />
          )}

          <Button type="submit">Filter</Button>
          <Button
            variant="secondary"
            onClick={() => {
              setFilters({});
              load({});
            }}
          >
            Clear
          </Button>
        </form>

        {/* Contextual note for EMPLOYEE */}
        {role === 'EMPLOYEE' && (
          <DataState type="info">Showing only vehicles assigned to your account.</DataState>
        )}

        {/* Contextual note for BUSINESS_ADMIN */}
        {role === 'BUSINESS_ADMIN' && businessId != null && (
          <DataState type="info">Showing all vehicles in your organization (ID {businessId}).</DataState>
        )}

        {error && <DataState type="error">{error}</DataState>}
        {loading && <DataState type="loading">Loading vehicles...</DataState>}

        {/* ── Table (desktop) / Card list (mobile via CSS + data-label) ── */}
        {!loading && !error && (
        <ResponsiveTable ariaLabel="Vehicles">
          <thead>
            <tr>
              <th>License Plate</th>
              <th>Brand / Model</th>
              <th>Type</th>
              <th>Fuel</th>
              <th>Status</th>
              <th>Department</th>
              {isSuperAdmin && <th>Organization</th>}
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {vehicles.length === 0 && (
              <tr>
                <td colSpan={isSuperAdmin ? 8 : 7} className="empty">No vehicles found.</td>
              </tr>
            )}
            {vehicles.map((v) => (
              <tr key={v.id}>
                <td data-label="License Plate">
                  <div className="vehicle-identity">
                    <Link to={`/vehicles/${v.id}`} className="vehicle-title-link">{v.licensePlate}</Link>
                    <span className="vehicle-subtext">VIN {v.vin}</span>
                  </div>
                </td>
                <td data-label="Brand / Model">
                  <div className="vehicle-identity">
                    <span>{v.brand} {v.model}</span>
                    <span className="vehicle-subtext">{v.currentMileage.toLocaleString()} km</span>
                  </div>
                </td>
                <td data-label="Type">{v.vehicleType}</td>
                <td data-label="Fuel">{v.fuelType}</td>
                <td data-label="Status">
                  <StatusBadge status={v.status} />
                </td>
                <td data-label="Department">{v.department}</td>
                {isSuperAdmin && (
                  <td data-label="Organization">{v.businessId ?? '-'}</td>
                )}
                <td data-label="Actions" className="actions-cell">
                  <ButtonLink to={`/vehicles/${v.id}`} size="sm" variant="secondary">View</ButtonLink>
                  {isAdmin && (
                    <>
                      <ButtonLink to={`/vehicles/${v.id}/edit`} size="sm" variant="ghost">Edit</ButtonLink>
                      <button
                        className="btn-link-danger"
                        type="button"
                        onClick={() => handleDelete(v.id)}
                      >
                        Delete
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </ResponsiveTable>
        )}
    </PageShell>
  );
}
