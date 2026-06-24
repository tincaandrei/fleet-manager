import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getVehicle, updateVehicle, uploadVehicleImage } from '../api/vehicleApi';
import type { Vehicle, VehicleRequest } from '../types/vehicle';
import VehicleForm from '../components/VehicleForm';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import DataState from '../components/ui/DataState';
import { getApiErrorMessage } from '../utils/apiError';

export default function VehicleEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [vehicle, setVehicle] = useState<Vehicle | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      getVehicle(Number(id))
        .then((res) => setVehicle(res.data))
        .catch((err: unknown) => setLoadError(getApiErrorMessage(err, 'Failed to load vehicle.')));
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [id]);

  const handleSubmit = async (data: VehicleRequest) => {
    setLoading(true);
    setSubmitError(null);
    try {
      await updateVehicle(Number(id), data);
      if (imageFile) {
        await uploadVehicleImage(Number(id), imageFile);
      }
      navigate(`/vehicles/${id}`);
    } catch (err: unknown) {
      setSubmitError(getApiErrorMessage(err, 'Failed to update vehicle.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageShell>
        <PageHeader
          title="Edit Vehicle"
          description={vehicle ? `${vehicle.licensePlate} - ${vehicle.brand} ${vehicle.model}` : 'Update vehicle details.'}
        />
        {loadError && <DataState type="error">{loadError}</DataState>}
        {vehicle && (
          <VehicleForm
            initial={vehicle}
            onSubmit={handleSubmit}
            loading={loading}
            error={submitError}
            submitLabel="Save Changes"
            currentVehicle={vehicle}
            imageFile={imageFile}
            onImageFileChange={setImageFile}
          />
        )}
    </PageShell>
  );
}
