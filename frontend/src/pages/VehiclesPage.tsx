import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getVehicles, deleteVehicle } from '../api/vehicleApi';
import type { Vehicle, VehicleFilters } from '../types/vehicle';
import Navbar from '../components/Navbar';
import { useAuth } from '../auth/AuthContext';

const STATUSES = ['ACTIVE', 'IN_SERVICE', 'INACTIVE', 'SOLD', 'DECOMMISSIONED'];
const TYPES = ['CAR', 'VAN', 'TRUCK', 'MOTORCYCLE', 'OTHER'];
const FUELS = ['DIESEL', 'PETROL', 'HYBRID', 'ELECTRIC', 'LPG', 'OTHER'];
const OWNERSHIPS = ['OWNED', 'LEASED', 'RENTED', 'OTHER'];

export default function VehiclesPage() {
  const { isAdmin } = useAuth();
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [filters, setFilters] = useState<VehicleFilters>({});
  const [error, setError] = useState<string | null>(null);

  const load = (f: VehicleFilters = {}) => {
    setError(null);
    getVehicles(f)
      .then((res) => setVehicles(res.data))
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load vehicles.'));
  };

  useEffect(() => load(), []);

  const setFilter = (field: keyof VehicleFilters) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
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
    } catch (err: any) {
      if (err.response?.status === 403) setError('Access denied.');
      else setError(err.response?.data?.message ?? 'Failed to delete vehicle.');
    }
  };

  return (
    <>
      <Navbar />
      <main className="page">
        <div className="page-header">
          <h1>Vehicles</h1>
          {isAdmin && <Link to="/vehicles/new" className="btn">+ New Vehicle</Link>}
        </div>

        <form onSubmit={handleFilter} className="filters">
          <input placeholder="License plate" value={filters.licensePlate ?? ''} onChange={setFilter('licensePlate')} />
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
          <input placeholder="Department" value={filters.department ?? ''} onChange={setFilter('department')} />
          <button type="submit">Filter</button>
          <button type="button" onClick={() => { setFilters({}); load(); }}>Clear</button>
        </form>

        {error && <p className="error">{error}</p>}

        <table className="vehicles-table">
          <thead>
            <tr>
              <th>License Plate</th>
              <th>Brand / Model</th>
              <th>Type</th>
              <th>Fuel</th>
              <th>Status</th>
              <th>Department</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {vehicles.length === 0 && (
              <tr><td colSpan={7} className="empty">No vehicles found.</td></tr>
            )}
            {vehicles.map((v) => (
              <tr key={v.id}>
                <td>{v.licensePlate}</td>
                <td>{v.brand} {v.model}</td>
                <td>{v.vehicleType}</td>
                <td>{v.fuelType}</td>
                <td><span className={`status-badge status-${v.status}`}>{v.status}</span></td>
                <td>{v.department}</td>
                <td className="actions-cell">
                  <Link to={`/vehicles/${v.id}`}>View</Link>
                  {isAdmin && (
                    <>
                      {' | '}
                      <Link to={`/vehicles/${v.id}/edit`}>Edit</Link>
                      {' | '}
                      <button className="btn-link-danger" onClick={() => handleDelete(v.id)}>Delete</button>
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
