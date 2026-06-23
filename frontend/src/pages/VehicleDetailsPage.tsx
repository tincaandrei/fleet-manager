import { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { deleteVehicle, getVehicle } from '../api/vehicleApi';
import type { Vehicle } from '../types/vehicle';
import { useAuth } from '../auth/useAuth';
import VehicleDocumentsSection from '../components/VehicleDocumentsSection';
import ComplianceSection from '../components/ComplianceSection';
import PageShell from '../components/ui/PageShell';
import DataState from '../components/ui/DataState';
import StatusBadge from '../components/ui/StatusBadge';
import { getApiErrorMessage } from '../utils/apiError';

type Tab = 'details' | 'documents' | 'compliance';

export default function VehicleDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const [vehicle, setVehicle] = useState<Vehicle | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('details');

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setLoading(true);
      setError(null);
      getVehicle(Number(id))
        .then((res) => setVehicle(res.data))
        .catch((err: unknown) => setError(getApiErrorMessage(err, 'Failed to load vehicle.')))
        .finally(() => setLoading(false));
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [id]);

  const handleDelete = async () => {
    if (!window.confirm('Delete this vehicle? This cannot be undone.')) return;
    try {
      await deleteVehicle(Number(id));
      navigate('/vehicles');
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to delete vehicle.'));
    }
  };

  return (
    <PageShell>
        <div className="page-header">
          <h1>Vehicle Details</h1>
          <div className="actions-row">
            <Link to="/vehicles">← Back</Link>
            {isAdmin && vehicle && (
              <>
                {' '}
                <Link to={`/vehicles/${vehicle.id}/edit`} className="btn">Edit</Link>
                {' '}
                <button className="btn btn-danger" onClick={handleDelete}>Delete</button>
              </>
            )}
          </div>
        </div>

        {loading && <DataState type="loading">Loading vehicle...</DataState>}
        {error && <DataState type="error">{error}</DataState>}
        {vehicle && (
          <>
            <section className="detail-summary" aria-label="Vehicle summary">
              <div className="detail-hero">
                <h2>{vehicle.brand} {vehicle.model}</h2>
                <StatusBadge status={vehicle.status} />
                <div className="detail-meta">
                  <span className="meta-chip">{vehicle.vehicleType}</span>
                  <span className="meta-chip">{vehicle.fuelType}</span>
                  <span className="meta-chip">{vehicle.ownershipType}</span>
                  <span className="meta-chip">{vehicle.currentMileage.toLocaleString()} km</span>
                </div>
              </div>
              <aside className="detail-aside">
                <div className="detail-aside-row"><span>Department</span><strong>{vehicle.department || '-'}</strong></div>
                <div className="detail-aside-row"><span>Driver</span><strong>{vehicle.assignedDriverName || '-'}</strong></div>
                <div className="detail-aside-row"><span>Organization</span><strong>{vehicle.businessId ?? '-'}</strong></div>
              </aside>
            </section>

            <nav className="vehicle-tabs">
              <button
                className={`vehicle-tab${activeTab === 'details' ? ' vehicle-tab--active' : ''}`}
                onClick={() => setActiveTab('details')}
              >
                Details
              </button>
              <button
                className={`vehicle-tab${activeTab === 'documents' ? ' vehicle-tab--active' : ''}`}
                onClick={() => setActiveTab('documents')}
              >
                Documents
              </button>
              <button
                className={`vehicle-tab${activeTab === 'compliance' ? ' vehicle-tab--active' : ''}`}
                onClick={() => setActiveTab('compliance')}
              >
                Compliance
              </button>
            </nav>

            {activeTab === 'details' && (
              <table className="detail-table">
                <tbody>
                  <tr><th>ID</th><td>{vehicle.id}</td></tr>
                  <tr><th>License Plate</th><td>{vehicle.licensePlate}</td></tr>
                  <tr><th>VIN</th><td>{vehicle.vin}</td></tr>
                  <tr><th>Brand</th><td>{vehicle.brand}</td></tr>
                  <tr><th>Model</th><td>{vehicle.model}</td></tr>
                  <tr><th>Year</th><td>{vehicle.manufactureYear}</td></tr>
                  <tr><th>Type</th><td>{vehicle.vehicleType}</td></tr>
                  <tr><th>Fuel</th><td>{vehicle.fuelType}</td></tr>
                  <tr><th>Ownership</th><td>{vehicle.ownershipType}</td></tr>
                  <tr><th>Status</th><td><StatusBadge status={vehicle.status} /></td></tr>
                  <tr><th>Department</th><td>{vehicle.department}</td></tr>
                  <tr><th>Assigned Driver</th><td>{vehicle.assignedDriverName || '—'}</td></tr>
                  <tr><th>Assigned User ID</th><td>{vehicle.assignedUserId ?? '—'}</td></tr>
                  <tr><th>Mileage</th><td>{vehicle.currentMileage.toLocaleString()} km</td></tr>
                  {vehicle.businessId != null && (
                    <tr><th>Organization ID</th><td>{vehicle.businessId}</td></tr>
                  )}
                  <tr><th>Created</th><td>{new Date(vehicle.createdAt).toLocaleString()}</td></tr>
                  <tr><th>Updated</th><td>{new Date(vehicle.updatedAt).toLocaleString()}</td></tr>
                </tbody>
              </table>
            )}

            {activeTab === 'documents' && (
              <VehicleDocumentsSection vehicleId={vehicle.id} />
            )}

            {activeTab === 'compliance' && (
              <ComplianceSection vehicleId={vehicle.id} />
            )}
          </>
        )}
    </PageShell>
  );
}
