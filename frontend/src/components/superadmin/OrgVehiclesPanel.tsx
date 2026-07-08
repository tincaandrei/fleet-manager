import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  deleteVehicle,
  deleteVehicleImage,
  getVehicles,
  uploadVehicleImage,
} from '../../api/vehicleApi';
import type { Vehicle } from '../../types/vehicle';
import DataState from '../ui/DataState';
import ResponsiveTable from '../ui/ResponsiveTable';
import StatusBadge from '../ui/StatusBadge';
import { Button, ButtonLink } from '../ui/Button';
import { getApiErrorMessage } from '../../utils/apiError';
import { showToast } from '../../utils/toast';

interface OrgVehiclesPanelProps {
  businessId: number;
}

/**
 * Vehicle management for one organization inside the superadmin console:
 * list, edit/delete, image upload/delete, new vehicle shortcut.
 */
export default function OrgVehiclesPanel({ businessId }: OrgVehiclesPanelProps) {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyVehicleId, setBusyVehicleId] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const uploadTargetRef = useRef<number | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    getVehicles({ businessId })
      .then((res) => setVehicles(res.data))
      .catch((err: unknown) => setError(getApiErrorMessage(err, 'Failed to load vehicles.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timeoutId = window.setTimeout(() => load(), 0);
    return () => window.clearTimeout(timeoutId);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [businessId]);

  const replaceVehicle = (updated: Vehicle) => {
    setVehicles((prev) => prev.map((v) => (v.id === updated.id ? updated : v)));
  };

  const handlePickImage = (vehicleId: number) => {
    uploadTargetRef.current = vehicleId;
    fileInputRef.current?.click();
  };

  const handleImageSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    e.target.value = '';
    const vehicleId = uploadTargetRef.current;
    uploadTargetRef.current = null;
    if (!file || vehicleId == null) return;

    setBusyVehicleId(vehicleId);
    try {
      const res = await uploadVehicleImage(vehicleId, file);
      replaceVehicle(res.data);
      showToast({ type: 'success', message: 'Vehicle image uploaded.' });
    } catch {
      // Toast shown by interceptor.
    } finally {
      setBusyVehicleId(null);
    }
  };

  const handleDeleteImage = async (vehicleId: number) => {
    setBusyVehicleId(vehicleId);
    try {
      const res = await deleteVehicleImage(vehicleId);
      replaceVehicle(res.data);
      showToast({ type: 'success', message: 'Vehicle image removed.' });
    } catch {
      // Toast shown by interceptor.
    } finally {
      setBusyVehicleId(null);
    }
  };

  const handleDeleteVehicle = async (vehicleId: number) => {
    if (!window.confirm('Delete this vehicle? This cannot be undone.')) return;
    setBusyVehicleId(vehicleId);
    try {
      await deleteVehicle(vehicleId);
      setVehicles((prev) => prev.filter((v) => v.id !== vehicleId));
      showToast({ type: 'success', message: 'Vehicle deleted.' });
    } catch {
      // Toast shown by interceptor.
    } finally {
      setBusyVehicleId(null);
    }
  };

  return (
    <div className="org-vehicles-panel">
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        style={{ display: 'none' }}
        onChange={handleImageSelected}
        aria-hidden="true"
        tabIndex={-1}
      />

      <div className="panel-toolbar">
        <ButtonLink to={`/vehicles/new?businessId=${businessId}`} size="sm">
          + New Vehicle
        </ButtonLink>
      </div>

      {loading && <DataState type="loading">Loading vehicles...</DataState>}
      {!loading && error && <DataState type="error">{error}</DataState>}
      {!loading && !error && vehicles.length === 0 && (
        <DataState>No vehicles in this organization yet. Create the first one.</DataState>
      )}

      {!loading && !error && vehicles.length > 0 && (
        <ResponsiveTable ariaLabel="Organization vehicles">
          <thead>
            <tr>
              <th>Plate</th>
              <th>Vehicle</th>
              <th>Status</th>
              <th>Mileage</th>
              <th>Driver</th>
              <th>Image</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {vehicles.map((vehicle) => {
              const busy = busyVehicleId === vehicle.id;
              return (
                <tr key={vehicle.id}>
                  <td data-label="Plate">
                    <Link to={`/vehicles/${vehicle.id}`}>{vehicle.licensePlate}</Link>
                  </td>
                  <td data-label="Vehicle">{vehicle.brand} {vehicle.model}</td>
                  <td data-label="Status"><StatusBadge status={vehicle.status} /></td>
                  <td data-label="Mileage">{vehicle.currentMileage.toLocaleString()} km</td>
                  <td data-label="Driver">{vehicle.assignedDriverName || '-'}</td>
                  <td data-label="Image">
                    <div className="row-actions">
                      <Button
                        size="sm"
                        variant="secondary"
                        disabled={busy}
                        onClick={() => handlePickImage(vehicle.id)}
                      >
                        {busy ? 'Working...' : vehicle.imageUrl ? 'Replace' : 'Upload'}
                      </Button>
                      {vehicle.imageUrl && (
                        <Button
                          size="sm"
                          variant="danger"
                          disabled={busy}
                          onClick={() => handleDeleteImage(vehicle.id)}
                        >
                          Remove
                        </Button>
                      )}
                    </div>
                  </td>
                  <td data-label="Actions">
                    <div className="row-actions">
                      <ButtonLink to={`/vehicles/${vehicle.id}/edit`} size="sm" variant="ghost">
                        Edit
                      </ButtonLink>
                      <Button
                        size="sm"
                        variant="danger"
                        disabled={busy}
                        onClick={() => handleDeleteVehicle(vehicle.id)}
                      >
                        Delete
                      </Button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </ResponsiveTable>
      )}
    </div>
  );
}
