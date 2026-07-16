import type { BusinessUser } from '../types/auth';
import type { Vehicle } from '../types/vehicle';

export function businessUserDisplayName(user: Pick<BusinessUser, 'username' | 'email'>) {
  return user.username?.trim() || user.email;
}

export function assignedDriverDisplayName(
  vehicle: Pick<Vehicle, 'assignedUserId' | 'assignedDriverName'>,
  driverNames: ReadonlyMap<number, string>,
) {
  if (vehicle.assignedDriverName?.trim()) return vehicle.assignedDriverName.trim();
  if (vehicle.assignedUserId == null) return '-';
  return driverNames.get(vehicle.assignedUserId)
    || `User #${vehicle.assignedUserId} (unavailable)`;
}
