export type VehicleStatus = 'ACTIVE' | 'IN_SERVICE' | 'INACTIVE' | 'SOLD' | 'DECOMMISSIONED';
export type VehicleType = 'CAR' | 'VAN' | 'TRUCK' | 'MOTORCYCLE' | 'OTHER';
export type FuelType = 'DIESEL' | 'PETROL' | 'HYBRID' | 'ELECTRIC' | 'LPG' | 'OTHER';
export type OwnershipType = 'OWNED' | 'LEASED' | 'RENTED' | 'OTHER';

export interface VehicleRequest {
  /** Target organization. Required for SUPERADMIN-created vehicles, omitted otherwise. */
  businessId?: number;
  licensePlate: string;
  vin: string;
  brand: string;
  model: string;
  manufactureYear: number;
  vehicleType: VehicleType;
  fuelType: FuelType;
  ownershipType: OwnershipType;
  status: VehicleStatus;
  department: string;
  currentMileage: number;
}

export interface Vehicle extends Omit<VehicleRequest, 'businessId'> {
  id: number;
  businessId: number | null;
  assignedUserId: number | null;
  assignedDriverName: string | null;
  imageUrl: string | null;
  imageOriginalFileName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface VehicleFilters {
  status?: string;
  vehicleType?: string;
  fuelType?: string;
  ownershipType?: string;
  department?: string;
  assignedUserId?: number;
  licensePlate?: string;
  businessId?: number;
}

export interface VehicleAssignmentRequest {
  assignedUserId: number | null;
  /** Sent for compatibility with older fleet-service deployments. */
  assignedDriverName?: string | null;
  /** Sent so older fleet-service deployments preserve the department. */
  department?: string | null;
}
