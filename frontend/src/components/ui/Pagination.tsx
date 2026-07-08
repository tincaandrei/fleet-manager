import { Button } from './Button';

interface PaginationProps {
  page: number;
  pageCount: number;
  onPageChange: (page: number) => void;
  /** Optional summary, e.g. "34 users". */
  summary?: string;
}

/** Compact pager; renders nothing when there is a single page. */
export default function Pagination({ page, pageCount, onPageChange, summary }: PaginationProps) {
  if (pageCount <= 1) {
    return null;
  }
  return (
    <nav className="table-pagination" aria-label="Pagination">
      {summary && <span className="table-pagination-summary">{summary}</span>}
      <div className="table-pagination-controls">
        <Button
          variant="secondary"
          size="sm"
          disabled={page <= 1}
          onClick={() => onPageChange(Math.max(1, page - 1))}
        >
          Previous
        </Button>
        <span className="table-pagination-status">Page {page} of {pageCount}</span>
        <Button
          variant="secondary"
          size="sm"
          disabled={page >= pageCount}
          onClick={() => onPageChange(Math.min(pageCount, page + 1))}
        >
          Next
        </Button>
      </div>
    </nav>
  );
}
