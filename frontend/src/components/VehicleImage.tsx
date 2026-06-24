import { useEffect, useState } from 'react';
import { getVehicleImage } from '../api/vehicleApi';
import type { Vehicle } from '../types/vehicle';

interface VehicleImageProps {
  vehicle: Pick<Vehicle, 'id' | 'brand' | 'model' | 'licensePlate' | 'imageUrl' | 'updatedAt'>;
  className?: string;
}

export default function VehicleImage({ vehicle, className = '' }: VehicleImageProps) {
  const [src, setSrc] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let objectUrl: string | null = null;
    let active = true;
    setSrc(null);
    setFailed(false);

    if (!vehicle.imageUrl) {
      return () => undefined;
    }

    getVehicleImage(vehicle.imageUrl, vehicle.updatedAt)
      .then((res) => {
        if (!active) {
          return;
        }
        objectUrl = URL.createObjectURL(res.data);
        setSrc(objectUrl);
      })
      .catch(() => {
        if (active) {
          setFailed(true);
        }
      });

    return () => {
      active = false;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [vehicle.id, vehicle.imageUrl, vehicle.updatedAt]);

  const fallback = (
    <div className={`vehicle-image-fallback${className ? ` ${className}` : ''}`} aria-hidden="true">
      <span>{vehicle.brand.slice(0, 1)}{vehicle.model.slice(0, 1)}</span>
    </div>
  );

  if (!src || failed) {
    return fallback;
  }

  return (
    <img
      className={`vehicle-image${className ? ` ${className}` : ''}`}
      src={src}
      alt={`${vehicle.licensePlate} ${vehicle.brand} ${vehicle.model}`}
      loading="lazy"
    />
  );
}
