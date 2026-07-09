export type NotificationType =
  | 'DOCUMENT_PARSING_COMPLETED'
  | 'DOCUMENT_PARSING_FAILED'
  | 'DOCUMENT_PENDING_REVIEW'
  | 'REPORT_GENERATED'
  | 'DOCUMENT_EXPIRING';

export interface NotificationResponse {
  id: string;
  userId: number;
  documentId: string | null;
  type: NotificationType;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface UnreadNotificationCountResponse {
  count: number;
}

export interface MarkAllNotificationsReadResponse {
  count: number;
}
