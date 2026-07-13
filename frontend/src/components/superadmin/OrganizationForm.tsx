import { useState } from 'react';
import { createBusiness, updateBusiness } from '../../api/authApi';
import type { Business, BusinessRequest } from '../../types/auth';
import DataState from '../ui/DataState';
import { Button } from '../ui/Button';
import { getApiErrorMessage } from '../../utils/apiError';

interface OrganizationFormProps {
  /**
   * When set, the form edits this organization; otherwise it creates a new one.
   * Pass a `key` (e.g. the business id) when switching between organizations so
   * the form state re-initialises.
   */
  business?: Business | null;
  onSaved: (saved: Business) => void;
  onCancel?: () => void;
}

function toFormState(business?: Business | null): BusinessRequest {
  return {
    name: business?.name ?? '',
    registrationNumber: business?.registrationNumber ?? '',
    contactEmail: business?.contactEmail ?? '',
    phone: business?.phone ?? '',
    address: business?.address ?? '',
    active: business?.active ?? true,
  };
}

export default function OrganizationForm({ business, onSaved, onCancel }: OrganizationFormProps) {
  const isEdit = Boolean(business);
  const [form, setForm] = useState<BusinessRequest>(() => toFormState(business));
  const [loading, setLoading] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

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
      const res = business
        ? await updateBusiness(business.id, payload)
        : await createBusiness(payload);
      onSaved(res.data);
    } catch (err: unknown) {
      setSubmitError(getApiErrorMessage(err, 'Failed to save organization.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="auth-form management-form">
      {submitError && <DataState type="error">{submitError}</DataState>}

      <label>
        Organization Name
        <input
          value={form.name}
          onChange={(e) => setField('name', e.target.value)}
          required
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

      <div className="form-actions-row">
        <button type="submit" disabled={loading}>
          {loading ? 'Saving...' : isEdit ? 'Save Changes' : 'Create Organization'}
        </button>
        {onCancel && (
          <Button variant="secondary" onClick={onCancel} disabled={loading}>
            Cancel
          </Button>
        )}
      </div>
    </form>
  );
}
