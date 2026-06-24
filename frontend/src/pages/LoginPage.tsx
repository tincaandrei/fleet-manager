import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login as apiLogin } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import { homeForRole, normalizeRole } from '../auth/roleHome';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await apiLogin({ username, password });
      const { token, username: name, role, userId, businessId, businessName } = res.data;
      const normalizedRole = normalizeRole(role);
      if (!normalizedRole) {
        setError('Unsupported role returned by server.');
        return;
      }
      login(token, name, normalizedRole, userId, businessId, businessName);
      navigate(homeForRole(normalizedRole, businessId));
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message ?? 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-bg">
      <div className="auth-card">
        {/* Brand */}
        <div className="auth-brand">
          <span className="auth-brand-dot" aria-hidden="true" />
          <span className="auth-brand-text">Fleet Manager</span>
        </div>

        <h1>Sign In</h1>
        <h2>Enter your credentials to continue</h2>

        <form onSubmit={handleSubmit} className="auth-form">
          {error && <p className="error">{error}</p>}

          <label>
            Username
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoFocus
              autoComplete="username"
            />
          </label>

          <label>
            Password
            <span className="password-input-wrap">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
              <button
                type="button"
                className="password-visibility-toggle"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                aria-pressed={showPassword}
                onClick={() => setShowPassword((visible) => !visible)}
              >
                {showPassword ? (
                  <svg
                    aria-hidden="true"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="m2 2 20 20" />
                    <path d="M10.58 10.58a2 2 0 0 0 2.83 2.83" />
                    <path d="M9.88 4.24A10.94 10.94 0 0 1 12 4c5 0 9 4 10 8a11.45 11.45 0 0 1-3.1 5.18" />
                    <path d="M6.61 6.61A11.45 11.45 0 0 0 2 12c1 4 5 8 10 8a10.94 10.94 0 0 0 5.39-1.39" />
                  </svg>
                ) : (
                  <svg
                    aria-hidden="true"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M2 12s4-8 10-8 10 8 10 8-4 8-10 8S2 12 2 12Z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                )}
              </button>
            </span>
          </label>

          <button type="submit" disabled={loading}>
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <p>No account? <Link to="/register">Register here</Link></p>
      </div>
    </div>
  );
}
