import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { createVehicle, uploadVehicleImage } from '../api/vehicleApi';
import { listBusinesses } from '../api/authApi';
import type { VehicleRequest } from '../types/vehicle';
import type { Business } from '../types/auth';
import { useAuth } from '../auth/useAuth';
import VehicleForm from '../components/VehicleForm';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import DataState from '../components/ui/DataState';
import { getApiErrorMessage } from '../utils/apiError';

export default function VehicleCreatePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isSuperAdmin } = useAuth();

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [businesses, setBusinesses] = useState<Business[] | null>(null);
  const [businessesError, setBusinessesError] = useState<string | null>(null);

  // SUPERADMIN picks the target organization; default comes from ?businessId= (console selection).
  const defaultBusinessId = Number(searchParams.get('businessId')) || undefined;

  useEffect(() => {
    if (!isSuperAdmin) return;
    const timeoutId = window.setTimeout(() => {
      listBusinesses()
        .then((res) => setBusinesses(res.data))
        .catch((err: unknown) =>
          setBusinessesError(getApiErrorMessage(err, 'Failed to load organizations.')));
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [isSuperAdmin]);

  const handleSubmit = async (data: VehicleRequest) => {
    if (isSuperAdmin && !data.businessId) {
      setError('Select an organization for the new vehicle.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const payload: VehicleRequest = isSuperAdmin
        ? data
        : { ...data, businessId: undefined };
      const res = await createVehicle(payload);
      if (imageFile) {
        await uploadVehicleImage(res.data.id, imageFile);
      }
      navigate(`/vehicles/${res.data.id}`);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to create vehicle.'));
    } finally {
      setLoading(false);
    }
  };

  const waitingForBusinesses = isSuperAdmin && businesses === null && !businessesError;

  return (
    <PageShell>
        <PageHeader
          title="New Vehicle"
          description="Create a vehicle record with assignment and operating details."
        />
        {businessesError && <DataState type="error">{businessesError}</DataState>}
        {waitingForBusinesses && <DataState type="loading">Loading organizations...</DataState>}

        {!waitingForBusinesses && !businessesError && (
          <VehicleForm
            onSubmit={handleSubmit}
            loading={loading}
            error={error}
            submitLabel="Create Vehicle"
            imageFile={imageFile}
            onImageFileChange={setImageFile}
            initial={{ businessId: defaultBusinessId }}
            organizations={isSuperAdmin && businesses ? businesses : undefined}
          />
        )}
    </PageShell>
  );
}
