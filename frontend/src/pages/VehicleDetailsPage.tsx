import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getVehicle, deleteVehicle } from '../api/vehicleApi';
import type { Vehicle } from '../types/vehicle';
import Navbar from '../components/Navbar';
import { useAuth } from '../auth/AuthContext';
import VehicleDocumentsSection from '../components/VehicleDocumentsSection';

type Tab = 'details' | 'documents';

export default function VehicleDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const [vehicle, setVehicle] = useState<Vehicle | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('details');

  useEffect(() => {
    getVehicle(Number(id))
      .then((res) => setVehicle(res.data))
      .catch((err) => {
        if (err.response?.status === 403) setError('Access denied.');
        else setError(err.response?.data?.message ?? 'Failed to load vehicle.');
      });
  }, [id]);

  const handleDelete = async () => {
    if (!window.confirm('Delete this vehicle? This cannot be undone.')) return;
    try {
      await deleteVehicle(Number(id));
      navigate('/vehicles');
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

        {error && <p className="error">{error}</p>}
        {vehicle && (
          <>
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
                  <tr><th>Status</th><td><span className={`status-badge status-${vehicle.status}`}>{vehicle.status}</span></td></tr>
                  <tr><th>Department</th><td>{vehicle.department}</td></tr>
                  <tr><th>Assigned Driver</th><td>{vehicle.assignedDriverName || '—'}</td></tr>
                  <tr><th>Assigned User ID</th><td>{vehicle.assignedUserId ?? '—'}</td></tr>
                  <tr><th>Mileage</th><td>{vehicle.currentMileage.toLocaleString()} km</td></tr>
                  <tr><th>Created</th><td>{new Date(vehicle.createdAt).toLocaleString()}</td></tr>
                  <tr><th>Updated</th><td>{new Date(vehicle.updatedAt).toLocaleString()}</td></tr>
                </tbody>
              </table>
            )}

            {activeTab === 'documents' && (
              <VehicleDocumentsSection vehicleId={vehicle.id} />
            )}
          </>
        )}
      </main>
    </>
  );
}
