import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

type NavItem = {
  label: string;
  to: string;
  activePrefix: string;
  visible?: boolean;
};

export default function Navbar() {
  const { username, role, isSuperAdmin, isAdmin, isBusinessAdmin, businessId, businessName, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const navItems = useMemo<NavItem[]>(
    () => [
      // SUPERADMIN: business management
      {
        label: 'Businesses',
        to: '/businesses',
        activePrefix: '/businesses',
        visible: isSuperAdmin,
      },
      // All roles: vehicles
      { label: 'Vehicles', to: '/vehicles', activePrefix: '/vehicles' },
      // SUPERADMIN + BUSINESS_ADMIN: alerts
      {
        label: 'Alerts',
        to: '/alerts/documents',
        activePrefix: '/alerts',
        visible: isAdmin,
      },
      {
        label: 'Users',
        to: businessId == null ? '/vehicles' : `/businesses/${businessId}/users`,
        activePrefix: '/businesses',
        visible: isBusinessAdmin && businessId != null,
      },
      // All roles: profile
      { label: 'Profile', to: '/profile', activePrefix: '/profile' },
    ],
    [isSuperAdmin, isAdmin, isBusinessAdmin, businessId],
  );

  const visibleNavItems = navItems.filter((item) => item.visible !== false);

  useEffect(() => {
    setIsMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    document.body.classList.toggle('mobile-menu-open', isMenuOpen);

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsMenuOpen(false);
      }
    };

    if (isMenuOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }

    return () => {
      document.body.classList.remove('mobile-menu-open');
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isMenuOpen]);

  const handleLogout = () => {
    setIsMenuOpen(false);
    logout();
    navigate('/login');
  };

  const isActive = (prefix: string) =>
    location.pathname === prefix || location.pathname.startsWith(prefix + '/');

  const initials = username ? username.slice(0, 2).toUpperCase() : '??';

  /** Human-readable role label */
  const roleLabel = role === 'SUPERADMIN'
    ? 'Super Admin'
    : role === 'BUSINESS_ADMIN'
    ? 'Business Admin'
    : role === 'EMPLOYEE'
    ? 'Employee'
    : role ?? '';

  const renderNavLink = (item: NavItem, className: string) => (
    <Link
      key={item.to}
      to={item.to}
      className={`${className}${isActive(item.activePrefix) ? ` ${className}--active` : ''}`}
    >
      {item.label}
    </Link>
  );

  return (
    <nav className="navbar">
      <span className="navbar-brand">
        <span className="navbar-brand-dot" aria-hidden="true" />
        Fleet Manager
      </span>

      <div className="nav-group" role="navigation" aria-label="Main navigation">
        {visibleNavItems.map((item) => renderNavLink(item, 'nav-link'))}
      </div>

      <div className="navbar-user">
        <div className="nav-avatar" aria-hidden="true" title={username ?? ''}>
          {initials}
        </div>

        <div className="nav-user-info">
          <span className="nav-username">{username}</span>
          {roleLabel && <span className="nav-role">{roleLabel}</span>}
          {businessName && <span className="nav-business">{businessName}</span>}
        </div>

        <button onClick={handleLogout} className="btn-logout" aria-label="Log out">
          Logout
        </button>
      </div>

      <button
        type="button"
        className="navbar-menu-button"
        aria-label={isMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
        aria-expanded={isMenuOpen}
        aria-controls="mobile-navigation"
        onClick={() => setIsMenuOpen((open) => !open)}
      >
        <span aria-hidden="true" />
        <span aria-hidden="true" />
        <span aria-hidden="true" />
      </button>

      <div
        className={`mobile-menu-overlay${isMenuOpen ? ' mobile-menu-overlay--open' : ''}`}
        onClick={() => setIsMenuOpen(false)}
        aria-hidden="true"
      />

      <aside
        id="mobile-navigation"
        className={`mobile-menu-panel${isMenuOpen ? ' mobile-menu-panel--open' : ''}`}
        aria-hidden={!isMenuOpen}
      >
        <div className="mobile-menu-header">
          <span className="navbar-brand">
            <span className="navbar-brand-dot" aria-hidden="true" />
            Fleet Manager
          </span>
          <button
            type="button"
            className="mobile-menu-close"
            aria-label="Close navigation menu"
            onClick={() => setIsMenuOpen(false)}
          >
            <span aria-hidden="true" />
            <span aria-hidden="true" />
          </button>
        </div>

        <div className="mobile-menu-user">
          <div className="nav-avatar" aria-hidden="true">
            {initials}
          </div>
          <div className="nav-user-info">
            <span className="nav-username">{username}</span>
            {roleLabel && <span className="nav-role">{roleLabel}</span>}
            {businessName && <span className="nav-business">{businessName}</span>}
          </div>
        </div>

        <div className="mobile-nav-links" role="navigation" aria-label="Mobile navigation">
          {visibleNavItems.map((item) => renderNavLink(item, 'mobile-nav-link'))}
        </div>

        <button onClick={handleLogout} className="mobile-logout" aria-label="Log out">
          Logout
        </button>
      </aside>
    </nav>
  );
}
