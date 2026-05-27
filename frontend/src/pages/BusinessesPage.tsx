import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { listBusinesses } from '../api/authApi';
import type { Business } from '../types/auth';

function apiMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

export default function BusinessesPage() {
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    listBusinesses()
      .then((res) => setBusinesses(res.data))
      .catch((err: unknown) => setError(apiMessage(err, 'Failed to load businesses.')))
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <Navbar />
      <main className="page">
        <div className="page-header">
          <h1>Businesses</h1>
          <Link to="/businesses/new" className="btn">＋ New Business</Link>
        </div>

        {loading && <p className="doc-empty">Loading businesses…</p>}
        {!loading && error && <p className="error">{error}</p>}
        {!loading && !error && businesses.length === 0 && (
          <p className="doc-empty">No businesses yet. Create the first one.</p>
        )}

        {!loading && !error && businesses.length > 0 && (
          <table className="vehicles-table">
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
              {businesses.map((b) => (
                <tr key={b.id}>
                  <td data-label="ID">{b.id}</td>
                  <td data-label="Name">{b.name}</td>
                  <td data-label="Registration">{b.registrationNumber || '-'}</td>
                  <td data-label="Contact">{b.contactEmail || '-'}</td>
                  <td data-label="Phone">{b.phone || '-'}</td>
                  <td data-label="Status">{b.active ? 'Active' : 'Inactive'}</td>
                  <td data-label="Actions" className="actions-cell">
                    <Link to={`/businesses/${b.id}/users`}>Users</Link>
                    {' | '}
                    <Link to={`/businesses/${b.id}/edit`}>Edit</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </>
  );
}
