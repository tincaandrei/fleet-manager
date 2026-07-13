import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { acceptInvitation, validateInvitation } from '../api/authApi';
import { getApiErrorMessage } from '../utils/apiError';
import BrandLogo from '../components/BrandLogo';

const SPECIAL_CHARACTER_PATTERN = /[^\p{L}\p{N}\s]/u;

const PASSWORD_REQUIREMENTS = [
  {
    label: 'at least 8 characters',
    isMet: (value: string) => value.trim().length >= 8,
  },
  {
    label: 'a letter',
    isMet: (value: string) => value.trim().split('').some((char) => /\p{L}/u.test(char)),
  },
  {
    label: 'a number',
    isMet: (value: string) => value.trim().split('').some((char) => /\p{N}/u.test(char)),
  },
  {
    label: 'a special character',
    isMet: (value: string) => SPECIAL_CHARACTER_PATTERN.test(value.trim()),
  },
] as const;

function getMissingPasswordRequirements(value: string) {
  return PASSWORD_REQUIREMENTS.filter((requirement) => !requirement.isMet(value)).map(
    (requirement) => requirement.label,
  );
}

function formatRequirementList(items: string[]) {
  if (items.length <= 1) {
    return items[0] ?? '';
  }

  if (items.length === 2) {
    return `${items[0]} and ${items[1]}`;
  }

  return `${items.slice(0, -1).join(', ')}, and ${items[items.length - 1]}`;
}

function passwordRequirementMessage(missingRequirements: string[]) {
  if (missingRequirements.length === 0) {
    return 'Password meets the requirements.';
  }

  return `Missing: ${formatRequirementList(missingRequirements)}.`;
}

export default function AcceptInvitePage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') ?? '';

  const [email, setEmail] = useState<string | null>(null);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const missingPasswordRequirements = getMissingPasswordRequirements(password);
  const isPasswordStrong = missingPasswordRequirements.length === 0;
  const passwordHelpText = password
    ? passwordRequirementMessage(missingPasswordRequirements)
    : 'Use at least 8 characters with a letter, a number, and a special character.';
  const passwordHelpClassName = password
    ? `password-requirements${isPasswordStrong ? ' password-requirements--ok' : ''}`
    : 'password-requirements password-requirements--neutral';

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      if (!token) {
        setPageError('Password setup token is missing.');
        setValidating(false);
        return;
      }

      validateInvitation(token)
        .then((res) => {
          if (!res.data.valid) {
            setPageError(res.data.message || 'This password setup link is no longer valid.');
            return;
          }
          setEmail(res.data.email);
        })
        .catch((err: unknown) => setPageError(getApiErrorMessage(err, 'This password setup link is no longer valid.')))
        .finally(() => setValidating(false));
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    if (!isPasswordStrong) {
      setSubmitError(passwordRequirementMessage(missingPasswordRequirements));
      return;
    }

    if (password !== confirmPassword) {
      setSubmitError('Passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await acceptInvitation({ token, newPassword: password });
      setSuccess(true);
      window.setTimeout(() => navigate('/login'), 900);
    } catch (err: unknown) {
      const message = getApiErrorMessage(err, 'Could not set the password.');
      setSubmitError(message === 'PASSWORD_TOO_WEAK' ? passwordRequirementMessage(missingPasswordRequirements) : message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-bg">
      <div className="auth-card">
        <BrandLogo className="auth-brand" />

        <h1>Set Your Password</h1>
        <h2>{email ? `Set password for ${email}` : 'Set your account password'}</h2>

        {validating && <p className="success-note">Checking password setup link...</p>}
        {!validating && success && <p className="success-note">Password saved. Redirecting to login...</p>}
        {!validating && pageError && <p className="error">{pageError}</p>}

        {!validating && !success && !pageError && (
          <form onSubmit={handleSubmit} className="auth-form">
            {submitError && <p className="error">{submitError}</p>}

            <label>
              New password
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="new-password"
                aria-describedby="password-requirements"
                aria-invalid={password.length > 0 && !isPasswordStrong}
              />
              <span id="password-requirements" className={passwordHelpClassName} aria-live="polite">
                {passwordHelpText}
              </span>
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
              {loading ? 'Saving...' : 'Save password'}
            </button>
          </form>
        )}

        <p>
          Password already set? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
