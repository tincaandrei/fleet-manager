import { useEffect, useState, useRef } from 'react';
import type { DocumentResponse, DocumentType } from '../types/document';
import {
  listDocumentsByVehicle,
  uploadDocument,
  downloadDocument,
  reviewDocument,
  archiveDocument,
} from '../api/documentApi';
import { useAuth } from '../auth/AuthContext';

const DOC_TYPES: DocumentType[] = ['INSPECTION', 'INSURANCE', 'INVOICE', 'REGISTRATION', 'OTHER'];

interface ReviewState {
  docId: string;
  decision: 'APPROVE' | 'REJECT';
  approvedDataJson: string;
  comment: string;
  loading: boolean;
  error: string | null;
}

interface Props {
  vehicleId: number;
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function apiMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

export default function VehicleDocumentsSection({ vehicleId }: Props) {
  const { role, isAdmin } = useAuth();
  const canReview = role === 'STAFF' || isAdmin;

  const [docs, setDocs] = useState<DocumentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadDocType, setUploadDocType] = useState<DocumentType | ''>('');
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [reviewState, setReviewState] = useState<ReviewState | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    listDocumentsByVehicle(vehicleId)
      .then((res) => setDocs(res.data))
      .catch((err: unknown) => setError(apiMessage(err, 'Failed to load documents.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, [vehicleId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    setUploadError(null);
    if (!uploadFile) {
      setUploadError('Please select a PDF file.');
      return;
    }
    if (!uploadFile.name.toLowerCase().endsWith('.pdf') && uploadFile.type !== 'application/pdf') {
      setUploadError('Only PDF files are accepted.');
      return;
    }
    if (!uploadDocType) {
      setUploadError('Please select a document type.');
      return;
    }
    setUploading(true);
    try {
      await uploadDocument(vehicleId, uploadDocType, uploadFile);
      setUploadFile(null);
      setUploadDocType('');
      if (fileInputRef.current) fileInputRef.current.value = '';
      load();
    } catch (err: unknown) {
      setUploadError(apiMessage(err, 'Upload failed.'));
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (doc: DocumentResponse) => {
    setActionError(null);
    try {
      const res = await downloadDocument(doc.id);
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.originalFileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch {
      setActionError('Download failed.');
    }
  };

  const startReview = (doc: DocumentResponse, decision: 'APPROVE' | 'REJECT') => {
    setReviewState({
      docId: doc.id,
      decision,
      approvedDataJson:
        decision === 'APPROVE'
          ? JSON.stringify(doc.extraction?.rawExtractedData ?? {}, null, 2)
          : '',
      comment: '',
      loading: false,
      error: null,
    });
  };

  const cancelReview = () => setReviewState(null);

  const submitReview = async () => {
    if (!reviewState) return;
    setReviewState((s) => (s ? { ...s, loading: true, error: null } : null));
    try {
      if (reviewState.decision === 'APPROVE') {
        let parsedData: Record<string, unknown>;
        try {
          parsedData = JSON.parse(reviewState.approvedDataJson) as Record<string, unknown>;
        } catch {
          setReviewState((s) =>
            s ? { ...s, loading: false, error: 'Approved data is not valid JSON.' } : null,
          );
          return;
        }
        await reviewDocument(reviewState.docId, {
          decision: 'APPROVE',
          approvedData: parsedData,
          comment: reviewState.comment || undefined,
        });
      } else {
        await reviewDocument(reviewState.docId, {
          decision: 'REJECT',
          comment: reviewState.comment || undefined,
        });
      }
      setReviewState(null);
      load();
    } catch (err: unknown) {
      setReviewState((s) =>
        s ? { ...s, loading: false, error: apiMessage(err, 'Review failed.') } : null,
      );
    }
  };

  const handleArchive = async (doc: DocumentResponse) => {
    if (!window.confirm(`Archive "${doc.originalFileName}"? This cannot be undone.`)) return;
    setActionError(null);
    try {
      await archiveDocument(doc.id);
      load();
    } catch (err: unknown) {
      setActionError(apiMessage(err, 'Archive failed.'));
    }
  };

  return (
    <section className="documents-section">
      {/* Upload form */}
      <form onSubmit={handleUpload} className="doc-upload-form">
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,application/pdf"
          onChange={(e) => setUploadFile(e.target.files?.[0] ?? null)}
        />
        <select
          value={uploadDocType}
          onChange={(e) => setUploadDocType(e.target.value as DocumentType | '')}
        >
          <option value="">Select type…</option>
          {DOC_TYPES.map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
        <button type="submit" disabled={uploading}>
          {uploading ? 'Uploading…' : 'Upload PDF'}
        </button>
        {uploadError && <p className="error doc-upload-error">{uploadError}</p>}
      </form>

      {actionError && (
        <p className="error" style={{ marginBottom: '0.75rem' }}>
          {actionError}
        </p>
      )}

      {loading && <p className="doc-empty">Loading documents…</p>}
      {!loading && error && <p className="error">{error}</p>}
      {!loading && !error && docs.length === 0 && (
        <p className="doc-empty">No documents attached to this vehicle yet.</p>
      )}

      <div className="doc-list">
        {docs.map((doc) => {
          const reviewing = reviewState?.docId === doc.id ? reviewState : null;
          return (
            <div key={doc.id} className="doc-card">
              {/* Header: filename + type badge + status badge */}
              <div className="doc-card-header">
                <span className="doc-filename">{doc.originalFileName}</span>
                <div className="doc-badges">
                  <span className="badge">{doc.documentType}</span>
                  <span className={`status-badge status-${doc.status}`}>{doc.status}</span>
                </div>
              </div>

              {/* Meta */}
              <div className="doc-card-meta">
                <span>Uploaded: {new Date(doc.createdAt).toLocaleString()}</span>
                <span>Size: {formatBytes(doc.fileSize)}</span>
              </div>

              {/* Parser error (FAILED_PARSING only) */}
              {doc.status === 'FAILED_PARSING' && doc.extraction?.errorMessage && (
                <p className="doc-error-msg">Parser error: {doc.extraction.errorMessage}</p>
              )}

              {/* Approved data */}
              {doc.approvedData && (
                <div className="doc-approved-data">
                  <strong>Approved data</strong>
                  <pre>{JSON.stringify(doc.approvedData.approvedData, null, 2)}</pre>
                </div>
              )}

              {/* Action buttons */}
              <div className="doc-card-actions">
                <button type="button" className="btn" onClick={() => handleDownload(doc)}>
                  Download
                </button>

                {canReview && doc.status === 'NEEDS_REVIEW' && !reviewing && (
                  <>
                    <button
                      type="button"
                      className="btn btn-success"
                      onClick={() => startReview(doc, 'APPROVE')}
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger"
                      onClick={() => startReview(doc, 'REJECT')}
                    >
                      Reject
                    </button>
                  </>
                )}

                {isAdmin && doc.status !== 'ARCHIVED' && (
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => handleArchive(doc)}
                  >
                    Archive
                  </button>
                )}
              </div>

              {/* Inline review form */}
              {reviewing && (
                <div className="doc-review-form">
                  {reviewing.decision === 'APPROVE' ? (
                    <>
                      <label>
                        Approved data (JSON — edit before submitting)
                        <textarea
                          rows={10}
                          value={reviewing.approvedDataJson}
                          onChange={(e) =>
                            setReviewState((s) =>
                              s ? { ...s, approvedDataJson: e.target.value } : null,
                            )
                          }
                        />
                      </label>
                      <label>
                        Comment (optional)
                        <input
                          type="text"
                          value={reviewing.comment}
                          onChange={(e) =>
                            setReviewState((s) => (s ? { ...s, comment: e.target.value } : null))
                          }
                        />
                      </label>
                    </>
                  ) : (
                    <label>
                      Reject reason (optional)
                      <input
                        type="text"
                        value={reviewing.comment}
                        onChange={(e) =>
                          setReviewState((s) => (s ? { ...s, comment: e.target.value } : null))
                        }
                      />
                    </label>
                  )}

                  {reviewing.error && <p className="error">{reviewing.error}</p>}

                  <div className="doc-review-buttons">
                    <button
                      type="button"
                      className={`btn ${reviewing.decision === 'APPROVE' ? 'btn-success' : 'btn-danger'}`}
                      onClick={submitReview}
                      disabled={reviewing.loading}
                    >
                      {reviewing.loading
                        ? 'Submitting…'
                        : reviewing.decision === 'APPROVE'
                          ? 'Confirm Approval'
                          : 'Confirm Rejection'}
                    </button>
                    <button type="button" className="btn btn-secondary" onClick={cancelReview}>
                      Cancel
                    </button>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}
