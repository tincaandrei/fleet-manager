import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/useAuth';
import { getMe } from '../../api/authApi';
import UserAvatar from '../UserAvatar';
import BrandLogo from '../BrandLogo';
import NotificationsBell from './NotificationsBell';

type NavItem = {
  label: string;
  to: string;
  activePrefix: string;
  icon: ReactNode;
  visible?: boolean;
};

const icon = {
  console: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="3" y="3" width="7" height="9" rx="1.5" />
      <rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" />
      <rect x="3" y="16" width="7" height="5" rx="1.5" />
    </svg>
  ),
  vehicles: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M5 11l1.6-4.2A2 2 0 0 1 8.5 5.5h7a2 2 0 0 1 1.9 1.3L19 11" />
      <path d="M4 11h16a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-1" />
      <path d="M3 12v4a1 1 0 0 0 1 1h1" />
      <circle cx="7.5" cy="17" r="1.8" />
      <circle cx="16.5" cy="17" r="1.8" />
    </svg>
  ),
  history: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
      <path d="M14 3v5h5" />
      <path d="M9 13h6M9 17h6" />
    </svg>
  ),
  alerts: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M10.3 4.6 2.9 17.5a1.6 1.6 0 0 0 1.4 2.4h15.4a1.6 1.6 0 0 0 1.4-2.4L13.7 4.6a1.95 1.95 0 0 0-3.4 0z" />
      <path d="M12 9.5v4" />
      <path d="M12 16.8h.01" />
    </svg>
  ),
  users: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  ),
  profile: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-3.5 3.6-6 8-6s8 2.5 8 6" />
    </svg>
  ),
  logout: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <path d="M16 17l5-5-5-5" />
      <path d="M21 12H9" />
    </svg>
  ),
};

interface AppLayoutProps {
  children: ReactNode;
}

/**
 * Shared dashboard shell for all authenticated roles: left sidebar with
 * role-based navigation and profile, plus a slim topbar with notifications.
 */
export default function AppLayout({ children }: AppLayoutProps) {
  const { username, role, isSuperAdmin, isAdmin, isBusinessAdmin, businessId, businessName, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [navOpen, setNavOpen] = useState(false);
  const [profileImageUrl, setProfileImageUrl] = useState<string | null>(null);

  const navItems = useMemo<NavItem[]>(
    () => [
      {
        label: 'Console',
        to: '/superadmin',
        activePrefix: '/superadmin',
        icon: icon.console,
        visible: isSuperAdmin,
      },
      { label: 'Vehicles', to: '/vehicles', activePrefix: '/vehicles', icon: icon.vehicles },
      {
        label: role === 'SUPERADMIN'
          ? 'Document History'
          : role === 'BUSINESS_ADMIN'
          ? 'Organization History'
          : 'My Documents',
        to: '/documents/history',
        activePrefix: '/documents/history',
        icon: icon.history,
      },
      {
        label: 'Alerts',
        to: '/alerts/documents',
        activePrefix: '/alerts',
        icon: icon.alerts,
        visible: isAdmin,
      },
      {
        label: 'Users',
        to: businessId == null ? '/vehicles' : `/businesses/${businessId}/users`,
        activePrefix: '/businesses',
        icon: icon.users,
        visible: isBusinessAdmin && businessId != null,
      },
      { label: 'Profile', to: '/profile', activePrefix: '/profile', icon: icon.profile },
    ],
    [role, isSuperAdmin, isAdmin, isBusinessAdmin, businessId],
  );

  const visibleNavItems = navItems.filter((item) => item.visible !== false);

  const loadProfileImageUrl = useCallback(() => {
    if (!username) {
      setProfileImageUrl(null);
      return;
    }
    getMe()
      .then((res) => setProfileImageUrl(res.data.profileImageUrl))
      .catch(() => setProfileImageUrl(null));
  }, [username]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => loadProfileImageUrl(), 0);
    window.addEventListener('profile-image-updated', loadProfileImageUrl);
    return () => {
      window.clearTimeout(timeoutId);
      window.removeEventListener('profile-image-updated', loadProfileImageUrl);
    };
  }, [loadProfileImageUrl]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => setNavOpen(false), 0);
    return () => window.clearTimeout(timeoutId);
  }, [location.pathname]);

  useEffect(() => {
    document.body.classList.toggle('app-nav-open', navOpen);

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setNavOpen(false);
    };
    if (navOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.body.classList.remove('app-nav-open');
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [navOpen]);

  const handleLogout = () => {
    setNavOpen(false);
    logout();
    navigate('/login');
  };

  const isActive = (prefix: string) =>
    location.pathname === prefix || location.pathname.startsWith(prefix + '/');

  const roleLabel = role === 'SUPERADMIN'
    ? 'Super Admin'
    : role === 'BUSINESS_ADMIN'
    ? 'Organization Admin'
    : role === 'EMPLOYEE'
    ? 'Employee'
    : role ?? '';

  const sidebarContent = (
    <>
      <div className="app-sidebar-brand">
        <BrandLogo className="app-sidebar-logo" />
      </div>

      <nav className="app-sidebar-nav" aria-label="Main navigation">
        {visibleNavItems.map((item) => (
          <Link
            key={item.to + item.label}
            to={item.to}
            className={`app-nav-link${isActive(item.activePrefix) ? ' app-nav-link--active' : ''}`}
            onClick={() => setNavOpen(false)}
          >
            <span className="app-nav-icon">{item.icon}</span>
            <span>{item.label}</span>
          </Link>
        ))}
      </nav>

      <div className="app-sidebar-profile">
        <Link to="/profile" className="app-profile-card" onClick={() => setNavOpen(false)}>
          <UserAvatar username={username} imageUrl={profileImageUrl} />
          <span className="app-profile-copy">
            <span className="app-profile-name">{username}</span>
            <span className="app-profile-role">{roleLabel}</span>
            {businessName && <span className="app-profile-org">{businessName}</span>}
          </span>
        </Link>
        <button type="button" className="app-logout" onClick={handleLogout}>
          <span className="app-nav-icon">{icon.logout}</span>
          <span>Log out</span>
        </button>
      </div>
    </>
  );

  return (
    <div className="app-shell">
      <aside className="app-sidebar" aria-label="Sidebar">
        {sidebarContent}
      </aside>

      <div
        className={`app-nav-overlay${navOpen ? ' app-nav-overlay--open' : ''}`}
        onClick={() => setNavOpen(false)}
        aria-hidden="true"
      />
      <aside
        id="app-mobile-navigation"
        className={`app-sidebar app-sidebar--drawer${navOpen ? ' app-sidebar--open' : ''}`}
        aria-hidden={!navOpen}
      >
        {sidebarContent}
      </aside>

      <div className="app-main">
        <header className="app-topbar">
          <button
            type="button"
            className="app-menu-button"
            aria-label={navOpen ? 'Close navigation menu' : 'Open navigation menu'}
            aria-expanded={navOpen}
            aria-controls="app-mobile-navigation"
            onClick={() => setNavOpen((open) => !open)}
          >
            <span aria-hidden="true" />
            <span aria-hidden="true" />
            <span aria-hidden="true" />
          </button>
          <BrandLogo className="app-topbar-brand" />
          <div className="app-topbar-actions">
            <NotificationsBell />
          </div>
        </header>
        {children}
      </div>
    </div>
  );
}
