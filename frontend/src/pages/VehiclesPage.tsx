import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { deleteVehicle, getVehicles } from '../api/vehicleApi';
import type { Vehicle, VehicleFilters } from '../types/vehicle';
import { useAuth } from '../auth/useAuth';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import { Button, ButtonLink } from '../components/ui/Button';
import DataState from '../components/ui/DataState';
import StatusBadge from '../components/ui/StatusBadge';
import VehicleImage from '../components/VehicleImage';
import { getApiErrorMessage } from '../utils/apiError';

const STATUSES = ['ACTIVE', 'IN_SERVICE', 'INACTIVE', 'SOLD', 'DECOMMISSIONED'];
const TYPES = ['CAR', 'VAN', 'TRUCK', 'MOTORCYCLE', 'OTHER'];
const FUELS = ['DIESEL', 'PETROL', 'HYBRID', 'ELECTRIC', 'LPG', 'OTHER'];
const OWNERSHIPS = ['OWNED', 'LEASED', 'RENTED', 'OTHER'];
const PAGE_SIZE = 6;

type VehicleFilterDraft = {
  licensePlate: string;
  department: string;
  businessId: string;
  status: string[];
  vehicleType: string[];
  fuelType: string[];
  ownershipType: string[];
};

const emptyFilterDraft: VehicleFilterDraft = {
  licensePlate: '',
  department: '',
  businessId: '',
  status: [],
  vehicleType: [],
  fuelType: [],
  ownershipType: [],
};

function hasText(value: string, query: string) {
  return value.toLowerCase().includes(query.trim().toLowerCase());
}

function applyVehicleFilters(vehicles: Vehicle[], draft: VehicleFilterDraft) {
  const licensePlate = draft.licensePlate.trim();
  const department = draft.department.trim();
  const businessId = draft.businessId.trim();

  return vehicles.filter((vehicle) => {
    if (licensePlate && !hasText(vehicle.licensePlate, licensePlate)) return false;
    if (department && !hasText(vehicle.department ?? '', department)) return false;
    if (businessId && vehicle.businessId !== Number(businessId)) return false;
    if (draft.status.length > 0 && !draft.status.includes(vehicle.status)) return false;
    if (draft.vehicleType.length > 0 && !draft.vehicleType.includes(vehicle.vehicleType)) return false;
    if (draft.fuelType.length > 0 && !draft.fuelType.includes(vehicle.fuelType)) return false;
    if (draft.ownershipType.length > 0 && !draft.ownershipType.includes(vehicle.ownershipType)) return false;
    return true;
  });
}

function filterCount(draft: VehicleFilterDraft) {
  return [
    draft.licensePlate.trim(),
    draft.department.trim(),
    draft.businessId.trim(),
  ].filter(Boolean).length
    + draft.status.length
    + draft.vehicleType.length
    + draft.fuelType.length
    + draft.ownershipType.length;
}

