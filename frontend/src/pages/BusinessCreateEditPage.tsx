import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getBusiness, createBusiness, updateBusiness } from '../api/authApi';
import type { BusinessRequest } from '../types/auth';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import { ButtonLink } from '../components/ui/Button';
import DataState from '../components/ui/DataState';

function apiMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

export default function BusinessCreateEditPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [form, setForm] = useState<BusinessRequest>({
    name: '',
    registrationNumber: '',
    contactEmail: '',
    phone: '',
    address: '',
    active: true,
  });
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!isEdit || !id) return;
    const timeoutId = window.setTimeout(() => {
      setLoadError(null);
      getBusiness(Number(id))
        .then((res) => {
          setForm({
            name: res.data.name ?? '',
            registrationNumber: res.data.registrationNumber ?? '',
            contactEmail: res.data.contactEmail ?? '',
            phone: res.data.phone ?? '',
            address: res.data.address ?? '',
            active: res.data.active,
          });
        })
        .catch((err: unknown) => setLoadError(apiMessage(err, 'Failed to load organization.')));
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [id, isEdit]);

  const setField = (field: keyof BusinessRequest, value: string | boolean) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const cleanOptional = (value: string | null | undefined) => {
    const trimmed = value?.trim();
    return trimmed ? trimmed : null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) {
      setSubmitError('Organization name is required.');
      return;
    }
    const payload: BusinessRequest = {
      name: form.name.trim(),
      registrationNumber: cleanOptional(form.registrationNumber),
      contactEmail: cleanOptional(form.contactEmail),
      phone: cleanOptional(form.phone),
      address: cleanOptional(form.address),
      active: form.active,
    };
    setLoading(true);
    setSubmitError(null);
    try {
      if (isEdit && id) {
        await updateBusiness(Number(id), payload);
        navigate('/businesses');
      } else {
        const res = await createBusiness(payload);
        navigate(`/businesses/${res.data.id}/users`);
      }
    } catch (err: unknown) {
      setSubmitError(apiMessage(err, 'Failed to save organization.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageShell>
        <PageHeader
          title={isEdit ? 'Edit Organization' : 'New Organization'}
          description="Keep organization details accurate for fleet access and assignment."
          actions={<ButtonLink to="/businesses" variant="secondary">Back to Organizations</ButtonLink>}
        />

        {loadError && <DataState type="error">{loadError}</DataState>}

        <form onSubmit={handleSubmit} className="auth-form management-form">
          {submitError && <DataState type="error">{submitError}</DataState>}

          <label>
            Organization Name
            <input
              value={form.name}
              onChange={(e) => setField('name', e.target.value)}
              required
              autoFocus
              placeholder="e.g. Acme Transport SRL"
            />
          </label>

          <label>
            Registration Number
            <input
              value={form.registrationNumber ?? ''}
              onChange={(e) => setField('registrationNumber', e.target.value)}
              placeholder="e.g. RO12345678"
            />
          </label>

          <label>
            Contact Email
            <input
              type="email"
              value={form.contactEmail ?? ''}
              onChange={(e) => setField('contactEmail', e.target.value)}
              placeholder="office@example.com"
            />
          </label>

          <label>
            Phone
            <input
              value={form.phone ?? ''}
              onChange={(e) => setField('phone', e.target.value)}
              placeholder="+407..."
            />
          </label>

          <label>
            Address
            <input
              value={form.address ?? ''}
              onChange={(e) => setField('address', e.target.value)}
              placeholder="City, street, number"
            />
          </label>

          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={form.active ?? true}
              onChange={(e) => setField('active', e.target.checked)}
            />
            Active organization
          </label>

          <button type="submit" disabled={loading}>
            {loading ? 'Saving...' : isEdit ? 'Save Changes' : 'Create Organization'}
          </button>
        </form>
    </PageShell>
  );
}
