export type ToastType = 'error' | 'success' | 'info';

export interface ToastPayload {
  type?: ToastType;
  title?: string;
  message: string;
  durationMs?: number;
}

export const TOAST_EVENT = 'fleet:toast';

const QUEUED_TOAST_KEY = 'fleet:queued-toast';

declare global {
  interface WindowEventMap {
    [TOAST_EVENT]: CustomEvent<ToastPayload>;
  }
}

export function showToast(toast: ToastPayload) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new CustomEvent<ToastPayload>(TOAST_EVENT, { detail: toast }));
}

export function queueToast(toast: ToastPayload) {
  if (typeof window === 'undefined') return;
  sessionStorage.setItem(QUEUED_TOAST_KEY, JSON.stringify(toast));
  showToast(toast);
}

export function consumeQueuedToast(): ToastPayload | null {
  if (typeof window === 'undefined') return null;

  const raw = sessionStorage.getItem(QUEUED_TOAST_KEY);
  if (!raw) return null;

  sessionStorage.removeItem(QUEUED_TOAST_KEY);

  try {
    const parsed = JSON.parse(raw) as ToastPayload;
    return parsed?.message ? parsed : null;
  } catch {
    return null;
  }
}
