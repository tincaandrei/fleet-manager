import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { acceptInvitation, validateInvitation } from '../api/authApi';
import { getApiErrorMessage } from '../utils/apiError';
import BrandLogo from '../components/BrandLogo';

export default function AcceptInvitePage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') ?? '';

  const [email, setEmail] = useState<string | null>(null);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!token) {
      setError('Invitation token is missing.');
      setValidating(false);
      return;
    }

    validateInvitation(token)
      .then((res) => {
        if (!res.data.valid) {
          setError(res.data.message || 'Invitation is no longer valid.');
          return;
        }
        setEmail(res.data.email);
      })
      .catch((err: unknown) => setError(getApiErrorMessage(err, 'Invitation is no longer valid.')))
      .finally(() => setValidating(false));
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await acceptInvitation({ token, newPassword: password });
      setSuccess(true);
      window.setTimeout(() => navigate('/login'), 900);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Could not accept invitation.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-bg">
      <div className="auth-card">
        <BrandLogo className="auth-brand" />

        <h1>Accept Invitation</h1>
        <h2>{email ? `Set password for ${email}` : 'Set your account password'}</h2>

        {validating && <p className="success-note">Checking invitation...</p>}
        {!validating && success && <p className="success-note">Account activated. Redirecting to login...</p>}
        {!validating && error && <p className="error">{error}</p>}

        {!validating && !success && !error && (
          <form onSubmit={handleSubmit} className="auth-form">
            <label>
              New password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="new-password"
              />
            </label>

            <label>
              Confirm password
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                autoComplete="new-password"
              />
            </label>

            <button type="submit" disabled={loading}>
              {loading ? 'Activating...' : 'Activate account'}
            </button>
          </form>
        )}

        <p>
          Already activated? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
