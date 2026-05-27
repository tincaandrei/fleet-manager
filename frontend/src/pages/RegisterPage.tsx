import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../api/authApi';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    phone: '',
    address: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const set = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await register(form);
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Registration failed.');
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

        <h1>Create Account</h1>
        <h2>Fill in your details to get started</h2>

        <form onSubmit={handleSubmit} className="auth-form">
          {error && <p className="error">{error}</p>}

          <label>
            Username
            <input
              value={form.username}
              onChange={set('username')}
              required
              autoFocus
              autoComplete="username"
            />
          </label>

          <label>
            Email
            <input
              type="email"
              value={form.email}
              onChange={set('email')}
              required
              autoComplete="email"
            />
          </label>

          <label>
            Password
            <input
              type="password"
              value={form.password}
              onChange={set('password')}
              required
              autoComplete="new-password"
            />
          </label>

          <label>
            Phone
            <input
              value={form.phone}
              onChange={set('phone')}
              autoComplete="tel"
            />
          </label>

          <label>
            Address
            <input
              value={form.address}
              onChange={set('address')}
              autoComplete="street-address"
            />
          </label>

          <button type="submit" disabled={loading}>
            {loading ? 'Registering…' : 'Register'}
          </button>
        </form>

        <p>Already have an account? <Link to="/login">Sign in</Link></p>
      </div>
    </div>
  );
}
