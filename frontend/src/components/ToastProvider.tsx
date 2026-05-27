import { useCallback, useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { TOAST_EVENT, consumeQueuedToast } from '../utils/toast';
import type { ToastPayload, ToastType } from '../utils/toast';

interface Toast extends ToastPayload {
  id: number;
  type: ToastType;
}

const DEFAULT_DURATION_MS = 7000;

export default function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const timersRef = useRef<Record<number, number>>({});

  const removeToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));

    const timer = timersRef.current[id];
    if (timer) {
      window.clearTimeout(timer);
      delete timersRef.current[id];
    }
  }, []);

  const addToast = useCallback((payload: ToastPayload) => {
    const id = Date.now() + Math.floor(Math.random() * 1000);
    const toast: Toast = {
      id,
      type: payload.type ?? 'error',
      title: payload.title,
      message: payload.message,
      durationMs: payload.durationMs,
    };

    setToasts((current) => [...current, toast]);

    if (payload.durationMs !== 0) {
      timersRef.current[id] = window.setTimeout(
        () => removeToast(id),
        payload.durationMs ?? DEFAULT_DURATION_MS,
      );
    }
  }, [removeToast]);

  useEffect(() => {
    const queuedToast = consumeQueuedToast();
    if (queuedToast) addToast(queuedToast);

    const handleToast = (event: WindowEventMap[typeof TOAST_EVENT]) => {
      addToast(event.detail);
    };

    window.addEventListener(TOAST_EVENT, handleToast);

    return () => {
      window.removeEventListener(TOAST_EVENT, handleToast);
      Object.values(timersRef.current).forEach((timer) => window.clearTimeout(timer));
      timersRef.current = {};
    };
  }, [addToast]);

  return (
    <>
      {children}
      <div className="toast-container" aria-live="polite" aria-atomic="true">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast-${toast.type}`} role="status">
            <div className="toast-content">
              {toast.title && <strong>{toast.title}</strong>}
              <span>{toast.message}</span>
            </div>
            <button
              type="button"
              className="toast-close"
              aria-label="Dismiss notification"
              onClick={() => removeToast(toast.id)}
            >
              x
            </button>
          </div>
        ))}
      </div>
    </>
  );
}
