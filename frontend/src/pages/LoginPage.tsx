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
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />
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
