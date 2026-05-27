import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getVehicle, updateVehicle } from '../api/vehicleApi';
import type { Vehicle, VehicleRequest } from '../types/vehicle';
import VehicleForm from '../components/VehicleForm';
import Navbar from '../components/Navbar';

export default function VehicleEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [vehicle, setVehicle] = useState<Vehicle | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getVehicle(Number(id))
      .then((res) => setVehicle(res.data))
      .catch((err: unknown) => {
        const e = err as { response?: { data?: { message?: string } } };
        setLoadError(e.response?.data?.message ?? 'Failed to load vehicle.');
      });
  }, [id]);

  const handleSubmit = async (data: VehicleRequest) => {
    setLoading(true);
    setSubmitError(null);
    try {
      await updateVehicle(Number(id), data);
      navigate(`/vehicles/${id}`);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setSubmitError(e.response?.data?.message ?? 'Failed to update vehicle.');
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
