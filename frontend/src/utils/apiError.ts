import axios from 'axios';

type ErrorBody = {
  message?: unknown;
  error?: unknown;
  detail?: unknown;
  title?: unknown;
  errors?: unknown;
};

function firstString(value: unknown): string | null {
  if (typeof value === 'string' && value.trim()) {
    return value.trim();
  }

  if (Array.isArray(value)) {
    const messages = value
      .map((item) => firstString(item))
      .filter((item): item is string => Boolean(item));
    return messages.length > 0 ? messages.slice(0, 3).join(' ') : null;
  }

  if (value && typeof value === 'object') {
    const messages = Object.values(value)
      .map((item) => firstString(item))
      .filter((item): item is string => Boolean(item));
    return messages.length > 0 ? messages.slice(0, 3).join(' ') : null;
  }

  return null;
}

export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ErrorBody | string | unknown;

    if (typeof data === 'string') {
      const dataMessage = firstString(data);
      if (dataMessage) return dataMessage;
    }

    if (data && typeof data === 'object') {
      const body = data as ErrorBody;
      const bodyMessage =
        firstString(body.message) ??
        firstString(body.detail) ??
        firstString(body.error) ??
        firstString(body.title) ??
        firstString(body.errors);

      if (bodyMessage) return bodyMessage;

      const dataMessage = firstString(data);
      if (dataMessage) return dataMessage;
    }

    if (!err.response) {
      return 'Server unavailable. Please try again.';
    }

    if (err.response.statusText) {
      return err.response.statusText;
    }
  }

  if (err instanceof Error && err.message) {
    return err.message;
  }

  return fallback;
}
