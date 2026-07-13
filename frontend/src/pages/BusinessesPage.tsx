import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listBusinesses, listUnassignedUsers } from '../api/authApi';
import type { Business, UserProfile } from '../types/auth';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import { ButtonLink } from '../components/ui/Button';
import DataState from '../components/ui/DataState';
import ResponsiveTable from '../components/ui/ResponsiveTable';
import Pagination from '../components/ui/Pagination';
import { usePagedList } from '../components/ui/usePagedList';
import PendingAccountsSection from '../components/superadmin/PendingAccountsSection';
import { getApiErrorMessage } from '../utils/apiError';

export default function BusinessesPage() {
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [unassignedUsers, setUnassignedUsers] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { page, pageCount, pageItems, setPage } = usePagedList(businesses);

  const load = () => {
    setLoading(true);
    setError(null);
    Promise.all([listBusinesses(), listUnassignedUsers()])
      .then(([businessRes, unassignedRes]) => {
        setBusinesses(businessRes.data);
        setUnassignedUsers(unassignedRes.data);
      })
      .catch((err: unknown) => setError(getApiErrorMessage(err, 'Failed to load organizations.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const timeoutId = window.setTimeout(() => load(), 0);
    return () => window.clearTimeout(timeoutId);
  }, []);

  const handleAssigned = (userId: number) => {
    setUnassignedUsers((current) => current.filter((user) => user.userId !== userId));
  };

  return (
    <PageShell>
        <PageHeader
          title="Organizations"
          description="Manage organizations and pending account assignments."
          actions={(
            <>
              <ButtonLink to="/superadmin" variant="secondary">Open Console</ButtonLink>
              <ButtonLink to="/businesses/new">New Organization</ButtonLink>
            </>
          )}
        />

        {loading && <DataState type="loading">Loading organizations...</DataState>}
        {!loading && error && <DataState type="error">{error}</DataState>}
        {!loading && !error && businesses.length === 0 && (
          <DataState>No organizations yet. Create the first one.</DataState>
        )}

        {!loading && !error && businesses.length > 0 && (
          <>
            <PendingAccountsSection
              businesses={businesses}
              users={unassignedUsers}
              onAssigned={handleAssigned}
            />

            <ResponsiveTable ariaLabel="Organizations">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Registration</th>
                  <th>Contact</th>
                  <th>Phone</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageItems.map((business) => (
                  <tr key={business.id}>
                    <td data-label="ID">{business.id}</td>
                    <td data-label="Name">{business.name}</td>
                    <td data-label="Registration">{business.registrationNumber || '-'}</td>
                    <td data-label="Contact">{business.contactEmail || '-'}</td>
                    <td data-label="Phone">{business.phone || '-'}</td>
                    <td data-label="Status">{business.active ? 'Active' : 'Inactive'}</td>
                    <td data-label="Actions" className="actions-cell">
                      <Link to={`/businesses/${business.id}/users`}>Users</Link>
                      {' | '}
                      <Link to={`/businesses/${business.id}/edit`}>Edit</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </ResponsiveTable>
            <Pagination
              page={page}
              pageCount={pageCount}
              onPageChange={setPage}
              summary={`${businesses.length} organizations`}
            />
          </>
        )}
    </PageShell>
  );
}
