import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { downloadDocument, exportDocumentHistoryPdf, listDocumentHistory } from '../api/documentApi';
import { useAuth } from '../auth/useAuth';
import type { DocumentHistoryItem, DocumentStatus } from '../types/document';
import DataState from '../components/ui/DataState';
import PageHeader from '../components/ui/PageHeader';
import PageShell from '../components/ui/PageShell';

const PAGE_SIZE = 20;

function apiMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function uploaderLabel(item: DocumentHistoryItem): string {
  if (item.uploadedByUsername && item.uploadedByEmail) {
    return `${item.uploadedByUsername} (${item.uploadedByEmail})`;
  }
  if (item.uploadedByUsername) return item.uploadedByUsername;
  if (item.uploadedByEmail) return item.uploadedByEmail;
  return item.uploadedByUserId == null ? 'Unknown user' : `User #${item.uploadedByUserId}`;
}

function statusLabel(status: DocumentStatus): string {
  switch (status) {
    case 'PARSING':
      return 'Parsing';
    case 'NEEDS_REVIEW':
      return 'Needs review';
    case 'VALIDATED':
      return 'Validated';
    case 'REJECTED':
      return 'Rejected';
    case 'ARCHIVED':
      return 'Archived';
    case 'FAILED':
    case 'PARSING_FAILED':
      return 'Parsing failed';
    default:
      return status;
  }
}

export default function DocumentHistoryPage() {
  const { role, isSuperAdmin } = useAuth();
  const [items, setItems] = useState<DocumentHistoryItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [exportingPdf, setExportingPdf] = useState(false);

  const title = useMemo(() => {
    if (role === 'SUPERADMIN') return 'All Document History';
    if (role === 'BUSINESS_ADMIN') return 'Organization Document History';
    return 'My Documents';
  }, [role]);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    listDocumentHistory(page, PAGE_SIZE)
      .then((res) => {
        setItems(res.data.items);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      })
      .catch((err: unknown) => setError(apiMessage(err, 'Failed to load document history.')))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => load(), 0);
    return () => window.clearTimeout(timeoutId);
  }, [load]);

  const handleDownload = async (item: DocumentHistoryItem) => {
    setDownloadingId(item.documentId);
    try {
      const res = await downloadDocument(item.documentId);
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = item.originalFileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err: unknown) {
      setError(apiMessage(err, 'Download failed.'));
    } finally {
      setDownloadingId(null);
    }
  };

  const handleExportPdf = async () => {
    setExportingPdf(true);
    setError(null);
    try {
      const res = await exportDocumentHistoryPdf();
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = `document-history-${new Date().toISOString().slice(0, 10)}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err: unknown) {
      setError(apiMessage(err, 'PDF export failed.'));
    } finally {
      setExportingPdf(false);
    }
  };

  const canGoBack = page > 0;
  const canGoNext = page + 1 < totalPages;

  return (
    <PageShell className="document-history-page">
      <PageHeader
        title={title}
        description={`${totalElements} uploaded document${totalElements === 1 ? '' : 's'}`}
        actions={(
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => void handleExportPdf()}
              disabled={exportingPdf || loading}
            >
              {exportingPdf ? 'Exporting...' : 'Export PDF'}
            </button>
            <Link to="/vehicles" className="btn btn-secondary">Vehicles</Link>
          </>
        )}
      />

      {loading && <DataState type="loading">Loading document history...</DataState>}
      {!loading && error && <DataState type="error">{error}</DataState>}
      {!loading && !error && items.length === 0 && (
        <DataState>No uploaded documents found.</DataState>
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <div className="history-table-wrapper">
            <table className="history-table">
              <thead>
                <tr>
                  <th>Uploaded</th>
                  <th>Uploader</th>
                  <th>File</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Vehicle</th>
                  {isSuperAdmin && <th>Organization</th>}
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.documentId}>
                    <td data-label="Uploaded">{formatDateTime(item.uploadedAt)}</td>
                    <td data-label="Uploader">{uploaderLabel(item)}</td>
                    <td data-label="File">
                      <span className="history-file-name">{item.originalFileName}</span>
                      <span className="history-file-meta">{formatBytes(item.fileSize)} · {item.contentType}</span>
                    </td>
                    <td data-label="Type">
                      {item.documentType}
                      {item.documentSubtype ? ` / ${item.documentSubtype}` : ''}
                    </td>
                    <td data-label="Status">
                      <span className={`status-badge status-${item.status}`}>
                        {statusLabel(item.status)}
                      </span>
                    </td>
                    <td data-label="Vehicle">
                      <Link to={`/vehicles/${item.vehicleId}`}>Vehicle #{item.vehicleId}</Link>
                    </td>
                    {isSuperAdmin && (
                      <td data-label="Organization">{item.businessId ?? '-'}</td>
                    )}
                    <td data-label="">
                      <button
                        type="button"
                        className="btn btn-sm"
                        onClick={() => void handleDownload(item)}
                        disabled={downloadingId === item.documentId}
                      >
                        {downloadingId === item.documentId ? 'Downloading...' : 'Download'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="history-pagination">
            <button
              type="button"
              className="btn btn-secondary"
              disabled={!canGoBack}
              onClick={() => setPage((current) => Math.max(current - 1, 0))}
            >
              Previous
            </button>
            <span>
              Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
            </span>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={!canGoNext}
              onClick={() => setPage((current) => current + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}
    </PageShell>
  );
}
