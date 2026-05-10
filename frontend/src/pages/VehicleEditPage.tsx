import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getVehicle, updateVehicle } from '../api/vehicleApi';
import type { Vehicle, VehicleRequest } from '../types/vehicle';
import VehicleForm from '../components/VehicleForm';
import Navbar from '../components/Navbar';
import { useAuth } from '../auth/AuthContext';

export default function VehicleEditPage() {
  const { id } = useParams<{ id: string }>();
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const [vehicle, setVehicle] = useState<Vehicle | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isAdmin) return;
    getVehicle(Number(id))
      .then((res) => setVehicle(res.data))
      .catch((err) => setLoadError(err.response?.data?.message ?? 'Failed to load vehicle.'));
  }, [id, isAdmin]);

  if (!isAdmin) {
    return (
      <>
        <Navbar />
        <main className="page">
          <p className="error">Access denied. Only admins can edit vehicles.</p>
        </main>
      </>
    );
  }

  const handleSubmit = async (data: VehicleRequest) => {
    setLoading(true);
    setSubmitError(null);
    try {
      await updateVehicle(Number(id), data);
      navigate(`/vehicles/${id}`);
    } catch (err: any) {
      if (err.response?.status === 403) setSubmitError('Access denied.');
      else setSubmitError(err.response?.data?.message ?? 'Failed to update vehicle.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Navbar />
      <main className="page">
        <div className="page-header">
          <h1>Edit Vehicle</h1>
        </div>
        {loadError && <p className="error">{loadError}</p>}
        {vehicle && (
          <VehicleForm
            initial={vehicle}
            onSubmit={handleSubmit}
            loading={loading}
            error={submitError}
            submitLabel="Save Changes"
          />
        )}
      </main>
    </>
  );
}
