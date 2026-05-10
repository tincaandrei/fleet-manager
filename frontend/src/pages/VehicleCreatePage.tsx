import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createVehicle } from '../api/vehicleApi';
import type { VehicleRequest } from '../types/vehicle';
import VehicleForm from '../components/VehicleForm';
import Navbar from '../components/Navbar';
import { useAuth } from '../auth/AuthContext';

export default function VehicleCreatePage() {
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isAdmin) {
    return (
      <>
        <Navbar />
        <main className="page">
          <p className="error">Access denied. Only admins can create vehicles.</p>
        </main>
      </>
    );
  }

  const handleSubmit = async (data: VehicleRequest) => {
    setLoading(true);
    setError(null);
    try {
      const res = await createVehicle(data);
      navigate(`/vehicles/${res.data.id}`);
    } catch (err: any) {
      if (err.response?.status === 403) setError('Access denied.');
      else setError(err.response?.data?.message ?? 'Failed to create vehicle.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Navbar />
      <main className="page">
        <div className="page-header">
          <h1>New Vehicle</h1>
        </div>
        <VehicleForm onSubmit={handleSubmit} loading={loading} error={error} submitLabel="Create Vehicle" />
      </main>
    </>
  );
}
