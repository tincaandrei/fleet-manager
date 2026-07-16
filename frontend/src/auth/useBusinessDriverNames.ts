import { useEffect, useMemo, useState } from 'react';
import { listBusinessUsers } from '../api/authApi';
import type { BusinessUser } from '../types/auth';
import { businessUserDisplayName } from '../utils/businessUserDisplay';

export function useBusinessDriverNames(businessId: number | null, enabled: boolean) {
  const [users, setUsers] = useState<BusinessUser[]>([]);

  useEffect(() => {
    if (!enabled || businessId == null) return;

    let active = true;
    listBusinessUsers(businessId)
      .then((response) => {
        if (active) setUsers(response.data);
      })
      .catch(() => {
        // Persisted vehicle data remains usable if this optional fallback is unavailable.
      });

    return () => {
      active = false;
    };
  }, [businessId, enabled]);

  return useMemo(
    () => new Map(users.map((user) => [user.userId, businessUserDisplayName(user)])),
    [users],
  );
}
