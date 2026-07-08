import { useState } from 'react';

export const DEFAULT_PAGE_SIZE = 10;

/**
 * Client-side pagination over an already filtered list.
 * The current page is clamped at render time, so when a search/filter shrinks
 * the list the pager stays valid without resetting other state.
 */
export function usePagedList<T>(items: T[], pageSize: number = DEFAULT_PAGE_SIZE) {
  const [rawPage, setPage] = useState(1);
  const pageCount = Math.max(1, Math.ceil(items.length / pageSize));
  const page = Math.min(rawPage, pageCount);

  const start = (page - 1) * pageSize;
  const pageItems = items.slice(start, start + pageSize);

  return { page, pageCount, pageItems, setPage };
}
