import { useState } from 'react';
import type { Vehicle, VehicleRequest } from '../types/vehicle';
import VehicleImage from './VehicleImage';

const STATUSES = ['ACTIVE', 'IN_SERVICE', 'INACTIVE', 'SOLD', 'DECOMMISSIONED'];
const TYPES = ['CAR', 'VAN', 'TRUCK', 'MOTORCYCLE', 'OTHER'];
const FUELS = ['DIESEL', 'PETROL', 'HYBRID', 'ELECTRIC', 'LPG', 'OTHER'];
const OWNERSHIPS = ['OWNED', 'LEASED', 'RENTED', 'OTHER'];

interface OrganizationOption {
  id: number;
  name: string;
  active: boolean;
}

interface Props {
  initial?: Partial<VehicleRequest>;
  onSubmit: (data: VehicleRequest) => void;
  loading: boolean;
  error: string | null;
  submitLabel: string;
  imageFile?: File | null;
  currentVehicle?: Vehicle | null;
  onImageFileChange?: (file: File | null) => void;
  /** When provided (SUPERADMIN create flow), shows an organization selector and sends businessId. */
  organizations?: OrganizationOption[];
}

export default function VehicleForm({
  initial,
  onSubmit,
  loading,
  error,
  submitLabel,
  imageFile,
  currentVehicle,
  onImageFileChange,
  organizations,
}: Props) {
  const [form, setForm] = useState<VehicleRequest>({
    businessId: initial?.businessId,
    licensePlate: initial?.licensePlate ?? '',
    vin: initial?.vin ?? '',
    brand: initial?.brand ?? '',
    model: initial?.model ?? '',
    manufactureYear: initial?.manufactureYear ?? new Date().getFullYear(),
    vehicleType: initial?.vehicleType ?? 'CAR',
    fuelType: initial?.fuelType ?? 'PETROL',
    ownershipType: initial?.ownershipType ?? 'OWNED',
    status: initial?.status ?? 'ACTIVE',
    department: initial?.department ?? '',
    currentMileage: initial?.currentMileage ?? 0,
  });

  const handleChange = (field: keyof VehicleRequest, value: string) => {
    setForm((prev) => ({
      ...prev,
      [field]:
        field === 'manufactureYear' || field === 'currentMileage'
          ? Number(value)
          : value,
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(form);
  };

  return (
    <form onSubmit={handleSubmit} className="vehicle-form">
      {error && <p className="error full-width">{error}</p>}

      {organizations && (
        <label className="full-width">
          Organization
          <select
            value={form.businessId ?? ''}
            onChange={(e) =>
              setForm((prev) => ({
                ...prev,
                businessId: e.target.value === '' ? undefined : Number(e.target.value),
              }))
            }
            required
          >
            <option value="" disabled>Select an organization...</option>
            {organizations.map((org) => (
              <option key={org.id} value={org.id} disabled={!org.active}>
                {org.name}{org.active ? '' : ' (inactive)'}
              </option>
            ))}
          </select>
        </label>
      )}

      <label>
        License Plate
        <input value={form.licensePlate} onChange={(e) => handleChange('licensePlate', e.target.value)} required />
      </label>
      <label>
        VIN
        <input value={form.vin} onChange={(e) => handleChange('vin', e.target.value)} required />
      </label>
      <label>
        Brand
        <input value={form.brand} onChange={(e) => handleChange('brand', e.target.value)} required />
      </label>
      <label>
        Model
        <input value={form.model} onChange={(e) => handleChange('model', e.target.value)} required />
      </label>
      <label>
        Manufacture Year
        <input type="number" value={form.manufactureYear} onChange={(e) => handleChange('manufactureYear', e.target.value)} required />
      </label>
      <label>
        Current Mileage
        <input type="number" value={form.currentMileage} onChange={(e) => handleChange('currentMileage', e.target.value)} required />
      </label>
      <label>
        Vehicle Type
        <select value={form.vehicleType} onChange={(e) => handleChange('vehicleType', e.target.value)}>
          {TYPES.map((t) => <option key={t}>{t}</option>)}
        </select>
      </label>
      <label>
        Fuel Type
        <select value={form.fuelType} onChange={(e) => handleChange('fuelType', e.target.value)}>
          {FUELS.map((f) => <option key={f}>{f}</option>)}
        </select>
      </label>
      <label>
        Ownership Type
        <select value={form.ownershipType} onChange={(e) => handleChange('ownershipType', e.target.value)}>
          {OWNERSHIPS.map((o) => <option key={o}>{o}</option>)}
        </select>
      </label>
      <label>
        Status
        <select value={form.status} onChange={(e) => handleChange('status', e.target.value)}>
          {STATUSES.map((s) => <option key={s}>{s}</option>)}
        </select>
      </label>
      <label>
        Department
        <input value={form.department} onChange={(e) => handleChange('department', e.target.value)} />
      </label>
      {onImageFileChange && (
        <div className="vehicle-image-field full-width">
          {currentVehicle?.imageUrl && (
            <VehicleImage vehicle={currentVehicle} className="vehicle-form-image" />
          )}
          <label>
            Vehicle image
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(e) => onImageFileChange(e.target.files?.[0] ?? null)}
            />
          </label>
          {imageFile && (
            <span className="vehicle-image-file-name">{imageFile.name}</span>
          )}
        </div>
      )}

      <div className="full-width">
        <button type="submit" disabled={loading}>{loading ? 'Saving...' : submitLabel}</button>
      </div>
    </form>
  );
}
