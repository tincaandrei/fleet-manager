import { useEffect, useState } from 'react';
import { getMe } from '../api/authApi';
import type { UserProfile } from '../types/auth';
import Navbar from '../components/Navbar';

export default function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMe()
      .then((res) => setProfile(res.data))
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load profile.'));
  }, []);

  return (
    <>
      <Navbar />
      <main className="page">
        <h1>My Profile</h1>
        {error && <p className="error">{error}</p>}
        {profile && (
          <table className="detail-table">
            <tbody>
              <tr><th>Username</th><td>{profile.username}</td></tr>
              <tr><th>Email</th><td>{profile.email}</td></tr>
              <tr><th>Phone</th><td>{profile.phone}</td></tr>
              <tr><th>Address</th><td>{profile.address}</td></tr>
              <tr><th>Role</th><td><span className="badge">{profile.role}</span></td></tr>
            </tbody>
          </table>
        )}
      </main>
    </>
  );
}
