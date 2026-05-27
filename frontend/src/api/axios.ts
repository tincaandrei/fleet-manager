import axios from 'axios';
import { getApiErrorMessage } from '../utils/apiError';
import { showToast } from '../utils/toast';

const api = axios.create();

function isAuthFormRequest(url: string | undefined): boolean {
  if (!url) return false;
  return /(^|\/)(api\/auth\/)?(login|register)$/.test(url);
}

/**
 * Perform a client-side logout by clearing stored credentials and redirecting
 * to /login. Called when the server returns 401 on a non-auth endpoint,
 * meaning the token is genuinely expired or invalid.
 */
function forceLogout() {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('role');
  localStorage.removeItem('userId');
  localStorage.removeItem('businessId');
  localStorage.removeItem('businessName');
  // Hard navigate so the React tree re-initialises from localStorage (no stale state).
  window.location.replace('/login');
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status as number | undefined;
    const requestUrl = err.config?.url as string | undefined;
    const isAuthForm = isAuthFormRequest(requestUrl);

    // ── 401 on a protected endpoint: token expired / invalid → force logout ──
    if (status === 401 && !isAuthForm) {
      showToast({
        type: 'error',
        title: 'Session expired',
        message: 'Your session has expired. Please sign in again.',
      });
      forceLogout();
      return Promise.reject(err);
    }

    // ── All other errors (400, 403, 500, network, auth form 401 …) → toast only ──
    if (status !== undefined && status >= 400 || !err.response) {
      showToast({
        type: 'error',
        message: getApiErrorMessage(err, 'Request failed. Please try again.'),
      });
    }

    return Promise.reject(err);
  }
);

export default api;
