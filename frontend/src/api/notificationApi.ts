import api from './axios';
import type {
  MarkAllNotificationsReadResponse,
  NotificationResponse,
  UnreadNotificationCountResponse,
} from '../types/notification';

export const listNotifications = () =>
  api.get<NotificationResponse[]>('/api/notifications');

export const getUnreadNotificationCount = () =>
  api.get<UnreadNotificationCountResponse>('/api/notifications/unread-count');

export const markNotificationRead = (notificationId: string) =>
  api.patch<NotificationResponse>(`/api/notifications/${notificationId}/read`);

export const markAllNotificationsRead = () =>
  api.patch<MarkAllNotificationsReadResponse>('/api/notifications/read-all');
