import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createVehicle } from '../api/vehicleApi';
import type { VehicleRequest } from '../types/vehicle';
import VehicleForm from '../components/VehicleForm';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import { getApiErrorMessage } from '../utils/apiError';

export default function VehicleCreatePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (data: VehicleRequest) => {
    setLoading(true);
    setError(null);
    try {
      const res = await createVehicle(data);
      navigate(`/vehicles/${res.data.id}`);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to create vehicle.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageShell>
        <PageHeader
          title="New Vehicle"
          description="Create a vehicle record with assignment and operating details."
        />
        <VehicleForm onSubmit={handleSubmit} loading={loading} error={error} submitLabel="Create Vehicle" />
    </PageShell>
  );
}
