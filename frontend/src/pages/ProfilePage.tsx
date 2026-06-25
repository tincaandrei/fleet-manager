import { useEffect, useState, type FormEvent } from 'react';
import { deleteMyProfileImage, getMe, updateMe, uploadMyProfileImage } from '../api/authApi';
import type { UserProfile } from '../types/auth';
import { showToast } from '../utils/toast';
import { getApiErrorMessage } from '../utils/apiError';
import PageShell from '../components/ui/PageShell';
import PageHeader from '../components/ui/PageHeader';
import { Button } from '../components/ui/Button';
import DataState from '../components/ui/DataState';
import UserAvatar from '../components/UserAvatar';

interface ProfileForm {
  username: string;
  email: string;
  phone: string;
  address: string;
}

const emptyForm: ProfileForm = {
  username: '',
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
  const [imageSaving, setImageSaving] = useState(false);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isImageOpen, setIsImageOpen] = useState(false);

  useEffect(() => {
    getMe()
      .then((res) => {
        setProfile(res.data);
        setForm({
          username: res.data.username ?? '',
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
      username: profile.username ?? '',
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

  const closeImageModal = () => {
    if (imageSaving) return;
    setIsImageOpen(false);
    setImageFile(null);
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
        username: form.username.trim() || null,
        email: form.email.trim(),
        phone: form.phone.trim() || null,
        address: form.address.trim() || null,
      });

      setProfile(res.data);
      setForm({
        username: res.data.username ?? '',
        email: res.data.email ?? '',
        phone: res.data.phone ?? '',
        address: res.data.address ?? '',
      });
      setIsEditOpen(false);
      setSuccess('Profile updated.');
      showToast({ type: 'success', message: 'Profile updated.' });
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to update profile.'));
    } finally {
      setSaving(false);
    }
  };

  const notifyProfileImageUpdated = () => {
    window.dispatchEvent(new Event('profile-image-updated'));
  };

  const handleImageUpload = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (!imageFile) {
      setError('Choose an image first.');
      return;
    }

    setImageSaving(true);
    try {
      const res = await uploadMyProfileImage(imageFile);
      setProfile(res.data);
      setImageFile(null);
      setIsImageOpen(false);
      setSuccess('Profile picture updated.');
      notifyProfileImageUpdated();
      showToast({ type: 'success', message: 'Profile picture updated.' });
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to update profile picture.'));
    } finally {
      setImageSaving(false);
    }
  };

  const handleImageDelete = async () => {
    setError(null);
    setSuccess(null);
    setImageSaving(true);
    try {
      const res = await deleteMyProfileImage();
      setProfile(res.data);
      setImageFile(null);
      setIsImageOpen(false);
      setSuccess('Profile picture removed.');
      notifyProfileImageUpdated();
      showToast({ type: 'success', message: 'Profile picture removed.' });
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to remove profile picture.'));
    } finally {
      setImageSaving(false);
    }
  };

  return (
    <PageShell>
        <PageHeader
          title="My Profile"
          description="View your account and organization assignment."
          actions={profile && <Button onClick={openEdit}>Edit</Button>}
        />
        {error && !isEditOpen && !isImageOpen && <DataState type="error">{error}</DataState>}
        {success && <DataState type="success">{success}</DataState>}
        {profile && (
          <div className="profile-layout">
            {(() => {
              const displayName = profile.username || profile.email;
              return (
                <>
            <section className="profile-card">
              <button
                type="button"
                className="profile-image-trigger"
                onClick={() => {
                  setError(null);
                  setSuccess(null);
                  setIsImageOpen(true);
                }}
                aria-label="Open profile picture"
              >
                <UserAvatar
                  username={displayName}
                  imageUrl={profile.profileImageUrl}
                  className="profile-avatar"
                />
              </button>
              <div className="profile-card-copy">
                <h2>{displayName}</h2>
                <span>{profile.role}</span>
                {profile.businessName && <p>{profile.businessName}</p>}
              </div>
            </section>

            <table className="detail-table">
              <tbody>
                <tr><th>Username</th><td>{profile.username || '-'}</td></tr>
                <tr><th>Email</th><td>{profile.email}</td></tr>
                <tr><th>Phone</th><td>{profile.phone}</td></tr>
                <tr><th>Address</th><td>{profile.address}</td></tr>
                <tr><th>Role</th><td><span className="badge">{profile.role}</span></td></tr>
                <tr><th>Organization</th><td>{profile.businessName ?? 'Pending assignment'}</td></tr>
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
                      Username
                      <input
                        value={form.username}
                        onChange={(event) => updateField('username', event.target.value)}
                        autoComplete="username"
                      />
                    </label>
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

            {isImageOpen && (
              <div className="modal-backdrop" role="presentation" onMouseDown={closeImageModal}>
                <section
                  className="image-modal"
                  role="dialog"
                  aria-modal="true"
                  aria-labelledby="profile-image-title"
                  onMouseDown={(event) => event.stopPropagation()}
                >
                  <div className="modal-header">
                    <h2 id="profile-image-title">Profile Picture</h2>
                    <button type="button" className="modal-close" onClick={closeImageModal} aria-label="Close">
                      x
                    </button>
                  </div>
                  <div className="image-modal-body">
                    <UserAvatar
                      username={displayName}
                      imageUrl={profile.profileImageUrl}
                      className="image-modal-avatar"
                    />
                    <form className="profile-image-form" onSubmit={handleImageUpload}>
                      <label className="profile-image-picker">
                        Change picture
                        <input
                          type="file"
                          accept="image/png,image/jpeg,image/webp"
                          onChange={(event) => setImageFile(event.target.files?.[0] ?? null)}
                        />
                      </label>
                      {imageFile && <span className="profile-image-file-name">{imageFile.name}</span>}
                      {error && <p className="error modal-message">{error}</p>}
                      <div className="profile-image-actions">
                        <Button type="submit" disabled={imageSaving || !imageFile}>
                          {imageSaving ? 'Uploading...' : 'Upload'}
                        </Button>
                        {profile.profileImageUrl && (
                          <Button
                            type="button"
                            variant="secondary"
                            disabled={imageSaving}
                            onClick={handleImageDelete}
                          >
                            Remove
                          </Button>
                        )}
                      </div>
                    </form>
                  </div>
                </section>
              </div>
            )}
                </>
              );
            })()}
          </div>
        )}
    </PageShell>
  );
}
