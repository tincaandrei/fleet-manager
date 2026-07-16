import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { listBusinessUsers } from '../api/authApi';
import { assignVehicleDriver } from '../api/vehicleApi';
import type { BusinessUser } from '../types/auth';
import type { Vehicle } from '../types/vehicle';
import { getApiErrorMessage } from '../utils/apiError';
import { businessUserDisplayName } from '../utils/businessUserDisplay';
import { showToast } from '../utils/toast';
import { Button } from './ui/Button';

interface AssignDriverModalProps {
  vehicle: Vehicle;
  onClose: () => void;
  onAssigned: (vehicle: Vehicle) => void;
}

export default function AssignDriverModal({ vehicle, onClose, onAssigned }: AssignDriverModalProps) {
  const missingBusiness = vehicle.businessId == null;
  const [users, setUsers] = useState<BusinessUser[]>([]);
  const [selectedUserId, setSelectedUserId] = useState(
    vehicle.assignedUserId == null ? '' : String(vehicle.assignedUserId),
  );
  const [loadingUsers, setLoadingUsers] = useState(!missingBusiness);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(
    missingBusiness ? 'This vehicle is not assigned to an organization.' : null,
  );

  const drivers = useMemo(
    () => users.filter((user) => user.role === 'EMPLOYEE' && (user.status ?? 'ACTIVE') === 'ACTIVE'),
    [users],
  );

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !saving) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose, saving]);

  useEffect(() => {
    if (vehicle.businessId == null) return;

    let active = true;
    listBusinessUsers(vehicle.businessId)
      .then((response) => {
        if (!active) return;
        setUsers(response.data);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(getApiErrorMessage(requestError, 'Failed to load organization drivers.'));
      })
      .finally(() => {
        if (active) setLoadingUsers(false);
      });

    return () => {
      active = false;
    };
  }, [vehicle.businessId]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const selectedDriver = drivers.find((driver) => String(driver.userId) === selectedUserId);
    if (selectedUserId && !selectedDriver) {
      setError('Select an active driver from this organization.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const response = await assignVehicleDriver(vehicle.id, {
        assignedUserId: selectedDriver?.userId ?? null,
        assignedDriverName: selectedDriver ? businessUserDisplayName(selectedDriver) : null,
        department: vehicle.department?.trim() || null,
      });
      onAssigned(response.data);
      showToast({
        type: 'success',
        message: selectedDriver
          ? `${businessUserDisplayName(selectedDriver)} assigned to ${vehicle.licensePlate}.`
          : `Driver removed from ${vehicle.licensePlate}.`,
      });
      onClose();
    } catch (requestError: unknown) {
      setError(getApiErrorMessage(requestError, 'Failed to update the vehicle driver.'));
    } finally {
      setSaving(false);
    }
  };

  const currentDriverUnavailable = vehicle.assignedUserId != null
    && !loadingUsers
    && !drivers.some((driver) => driver.userId === vehicle.assignedUserId);

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={() => {
        if (!saving) onClose();
      }}
    >
      <section
        className="assignment-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="assign-driver-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <h2 id="assign-driver-title">Assign driver</h2>
            <span className="assignment-modal-vehicle">
              {vehicle.licensePlate} · {vehicle.brand} {vehicle.model}
            </span>
          </div>
          <button
            type="button"
            className="modal-close"
            onClick={onClose}
            aria-label="Close"
            disabled={saving}
          >
            ×
          </button>
        </div>

        <form className="assignment-form" onSubmit={handleSubmit}>
          <label htmlFor="vehicle-driver-select">Driver</label>
          <select
            id="vehicle-driver-select"
            value={selectedUserId}
            onChange={(event) => setSelectedUserId(event.target.value)}
            disabled={loadingUsers || saving || Boolean(error && users.length === 0)}
            autoFocus
          >
            <option value="">No driver assigned</option>
            {currentDriverUnavailable && (
              <option value={vehicle.assignedUserId ?? ''} disabled>
                {vehicle.assignedDriverName || 'Current driver'} — unavailable
              </option>
            )}
            {drivers.map((driver) => (
              <option key={driver.userId} value={driver.userId}>
                {businessUserDisplayName(driver)}{driver.username ? ` — ${driver.email}` : ''}
              </option>
            ))}
          </select>

          {loadingUsers && <p className="assignment-help">Loading organization drivers...</p>}
          {!loadingUsers && drivers.length === 0 && !error && (
            <p className="assignment-help">There are no active employees in this organization.</p>
          )}
          {currentDriverUnavailable && (
            <p className="assignment-help">
              The currently assigned account is no longer an active employee. Choose another driver or remove it.
            </p>
          )}
          {error && <p className="error assignment-error">{error}</p>}

          <div className="assignment-actions">
            <Button type="button" variant="secondary" onClick={onClose} disabled={saving}>
              Cancel
            </Button>
            <Button type="submit" disabled={loadingUsers || saving || Boolean(error && users.length === 0)}>
              {saving ? 'Saving...' : 'Save assignment'}
            </Button>
          </div>
        </form>
      </section>
    </div>
  );
}
