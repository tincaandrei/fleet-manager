import { useCallback, useEffect, useRef, useState } from 'react';
import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../../api/notificationApi';
import type { NotificationResponse } from '../../types/notification';

/** Notification bell + dropdown, shown in the app topbar. */
export default function NotificationsBell() {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const loadUnreadCount = useCallback(() => {
    return getUnreadNotificationCount()
      .then((res) => setUnreadCount(res.data.count))
      .catch(() => undefined);
  }, []);

  const loadNotifications = useCallback(() => {
    setLoading(true);
    return listNotifications()
      .then((res) => setNotifications(res.data))
      .catch(() => undefined)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    void loadUnreadCount();
    const intervalId = window.setInterval(() => {
      void loadUnreadCount();
    }, 20000);
    return () => window.clearInterval(intervalId);
  }, [loadUnreadCount]);

  useEffect(() => {
    if (!open) return;

    const handlePointerDown = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };

    document.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [open]);

  const handleToggle = () => {
    const nextOpen = !open;
    setOpen(nextOpen);
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

  return (
    <div className="notification-menu" ref={containerRef}>
      <button
        type="button"
        className="notification-trigger"
        aria-label="Notifications"
        aria-expanded={open}
        onClick={handleToggle}
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

      {open && (
        <div className="notification-dropdown" role="menu">
          <div className="notification-dropdown-header">
            <strong>Notifications</strong>
            {unreadCount > 0 && (
              <button type="button" onClick={handleMarkAllRead}>
                Mark all read
              </button>
            )}
          </div>

          {loading && <p className="notification-empty">Loading notifications...</p>}

          {!loading && notifications.length === 0 && (
            <p className="notification-empty">No notifications yet.</p>
          )}

          {!loading && notifications.map((notification) => (
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
}
