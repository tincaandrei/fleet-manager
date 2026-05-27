import { useEffect, useState, type FormEvent } from 'react';
import { getMe, updateMe } from '../api/authApi';
import type { UserProfile } from '../types/auth';
import Navbar from '../components/Navbar';
import { showToast } from '../utils/toast';

interface ProfileForm {
  email: string;
  phone: string;
  address: string;
}

const emptyForm: ProfileForm = {
  email: '',
  phone: '',
  address: '',
};

export default function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [form, setForm] = useState<ProfileForm>(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);

  useEffect(() => {
    getMe()
      .then((res) => {
        setProfile(res.data);
        setForm({
          email: res.data.email ?? '',
          phone: res.data.phone ?? '',
          address: res.data.address ?? '',
        });
      })
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load profile.'));
  }, []);

  const updateField = (field: keyof ProfileForm, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setError(null);
    setSuccess(null);
  };

  const openEdit = () => {
    if (!profile) return;
    setForm({
      email: profile.email ?? '',
      phone: profile.phone ?? '',
      address: profile.address ?? '',
    });
    setError(null);
    setSuccess(null);
    setIsEditOpen(true);
  };

  const closeEdit = () => {
    if (saving) return;
    setIsEditOpen(false);
    setError(null);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (!form.email.trim()) {
      setError('Email is required.');
      return;
    }

    setSaving(true);
    try {
      const res = await updateMe({
        email: form.email.trim(),
        phone: form.phone.trim() || null,
        address: form.address.trim() || null,
      });

      setProfile(res.data);
      setForm({
        email: res.data.email ?? '',
        phone: res.data.phone ?? '',
        address: res.data.address ?? '',
      });
      setIsEditOpen(false);
      setSuccess('Profile updated.');
      showToast({ type: 'success', message: 'Profile updated.' });
    } catch (err: any) {
      const message = err.response?.data?.message ?? 'Failed to update profile.';
      setError(message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <Navbar />
      <main className="page">
        <div className="profile-header">
          <h1>My Profile</h1>
          {profile && (
            <button type="button" className="btn" onClick={openEdit}>
              Edit
            </button>
          )}
        </div>
        {error && !isEditOpen && <p className="error">{error}</p>}
        {success && <p className="success-note">{success}</p>}
        {profile && (
          <div className="profile-layout">
            <table className="detail-table">
              <tbody>
                <tr><th>Username</th><td>{profile.username}</td></tr>
                <tr><th>Email</th><td>{profile.email}</td></tr>
                <tr><th>Phone</th><td>{profile.phone}</td></tr>
                <tr><th>Address</th><td>{profile.address}</td></tr>
                <tr><th>Role</th><td><span className="badge">{profile.role}</span></td></tr>
              </tbody>
            </table>

            {isEditOpen && (
              <div className="modal-backdrop" role="presentation" onMouseDown={closeEdit}>
                <section
                  className="profile-modal"
                  role="dialog"
                  aria-modal="true"
                  aria-labelledby="profile-edit-title"
                  onMouseDown={(event) => event.stopPropagation()}
                >
                  <div className="modal-header">
                    <h2 id="profile-edit-title">Edit Profile</h2>
                    <button type="button" className="modal-close" onClick={closeEdit} aria-label="Close">
                      x
                    </button>
                  </div>
                  <form className="profile-edit-form" onSubmit={handleSubmit}>
                    <label>
                      Email
                      <input
                        type="email"
                        value={form.email}
                        onChange={(event) => updateField('email', event.target.value)}
                        required
                      />
                    </label>
                    <label>
                      Phone
                      <input
                        type="tel"
                        value={form.phone}
                        onChange={(event) => updateField('phone', event.target.value)}
                      />
                    </label>
                    <label className="full-width">
                      Address
                      <textarea
                        value={form.address}
                        onChange={(event) => updateField('address', event.target.value)}
                        rows={3}
                      />
                    </label>
                    {error && <p className="error modal-message">{error}</p>}
                    <div className="profile-actions">
                      <button type="button" className="btn btn-secondary" onClick={closeEdit} disabled={saving}>
                        Cancel
                      </button>
                      <button type="submit" disabled={saving}>
                        {saving ? 'Saving...' : 'Save changes'}
                      </button>
                    </div>
                  </form>
                </section>
              </div>
            )}
          </div>
        )}
      </main>
    </>
  );
}
