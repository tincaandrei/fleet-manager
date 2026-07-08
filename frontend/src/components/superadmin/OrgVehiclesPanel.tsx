import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
import { ButtonLink } from '../ui/Button';
import Pagination from '../ui/Pagination';
import { usePagedList } from '../ui/usePagedList';
import RowActionMenu from '../ui/RowActionMenu';
import type { RowAction } from '../ui/RowActionMenu';
import { getApiErrorMessage } from '../../utils/apiError';
import { showToast } from '../../utils/toast';

interface VehicleRowMenuProps {
  vehicle: Vehicle;
  busy: boolean;
  onEdit: (vehicleId: number) => void;
  onPickImage: (vehicleId: number) => void;
  onDeleteImage: (vehicleId: number) => void;
  onDelete: (vehicleId: number) => void;
}

function VehicleRowMenu({ vehicle, busy, onEdit, onPickImage, onDeleteImage, onDelete }: VehicleRowMenuProps) {
  const actions: RowAction[] = [
    { label: 'Edit vehicle', onSelect: () => onEdit(vehicle.id) },
    {
      label: vehicle.imageUrl ? 'Replace image' : 'Upload image',
      onSelect: () => onPickImage(vehicle.id),
    },
  ];
  if (vehicle.imageUrl) {
    actions.push({ label: 'Remove image', onSelect: () => onDeleteImage(vehicle.id) });
  }
  actions.push({ label: 'Delete vehicle', danger: true, onSelect: () => onDelete(vehicle.id) });

  return (
    <div className="row-actions row-actions--menu">
      {busy && <span className="row-busy" aria-live="polite">Working...</span>}
      <RowActionMenu
        actions={actions}
        label={`Actions for ${vehicle.licensePlate}`}
        disabled={busy}
      />
    </div>
  );
}

interface OrgVehiclesPanelProps {
  businessId: number;
}

/**
 * Vehicle management for one organization inside the superadmin console:
 * list, edit/delete, image upload/delete, new vehicle shortcut.
 */
export default function OrgVehiclesPanel({ businessId }: OrgVehiclesPanelProps) {
  const navigate = useNavigate();
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyVehicleId, setBusyVehicleId] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const uploadTargetRef = useRef<number | null>(null);

  const { page, pageCount, pageItems, setPage } = usePagedList(vehicles);

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

  const handleEditVehicle = (vehicleId: number) => {
    navigate(`/vehicles/${vehicleId}/edit`);
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
        <span className="panel-toolbar-summary">
          {loading ? 'Loading...' : `${vehicles.length} vehicle${vehicles.length === 1 ? '' : 's'}`}
        </span>
        <ButtonLink to={`/vehicles/new?businessId=${businessId}`} size="sm">
          New vehicle
        </ButtonLink>
      </div>

      {loading && <DataState type="loading">Loading vehicles...</DataState>}
      {!loading && error && <DataState type="error">{error}</DataState>}
      {!loading && !error && vehicles.length === 0 && (
        <DataState>No vehicles in this organization yet. Create the first one.</DataState>
      )}

      {!loading && !error && vehicles.length > 0 && (
        <>
          <ResponsiveTable ariaLabel="Organization vehicles">
            <thead>
              <tr>
                <th>Vehicle</th>
                <th>Status</th>
                <th>Mileage</th>
                <th>Driver</th>
                <th>Image</th>
                <th className="actions-col" aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {pageItems.map((vehicle) => {
                const busy = busyVehicleId === vehicle.id;
                return (
                  <tr key={vehicle.id}>
                    <td data-label="Vehicle">
                      <Link to={`/vehicles/${vehicle.id}`} className="cell-primary">
                        {vehicle.licensePlate}
                      </Link>
                      <span className="cell-secondary">{vehicle.brand} {vehicle.model}</span>
                    </td>
                    <td data-label="Status"><StatusBadge status={vehicle.status} /></td>
                    <td data-label="Mileage">{vehicle.currentMileage.toLocaleString()} km</td>
                    <td data-label="Driver">{vehicle.assignedDriverName || '-'}</td>
                    <td data-label="Image">{vehicle.imageUrl ? 'Yes' : '-'}</td>
                    <td data-label="Actions" className="actions-col">
                      <VehicleRowMenu
                        vehicle={vehicle}
                        busy={busy}
                        onEdit={handleEditVehicle}
                        onPickImage={handlePickImage}
                        onDeleteImage={handleDeleteImage}
                        onDelete={handleDeleteVehicle}
                      />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </ResponsiveTable>
          <Pagination
            page={page}
            pageCount={pageCount}
            onPageChange={setPage}
            summary={`${vehicles.length} vehicles`}
          />
        </>
      )}
    </div>
  );
}
