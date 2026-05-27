import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getVehicles, deleteVehicle } from '../api/vehicleApi';
import type { Vehicle, VehicleFilters } from '../types/vehicle';
import Navbar from '../components/Navbar';
import { useAuth } from '../auth/AuthContext';

const STATUSES   = ['ACTIVE', 'IN_SERVICE', 'INACTIVE', 'SOLD', 'DECOMMISSIONED'];
const TYPES      = ['CAR', 'VAN', 'TRUCK', 'MOTORCYCLE', 'OTHER'];
const FUELS      = ['DIESEL', 'PETROL', 'HYBRID', 'ELECTRIC', 'LPG', 'OTHER'];
const OWNERSHIPS = ['OWNED', 'LEASED', 'RENTED', 'OTHER'];

export default function VehiclesPage() {
  const { isAdmin, isSuperAdmin, role, businessId } = useAuth();
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [filters, setFilters]   = useState<VehicleFilters>({});
  const [error, setError]       = useState<string | null>(null);

  /**
   * Build the initial filter set based on role:
   * - EMPLOYEE: backend already scopes by assignedUserId, no extra filter needed
   * - BUSINESS_ADMIN: backend scopes by businessId, no extra filter needed
   * - SUPERADMIN: no default scope, but may manually filter by businessId
   */
  const load = (f: VehicleFilters = filters) => {
    setError(null);
    getVehicles(f)
      .then((res) => setVehicles(res.data))
      .catch((err: unknown) => {
        const e = err as { response?: { data?: { message?: string } } };
        setError(e.response?.data?.message ?? 'Failed to load vehicles.');
      });
  };

  useEffect(() => {
    load({});
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
      const e = err as { response?: { status?: number; data?: { message?: string } } };
      setError(e.response?.data?.message ?? 'Failed to delete vehicle.');
    }
  };

  return (
    <>
      <Navbar />
      <main className="page">
        <div className="page-header">
          <h1>
            {role === 'EMPLOYEE' ? 'My Vehicles' : 'Fleet'}
          </h1>
          {isAdmin && (
            <Link to="/vehicles/new" className="btn">
              ＋ New Vehicle
            </Link>
          )}
        </div>

        {/* ── Filters ── */}
        <form onSubmit={handleFilter} className="filters">
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

          {/* SUPERADMIN can filter across businesses */}
          {isSuperAdmin && (
            <input
              type="number"
              placeholder="Business ID"
              value={filters.businessId ?? ''}
              onChange={(e) =>
                setFilters((prev) => ({
                  ...prev,
                  businessId: e.target.value ? Number(e.target.value) : undefined,
                }))
              }
            />
          )}

          <button type="submit">Filter</button>
          <button
            type="button"
            onClick={() => {
              setFilters({});
              load({});
            }}
          >
            Clear
          </button>
        </form>

        {/* Contextual note for EMPLOYEE */}
        {role === 'EMPLOYEE' && (
          <p className="info-note">Showing only vehicles assigned to your account.</p>
        )}

        {/* Contextual note for BUSINESS_ADMIN */}
        {role === 'BUSINESS_ADMIN' && businessId != null && (
          <p className="info-note">Showing all vehicles in your business (ID {businessId}).</p>
        )}

        {error && <p className="error">{error}</p>}

        {/* ── Table (desktop) / Card list (mobile via CSS + data-label) ── */}
        <table className="vehicles-table">
          <thead>
            <tr>
              <th>License Plate</th>
              <th>Brand / Model</th>
              <th>Type</th>
              <th>Fuel</th>
              <th>Status</th>
              <th>Department</th>
              {isSuperAdmin && <th>Business</th>}
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
                <td data-label="License Plate">{v.licensePlate}</td>
                <td data-label="Brand / Model">{v.brand} {v.model}</td>
                <td data-label="Type">{v.vehicleType}</td>
                <td data-label="Fuel">{v.fuelType}</td>
                <td data-label="Status">
                  <span className={`status-badge status-${v.status}`}>{v.status}</span>
                </td>
                <td data-label="Department">{v.department}</td>
                {isSuperAdmin && (
                  <td data-label="Business">{v.businessId ?? '—'}</td>
                )}
                <td data-label="Actions" className="actions-cell">
                  <Link to={`/vehicles/${v.id}`}>View</Link>
                  {isAdmin && (
                    <>
                      {' | '}
                      <Link to={`/vehicles/${v.id}/edit`}>Edit</Link>
                      {' | '}
                      <button
                        className="btn-link-danger"
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
        </table>
      </main>
    </>
  );
}
