export type VehicleStatus = 'ACTIVE' | 'IN_SERVICE' | 'INACTIVE' | 'SOLD' | 'DECOMMISSIONED';
export type VehicleType = 'CAR' | 'VAN' | 'TRUCK' | 'MOTORCYCLE' | 'OTHER';
export type FuelType = 'DIESEL' | 'PETROL' | 'HYBRID' | 'ELECTRIC' | 'LPG' | 'OTHER';
export type OwnershipType = 'OWNED' | 'LEASED' | 'RENTED' | 'OTHER';

export interface VehicleRequest {
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
  assignedUserId?: number;
  assignedDriverName?: string;
  currentMileage: number;
}

export interface Vehicle extends VehicleRequest {
  id: number;
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
}