export default function VehiclesPage() {
  const { isAdmin, isSuperAdmin, role } = useAuth();
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [filterDraft, setFilterDraft] = useState<VehicleFilterDraft>(emptyFilterDraft);
  const [appliedFilters, setAppliedFilters] = useState<VehicleFilterDraft>(emptyFilterDraft);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  const load = (draft: VehicleFilterDraft = appliedFilters) => {
    setLoading(true);
    setError(null);
    const apiFilters: VehicleFilters = {};
    if (isSuperAdmin && draft.businessId.trim()) {
      apiFilters.businessId = Number(draft.businessId);
    }
    getVehicles(apiFilters)
      .then((res) => {
        setVehicles(applyVehicleFilters(res.data, draft));
        setPage(1);
      })
      .catch((err: unknown) => setError(getApiErrorMessage(err, 'Failed to load vehicles.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timeoutId = window.setTimeout(() => load(emptyFilterDraft), 0);
    return () => window.clearTimeout(timeoutId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setTextFilter =
    (field: 'licensePlate' | 'department' | 'businessId') =>
    (e: React.ChangeEvent<HTMLInputElement>) =>
      setFilterDraft((prev) => ({ ...prev, [field]: e.target.value }));

  const toggleFilterValue = (
    field: 'status' | 'vehicleType' | 'fuelType' | 'ownershipType',
    value: string,
  ) => {
    setFilterDraft((prev) => {
      const selected = prev[field];
      return {
        ...prev,
        [field]: selected.includes(value)
          ? selected.filter((item) => item !== value)
          : [...selected, value],
      };
    });
  };

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault();
    setAppliedFilters(filterDraft);
    load(filterDraft);
    setFiltersOpen(false);
  };

  const handleClearFilters = () => {
    setFilterDraft(emptyFilterDraft);
    setAppliedFilters(emptyFilterDraft);
    load(emptyFilterDraft);
    setFiltersOpen(false);
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
  const pageCount = Math.max(1, Math.ceil(vehicles.length / PAGE_SIZE));
  const pageStart = (page - 1) * PAGE_SIZE;
  const selectedFilterCount = filterCount(filterDraft);
  const appliedFilterCount = filterCount(appliedFilters);
  const paginatedVehicles = useMemo(
    () => vehicles.slice(pageStart, pageStart + PAGE_SIZE),
    [vehicles, pageStart],
  );

  useEffect(() => {
    if (page > pageCount) {
      setPage(pageCount);
    }
  }, [page, pageCount]);

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

      <section className="filter-shell" aria-label="Vehicle filters">
        <div className="filter-toolbar">
          <Button
            variant={filtersOpen ? 'secondary' : 'primary'}
            onClick={() => setFiltersOpen((open) => !open)}
            aria-expanded={filtersOpen}
            aria-controls="vehicle-filter-panel"
          >
            Filters{appliedFilterCount > 0 ? ` (${appliedFilterCount})` : ''}
          </Button>
          {appliedFilterCount > 0 && (
            <Button variant="ghost" onClick={handleClearFilters}>
              Clear
            </Button>
          )}
        </div>

        {filtersOpen && (
          <form id="vehicle-filter-panel" onSubmit={handleFilter} className="filter-panel">
            <div className="filter-search-row">
              <label>
                License plate
                <input
                  placeholder="B-101-ATL"
                  value={filterDraft.licensePlate}
                  onChange={setTextFilter('licensePlate')}
                />
              </label>
              <label>
                Department
                <input
                  placeholder="Operations"
                  value={filterDraft.department}
                  onChange={setTextFilter('department')}
                />
              </label>
              {isSuperAdmin && (
                <label>
                  Organization ID
                  <input
                    type="number"
                    placeholder="101"
                    value={filterDraft.businessId}
                    onChange={setTextFilter('businessId')}
                  />
                </label>
              )}
            </div>

            <div className="filter-groups">
              <fieldset className="filter-group">
                <legend>Status</legend>
                {STATUSES.map((status) => (
                  <label key={status} className="filter-check">
                    <input
                      type="checkbox"
                      checked={filterDraft.status.includes(status)}
                      onChange={() => toggleFilterValue('status', status)}
                    />
                    <span>{status}</span>
                  </label>
                ))}
              </fieldset>

              <fieldset className="filter-group">
                <legend>Type</legend>
                {TYPES.map((type) => (
                  <label key={type} className="filter-check">
                    <input
                      type="checkbox"
                      checked={filterDraft.vehicleType.includes(type)}
                      onChange={() => toggleFilterValue('vehicleType', type)}
                    />
                    <span>{type}</span>
                  </label>
                ))}
              </fieldset>

              <fieldset className="filter-group">
                <legend>Fuel</legend>
                {FUELS.map((fuel) => (
                  <label key={fuel} className="filter-check">
                    <input
                      type="checkbox"
                      checked={filterDraft.fuelType.includes(fuel)}
                      onChange={() => toggleFilterValue('fuelType', fuel)}
                    />
                    <span>{fuel}</span>
                  </label>
                ))}
              </fieldset>

              <fieldset className="filter-group">
                <legend>Ownership</legend>
                {OWNERSHIPS.map((ownership) => (
                  <label key={ownership} className="filter-check">
                    <input
                      type="checkbox"
                      checked={filterDraft.ownershipType.includes(ownership)}
                      onChange={() => toggleFilterValue('ownershipType', ownership)}
                    />
                    <span>{ownership}</span>
                  </label>
                ))}
              </fieldset>
            </div>

            <div className="filter-actions">
              <Button type="submit">Apply{selectedFilterCount > 0 ? ` (${selectedFilterCount})` : ''}</Button>
              <Button variant="secondary" onClick={handleClearFilters}>Reset</Button>
            </div>
          </form>
        )}
      </section>

      {role === 'EMPLOYEE' && (
        <DataState type="info">Showing only vehicles assigned to your account.</DataState>
      )}

      {error && <DataState type="error">{error}</DataState>}
      {loading && <DataState type="loading">Loading vehicles...</DataState>}

      {!loading && !error && (
        <>
          {vehicles.length === 0 ? (
            <DataState>No vehicles found.</DataState>
          ) : (
            <>
              <div className="vehicle-card-grid" aria-label="Vehicles">
                {paginatedVehicles.map((v) => (
                  <article key={v.id} className="vehicle-card">
                    <Link
                      to={`/vehicles/${v.id}`}
                      className="vehicle-card-media"
                      aria-label={`Open ${v.licensePlate} details`}
                    >
                      <VehicleImage vehicle={v} className="vehicle-card-image" />
                    </Link>

                    <div className="vehicle-card-body">
                      <div className="vehicle-card-heading">
                        <div>
                          <Link to={`/vehicles/${v.id}`} className="vehicle-card-title">
                            {v.licensePlate}
                          </Link>
                          <span className="vehicle-card-subtitle">{v.brand} {v.model}</span>
                        </div>
                        <StatusBadge status={v.status} />
                      </div>

                      <div className="vehicle-card-chips" aria-label="Vehicle attributes">
                        <span>{v.vehicleType}</span>
                        <span>{v.fuelType}</span>
                        <span>{v.ownershipType}</span>
                      </div>

                      <dl className="vehicle-card-facts">
                        <div>
                          <dt>Mileage</dt>
                          <dd>{v.currentMileage.toLocaleString()} km</dd>
                        </div>
                        <div>
                          <dt>Department</dt>
                          <dd>{v.department || '-'}</dd>
                        </div>
                        <div>
                          <dt>Driver</dt>
                          <dd>{v.assignedDriverName || '-'}</dd>
                        </div>
                        {isSuperAdmin && (
                          <div>
                            <dt>Organization</dt>
                            <dd>{v.businessId ?? '-'}</dd>
                          </div>
                        )}
                      </dl>

                      <div className="vehicle-card-actions">
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
                      </div>
                    </div>
                  </article>
                ))}
              </div>

              {pageCount > 1 && (
                <nav className="vehicle-pagination" aria-label="Vehicle pages">
                  <Button
                    variant="secondary"
                    disabled={page === 1}
                    onClick={() => setPage((current) => Math.max(1, current - 1))}
                  >
                    Previous
                  </Button>
                  <span>Page {page} of {pageCount}</span>
                  <Button
                    variant="secondary"
                    disabled={page === pageCount}
                    onClick={() => setPage((current) => Math.min(pageCount, current + 1))}
                  >
                    Next
                  </Button>
                </nav>
              )}
            </>
          )}
        </>
      )}
    </PageShell>
  );
}
