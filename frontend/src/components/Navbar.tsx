import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/notificationApi';
import { getMe } from '../api/authApi';
import UserAvatar from './UserAvatar';
import type { NotificationResponse } from '../types/notification';

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
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [profileImageUrl, setProfileImageUrl] = useState<string | null>(null);

  const navItems = useMemo<NavItem[]>(
    () => [
      // SUPERADMIN: organization management
      {
        label: 'Organizations',
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

  const loadUnreadCount = useCallback(() => {
    return getUnreadNotificationCount()
      .then((res) => setUnreadCount(res.data.count))
      .catch(() => undefined);
  }, []);

  const loadNotifications = useCallback(() => {
    setNotificationsLoading(true);
    return listNotifications()
      .then((res) => setNotifications(res.data))
      .catch(() => undefined)
      .finally(() => setNotificationsLoading(false));
  }, []);

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
    const timeoutId = window.setTimeout(() => {
      setIsMenuOpen(false);
      setNotificationsOpen(false);
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [location.pathname]);

  useEffect(() => {
    void loadUnreadCount();
    const intervalId = window.setInterval(() => {
      void loadUnreadCount();
    }, 20000);
    return () => window.clearInterval(intervalId);
  }, [loadUnreadCount]);

  useEffect(() => {
    loadProfileImageUrl();
    window.addEventListener('profile-image-updated', loadProfileImageUrl);
    return () => window.removeEventListener('profile-image-updated', loadProfileImageUrl);
  }, [loadProfileImageUrl]);

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
    setNotificationsOpen(false);
    logout();
    navigate('/login');
  };

  const handleToggleNotifications = () => {
    const nextOpen = !notificationsOpen;
    setNotificationsOpen(nextOpen);
    if (nextOpen) {
      void loadNotifications();
      void loadUnreadCount();
    }
  };

  const handleMarkRead = async (notificationId: string) => {
    await markNotificationRead(notificationId);
    setNotifications((current) =>
      current.map((notification) =>
        notification.id === notificationId ? { ...notification, isRead: true } : notification,
      ),
    );
    await loadUnreadCount();
  };

  const handleMarkAllRead = async () => {
    await markAllNotificationsRead();
    setNotifications((current) =>
      current.map((notification) => ({ ...notification, isRead: true })),
    );
    setUnreadCount(0);
  };

  const isActive = (prefix: string) =>
    location.pathname === prefix || location.pathname.startsWith(prefix + '/');

  /** Human-readable role label */
  const roleLabel = role === 'SUPERADMIN'
    ? 'Super Admin'
    : role === 'BUSINESS_ADMIN'
    ? 'Organization Admin'
    : role === 'EMPLOYEE'
    ? 'Employee'
    : role ?? '';

  const renderNavLink = (item: NavItem, className: string) => (
    <Link
      key={item.to}
      to={item.to}
      className={`${className}${isActive(item.activePrefix) ? ` ${className}--active` : ''}`}
      onClick={() => setIsMenuOpen(false)}
    >
      {item.label}
    </Link>
  );

  const renderNotifications = (variant: 'desktop' | 'mobile') => (
    <div className={`notification-menu notification-menu--${variant}`}>
      <button
        type="button"
        className="notification-trigger"
        aria-label="Notifications"
        aria-expanded={notificationsOpen}
        onClick={handleToggleNotifications}
      >
        <svg
          className="notification-bell"
          aria-hidden="true"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        <span className="sr-only">Notifications</span>
        {unreadCount > 0 && (
          <span className="notification-count">{unreadCount > 99 ? '99+' : unreadCount}</span>
        )}
      </button>

      {notificationsOpen && (
        <div className="notification-dropdown" role="menu">
          <div className="notification-dropdown-header">
            <strong>Notifications</strong>
            {unreadCount > 0 && (
              <button type="button" onClick={handleMarkAllRead}>
                Mark all read
              </button>
            )}
          </div>

          {notificationsLoading && <p className="notification-empty">Loading notifications...</p>}

          {!notificationsLoading && notifications.length === 0 && (
            <p className="notification-empty">No notifications yet.</p>
          )}

          {!notificationsLoading && notifications.map((notification) => (
            <button
              key={notification.id}
              type="button"
              className={`notification-item${notification.isRead ? '' : ' notification-item--unread'}`}
              onClick={() => {
                if (!notification.isRead) {
                  void handleMarkRead(notification.id);
                }
              }}
            >
              <span className="notification-title">{notification.title}</span>
              <span className="notification-message">{notification.message}</span>
              <span className="notification-time">
                {new Date(notification.createdAt).toLocaleString()}
              </span>
            </button>
          ))}
        </div>
      )}
    </div>
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
        {renderNotifications('desktop')}

        <UserAvatar username={username} imageUrl={profileImageUrl} />

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
          <UserAvatar username={username} imageUrl={profileImageUrl} />
          <div className="nav-user-info">
            <span className="nav-username">{username}</span>
            {roleLabel && <span className="nav-role">{roleLabel}</span>}
            {businessName && <span className="nav-business">{businessName}</span>}
          </div>
        </div>

        {renderNotifications('mobile')}

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
