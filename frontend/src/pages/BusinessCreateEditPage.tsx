import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getBusiness } from '../api/authApi';
import type { Business } from '../types/auth';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import { ButtonLink } from '../components/ui/Button';
import DataState from '../components/ui/DataState';
import OrganizationForm from '../components/superadmin/OrganizationForm';
import { getApiErrorMessage } from '../utils/apiError';

export default function BusinessCreateEditPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [business, setBusiness] = useState<Business | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loadingBusiness, setLoadingBusiness] = useState(isEdit);

  useEffect(() => {
    if (!isEdit || !id) return;
    const timeoutId = window.setTimeout(() => {
      setLoadError(null);
      setLoadingBusiness(true);
      getBusiness(Number(id))
        .then((res) => setBusiness(res.data))
        .catch((err: unknown) => setLoadError(getApiErrorMessage(err, 'Failed to load organization.')))
        .finally(() => setLoadingBusiness(false));
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [id, isEdit]);

  const handleSaved = (saved: Business) => {
    if (isEdit) {
      navigate('/superadmin');
    } else {
      navigate(`/superadmin?org=${saved.id}&tab=users`);
    }
  };

  return (
    <PageShell>
        <PageHeader
          title={isEdit ? 'Edit Organization' : 'New Organization'}
          description="Keep organization details accurate for fleet access and assignment."
          actions={<ButtonLink to="/superadmin" variant="secondary">Back to Console</ButtonLink>}
        />

        {loadError && <DataState type="error">{loadError}</DataState>}
        {loadingBusiness && !loadError && <DataState type="loading">Loading organization...</DataState>}

        {!loadingBusiness && !loadError && (
          <OrganizationForm business={business} onSaved={handleSaved} />
        )}
    </PageShell>
  );
}
