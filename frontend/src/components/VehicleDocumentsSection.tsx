import { useCallback, useEffect, useState, useRef } from 'react';
import type { DocumentResponse, DocumentExtractionResponse } from '../types/document';
import {
  listDocumentsByVehicle,
  uploadDocument,
  downloadDocument,
  reviewDocument,
  archiveDocument,
} from '../api/documentApi';
import { useAuth } from '../auth/AuthContext';
import DocumentInfoFolderPanel from './DocumentInfoFolder';
import { showToast } from '../utils/toast';

// ── Review form state ─────────────────────────────────────────────────────────

/**
 * Structured fields shown in the approval form.
 * All values are kept as strings so they bind cleanly to <input type="text">.
 */
interface ReviewFields {
  documentType: string;
  subtype: string;
  licensePlate: string;
  vin: string;
  validFrom: string;
  validUntil: string;
  policyNumber: string;
  inspectionNumber: string;
  invoiceNumber: string;
  category: string;
  issuer: string;
  transactionId: string;
  totalAmount: string;
  amount: string;
  currency: string;
  expenseCategory: string;
}

interface ReviewState {
  docId: string;
  decision: 'APPROVE' | 'REJECT';
  fields: ReviewFields;
  comment: string;
  loading: boolean;
  error: string | null;
}

type UploadNotice = {
  type: 'info' | 'success' | 'error';
  message: string;
};

// ── Helpers ───────────────────────────────────────────────────────────────────

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

/** Safely coerce an unknown extraction value to a display string. */
function str(val: unknown): string {
  if (val === null || val === undefined) return '';
  if (typeof val === 'string') return val;
  if (typeof val === 'number' || typeof val === 'boolean') return String(val);
  return '';
}

function dateOnly(val: unknown): string {
  const value = str(val).trim();
  return /^\d{4}-\d{2}-\d{2}/.test(value) ? value.slice(0, 10) : value;
}

/**
 * Pre-populate ReviewFields from parser extraction data.
 */
function fieldsFromExtraction(
  extraction: DocumentExtractionResponse | null | undefined,
): ReviewFields {
  const data = extraction?.extractedData ?? {};
  return {
    documentType: str(data['documentType']) || extraction?.detectedDocumentType || '',
    subtype: str(data['subtype']) || extraction?.detectedSubtype || '',
    licensePlate: str(data['licensePlate']),
    vin: str(data['vin']),
    validFrom: dateOnly(data['validFrom']),
    validUntil: dateOnly(data['validUntil']),
    policyNumber: str(data['policyNumber']),
    inspectionNumber: str(data['inspectionNumber']),
    invoiceNumber: str(data['invoiceNumber']),
    category: str(data['category']),
    issuer: str(data['issuer']),
    transactionId: str(data['transactionId']),
    totalAmount: str(data['totalAmount']),
    amount: str(data['amount']),
    currency: str(data['currency']),
    expenseCategory: str(data['expenseCategory']),
  };
}

function reviewStateFor(doc: DocumentResponse, decision: 'APPROVE' | 'REJECT'): ReviewState {
  return {
    docId: doc.id,
    decision,
    fields: fieldsFromExtraction(doc.extraction),
    comment: '',
    loading: false,
    error: null,
  };
}

function parserFailureMessage(doc: DocumentResponse): string {
  return doc.extraction?.errorMessage ?? 'Automatic parsing failed. The uploaded PDF is stored, but no structured data could be extracted.';
}

/**
 * Build the approvedData payload from structured form fields.
 * Omits fields that are blank strings.
 */
function buildApprovedData(fields: ReviewFields): Record<string, unknown> {
  const obj: Record<string, unknown> = {};
  const add = (k: string, v: string) => { if (v.trim()) obj[k] = v.trim(); };
  add('documentType', fields.documentType);
  add('subtype', fields.subtype);
  add('licensePlate', fields.licensePlate);
  add('vin', fields.vin);
  add('validFrom', fields.validFrom);
  add('validUntil', fields.validUntil);
  add('policyNumber', fields.policyNumber);
  add('inspectionNumber', fields.inspectionNumber);
  add('invoiceNumber', fields.invoiceNumber);
  add('category', fields.category);
  add('issuer', fields.issuer);
  add('transactionId', fields.transactionId);
  add('totalAmount', fields.totalAmount);
  add('amount', fields.amount);
  add('currency', fields.currency);
  add('expenseCategory', fields.expenseCategory);
  return obj;
}

function formatExtractedValue(value: unknown): string {
  if (value === null || value === undefined || value === '') return '-';
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return JSON.stringify(value);
}

function humanizeKey(key: string): string {
  return key
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function ExtractedDataPreview({ extraction }: { extraction: DocumentExtractionResponse | null | undefined }) {
  const data = extraction?.extractedData;
  if (!data || Object.keys(data).length === 0) {
    return null;
  }

  const priority = [
    'documentType',
    'subtype',
    'licensePlate',
    'vin',
    'validFrom',
    'validUntil',
    'category',
    'issuer',
    'transactionId',
    'amount',
    'currency',
    'invoiceNumber',
    'invoiceDate',
    'supplierName',
    'totalAmount',
    'expenseCategory',
  ];
  const keys = [
    ...priority.filter((key) => Object.prototype.hasOwnProperty.call(data, key)),
    ...Object.keys(data).filter((key) => !priority.includes(key)),
  ];

  return (
    <div className="extracted-preview">
      <strong>Extracted parser data</strong>
      <div className="extracted-preview-grid">
        {keys.map((key) => (
          <div key={key} className="extracted-preview-row">
            <span>{humanizeKey(key)}</span>
            <code>{formatExtractedValue(data[key])}</code>
          </div>
        ))}
      </div>
    </div>
  );
}

function ParsedDocumentSummary({ extraction }: { extraction: DocumentExtractionResponse | null | undefined }) {
  const data = extraction?.extractedData;
  if (!data || Object.keys(data).length === 0) {
    return null;
  }

  const summary = [
    ['License plate', str(data['licensePlate'])],
    ['VIN', str(data['vin'])],
    ['Valid from', dateOnly(data['validFrom'])],
    ['Valid until', dateOnly(data['validUntil'])],
    ['Category', str(data['category'])],
    ['Issuer', str(data['issuer'])],
  ].filter(([, value]) => value);

  if (summary.length === 0) {
    return null;
  }

  return (
    <div className="doc-extracted-summary">
      {summary.map(([label, value]) => (
        <span key={label}>
          <strong>{label}:</strong> {value}
        </span>
      ))}
    </div>
  );
}

// ── Review form sub-component ─────────────────────────────────────────────────

interface ReviewFormProps {
  state: ReviewState;
  extraction: DocumentExtractionResponse | null | undefined;
  onChange: (patch: Partial<ReviewFields>) => void;
  onCommentChange: (c: string) => void;
  onSubmit: () => void;
  onCancel: () => void;
}

function ReviewForm({
  state,
  extraction,
  onChange,
  onCommentChange,
  onSubmit,
  onCancel,
}: ReviewFormProps) {
  const f = state.fields;
  const field = (label: string, key: keyof ReviewFields, placeholder?: string) => (
    <label className="review-field-label">
      {label}
      <input
        type="text"
        value={f[key]}
        placeholder={placeholder ?? ''}
        onChange={(e) => onChange({ [key]: e.target.value })}
      />
    </label>
  );

  return (
    <div className="doc-review-form">
      {state.decision === 'APPROVE' ? (
        <>
          <ExtractedDataPreview extraction={extraction} />

          <div className="doc-review-fields">
            {field('Document type', 'documentType')}
            {field('Subtype', 'subtype', 'e.g. RCA, ITP, ROVINIETA')}
            {field('License plate', 'licensePlate')}
            {field('VIN', 'vin')}
            {field('Valid from', 'validFrom', 'YYYY-MM-DD')}
            {field('Valid until', 'validUntil', 'YYYY-MM-DD')}
            {field('Category', 'category')}
            {field('Issuer', 'issuer')}
            {field('Transaction ID', 'transactionId')}
            {field('Amount', 'amount')}
            {field('Currency', 'currency')}
            {field('Policy number', 'policyNumber')}
            {field('Inspection number', 'inspectionNumber')}
            {field('Invoice number', 'invoiceNumber')}
            {field('Total amount', 'totalAmount')}
            {field('Expense category', 'expenseCategory')}
          </div>

          <label className="review-field-label">
            Comment (optional)
            <input
              type="text"
              value={state.comment}
              onChange={(e) => onCommentChange(e.target.value)}
            />
          </label>

          {extraction && (
            <details className="raw-parser">
              <summary>Raw parser data</summary>
              <pre>{JSON.stringify(extraction, null, 2)}</pre>
            </details>
          )}
        </>
      ) : (
        <label className="review-field-label">
          Reject reason (optional)
          <input
            type="text"
            value={state.comment}
            onChange={(e) => onCommentChange(e.target.value)}
          />
        </label>
      )}

      {state.error && <p className="error">{state.error}</p>}

      <div className="doc-review-buttons">
        <button
          type="button"
          className={`btn ${state.decision === 'APPROVE' ? 'btn-success' : 'btn-danger'}`}
          onClick={onSubmit}
          disabled={state.loading}
        >
          {state.loading
            ? 'Submitting…'
            : state.decision === 'APPROVE'
              ? 'Confirm Approval'
              : 'Confirm Rejection'}
        </button>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </div>
  );
}

// ── Extraction badges ─────────────────────────────────────────────────────────

function ExtractionBadges({ extraction }: { extraction: DocumentExtractionResponse }) {
  return (
    <div className="extraction-badges">
      {extraction.confidence !== null && (
        <span className="badge badge-confidence">
          Confidence {(extraction.confidence * 100).toFixed(0)}%
        </span>
      )}
      {extraction.warnings?.map((w, i) => (
        <span key={i} className="badge badge-warning" title={w}>
          ⚠ {w.length > 40 ? `${w.slice(0, 40)}…` : w}
        </span>
      ))}
      {!extraction.extractedData?.validUntil && (
        <span className="badge badge-warning">Missing validUntil</span>
      )}
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────────────────

export default function VehicleDocumentsSection({ vehicleId }: Props) {
  const { isSuperAdmin, isBusinessAdmin } = useAuth();
  const canReview = isSuperAdmin || isBusinessAdmin;
  const canArchive = isSuperAdmin || isBusinessAdmin;

  const [docs, setDocs] = useState<DocumentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadNotice, setUploadNotice] = useState<UploadNotice | null>(null);
  const [trackedUploadId, setTrackedUploadId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [reviewState, setReviewState] = useState<ReviewState | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  /** The document ID whose info folder is currently open, or null. */
  const [infoFolderDocId, setInfoFolderDocId] = useState<string | null>(null);

  const load = useCallback((showSpinner = true) => {
    if (showSpinner) {
      setLoading(true);
      setError(null);
    }
    return listDocumentsByVehicle(vehicleId)
      .then((res) => {
        setDocs(res.data);
        return res.data;
      })
      .catch((err: unknown) => {
        if (showSpinner) {
          setError(apiMessage(err, 'Failed to load documents.'));
        }
        return [];
      })
      .finally(() => {
        if (showSpinner) {
          setLoading(false);
        }
      });
  }, [vehicleId]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!docs.some((doc) => doc.status === 'PARSING')) return undefined;
    const intervalId = window.setInterval(() => {
      void load(false);
    }, 3000);
    return () => window.clearInterval(intervalId);
  }, [docs, load]);

  useEffect(() => {
    if (!trackedUploadId) return;
    const uploadedDoc = docs.find((doc) => doc.id === trackedUploadId);
    if (!uploadedDoc || uploadedDoc.status === 'PARSING') return;

    if (uploadedDoc.status === 'PARSING_FAILED') {
      const message = `Document uploaded, but parser failed: ${parserFailureMessage(uploadedDoc)}`;
      setUploadNotice({ type: 'error', message });
      showToast({ type: 'error', message, durationMs: 7000 });
      if (canReview) {
        setReviewState(reviewStateFor(uploadedDoc, 'REJECT'));
      }
    } else if (uploadedDoc.status === 'NEEDS_REVIEW') {
      const message = canReview
        ? 'Document uploaded and parsed. Review the extracted fields before approval.'
        : 'Document uploaded and is waiting for administrator review.';
      setUploadNotice({ type: 'success', message });
      showToast({ type: 'success', message });
      if (canReview) {
        setReviewState(reviewStateFor(uploadedDoc, 'APPROVE'));
      }
    } else {
      setUploadNotice({ type: 'success', message: 'Document processing finished.' });
    }
    setTrackedUploadId(null);
  }, [canReview, docs, trackedUploadId]);

  // ── Upload ──────────────────────────────────────────────────────────────────

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFile) {
      setUploadNotice({ type: 'error', message: 'Please select a PDF file.' });
      showToast({ type: 'error', message: 'Please select a PDF file.' });
      return;
    }
    if (!uploadFile.name.toLowerCase().endsWith('.pdf') && uploadFile.type !== 'application/pdf') {
      setUploadNotice({ type: 'error', message: 'Only PDF files are accepted.' });
      showToast({ type: 'error', message: 'Only PDF files are accepted.' });
      return;
    }
    setUploading(true);
    setActionError(null);
    setUploadNotice({ type: 'info', message: 'Uploading document...' });
    try {
      const uploaded = (await uploadDocument(vehicleId, uploadFile)).data;
      setUploadFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';

      if (uploaded.status === 'NEEDS_REVIEW') {
        showToast({
          type: 'success',
          message: canReview
            ? 'Document uploaded and parsed. Review the extracted fields before approval.'
            : 'Document uploaded and is waiting for administrator review.',
        });
        setUploadNotice({
          type: 'success',
          message: canReview
            ? 'Document uploaded and parsed. Review the extracted fields before approval.'
            : 'Document uploaded and is waiting for administrator review.',
        });
        if (canReview) {
          setReviewState(reviewStateFor(uploaded, 'APPROVE'));
        }
      } else if (uploaded.status === 'PARSING_FAILED') {
        const parserMessage = parserFailureMessage(uploaded);
        showToast({
          type: 'error',
          message: `Document uploaded, but parser failed: ${parserMessage}`,
          durationMs: 7000,
        });
        setUploadNotice({
          type: 'error',
          message: `Document uploaded, but parser failed: ${parserMessage}`,
        });
        if (canReview) {
          setReviewState(reviewStateFor(uploaded, 'REJECT'));
        }
      } else if (uploaded.status === 'PARSING') {
        setTrackedUploadId(uploaded.id);
        setUploadNotice({
          type: 'info',
          message: 'Document uploaded. Automatic parsing is running; this list will update when it finishes.',
        });
      } else {
        showToast({ type: 'success', message: 'Document uploaded successfully.' });
        setUploadNotice({ type: 'success', message: 'Document uploaded successfully.' });
      }

      load();
    } catch (err: unknown) {
      // Toast is shown by the axios interceptor; do NOT logout — just surface the error.
      const msg = apiMessage(err, 'Upload failed. Please try again.');
      setUploadNotice({ type: 'error', message: msg });
      showToast({ type: 'error', message: msg });
    } finally {
      setUploading(false);
    }
  };

  // ── Download ────────────────────────────────────────────────────────────────

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

  // ── Review ──────────────────────────────────────────────────────────────────

  const startReview = (doc: DocumentResponse, decision: 'APPROVE' | 'REJECT') => {
    setReviewState(reviewStateFor(doc, decision));
  };

  const cancelReview = () => setReviewState(null);

  const patchFields = (patch: Partial<ReviewFields>) =>
    setReviewState((s) => s ? { ...s, fields: { ...s.fields, ...patch } } : null);

  const submitReview = async () => {
    if (!reviewState) return;
    setReviewState((s) => s ? { ...s, loading: true, error: null } : null);
    try {
      if (reviewState.decision === 'APPROVE') {
        await reviewDocument(reviewState.docId, {
          decision: 'APPROVE',
          approvedData: buildApprovedData(reviewState.fields),
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

  // ── Archive ─────────────────────────────────────────────────────────────────

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

  // ── Render ──────────────────────────────────────────────────────────────────

  return (
    <section className="documents-section">
      {/* ── Upload form (no document type selector — parser decides) ── */}
      <form onSubmit={handleUpload} className="doc-upload-form">
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,application/pdf"
          onChange={(e) => setUploadFile(e.target.files?.[0] ?? null)}
        />
        <button type="submit" disabled={uploading}>
          {uploading ? 'Uploading…' : 'Upload PDF'}
        </button>
      </form>

      {uploadNotice && (
        <div className={`doc-flow-notice doc-flow-notice-${uploadNotice.type}`} role="status">
          {uploadNotice.message}
        </div>
      )}

      {actionError && (
        <p className="error" style={{ marginBottom: '0.75rem' }}>{actionError}</p>
      )}

      {loading && <p className="doc-empty">Loading documents…</p>}
      {!loading && error && <p className="error">{error}</p>}
      {!loading && !error && docs.length === 0 && (
        <p className="doc-empty">No documents attached to this vehicle yet.</p>
      )}

      {/* Info folder panel */}
      {infoFolderDocId && (
        <DocumentInfoFolderPanel
          documentId={infoFolderDocId}
          onClose={() => setInfoFolderDocId(null)}
        />
      )}

      <div className="doc-list">
        {docs.map((doc) => {
          const reviewing = reviewState?.docId === doc.id ? reviewState : null;
          return (
            <div key={doc.id} className="doc-card">
              {/* Header */}
              <div className="doc-card-header">
                <span className="doc-filename">{doc.originalFileName}</span>
                <div className="doc-badges">
                  {doc.documentType && (
                    <span className="badge">{doc.documentType}</span>
                  )}
                  <span className={`status-badge status-${doc.status}`}>{doc.status}</span>
                </div>
              </div>

              {/* Extraction confidence/warning badges */}
              {doc.extraction && doc.extraction.parserStatus !== 'FAILED' && (
                <ExtractionBadges extraction={doc.extraction} />
              )}

              {doc.extraction?.parserStatus === 'PARSED' && (
                <ParsedDocumentSummary extraction={doc.extraction} />
              )}

              {doc.status === 'PARSING' && (
                <p className="doc-info-msg">
                  Automatic parsing is running. This page updates automatically.
                </p>
              )}

              {/* Parser error */}
              {(doc.status === 'PARSING_FAILED' || doc.extraction?.parserStatus === 'FAILED') && (
                <p className="doc-error-msg">
                  Automatic parsing failed: {parserFailureMessage(doc)}
                </p>
              )}

              {/* Meta */}
              <div className="doc-card-meta">
                <span>Uploaded: {new Date(doc.createdAt).toLocaleString()}</span>
                <span>Size: {formatBytes(doc.fileSize)}</span>
              </div>

              {/* Approved data summary */}
              {doc.approvedData && (
                <div className="doc-approved-data">
                  <strong>Approved data</strong>
                  <div className="doc-approved-fields">
                    {doc.approvedData.documentType && (
                      <span>Type: {doc.approvedData.documentType}
                        {doc.approvedData.subtype ? ` / ${doc.approvedData.subtype}` : ''}
                      </span>
                    )}
                    {doc.approvedData.validFrom && (
                      <span>Valid from: {doc.approvedData.validFrom}</span>
                    )}
                    {doc.approvedData.validUntil && (
                      <span>Valid until: {doc.approvedData.validUntil}</span>
                    )}
                  </div>
                  <details className="raw-parser" style={{ marginTop: '0.4rem' }}>
                    <summary>Full approved data</summary>
                    <pre>{JSON.stringify(doc.approvedData.approvedData, null, 2)}</pre>
                  </details>
                </div>
              )}

              {/* Action buttons */}
              <div className="doc-card-actions">
                <button type="button" className="btn" onClick={() => handleDownload(doc)}>
                  Download
                </button>

                {/* Info folder — available once parsing has produced some output */}
                {(doc.status === 'VALIDATED' ||
                  doc.status === 'NEEDS_REVIEW' ||
                  doc.status === 'PARSING_FAILED' ||
                  doc.approvedData != null) && (
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() =>
                      setInfoFolderDocId((prev) => (prev === doc.id ? null : doc.id))
                    }
                  >
                    {infoFolderDocId === doc.id ? 'Hide Info' : 'Info Folder'}
                  </button>
                )}

                {canReview && (doc.status === 'NEEDS_REVIEW' || doc.status === 'PARSING_FAILED') && !reviewing && (
                  <>
                    {doc.status === 'NEEDS_REVIEW' && (
                      <button
                        type="button"
                        className="btn btn-success"
                        onClick={() => startReview(doc, 'APPROVE')}
                      >
                        Review
                      </button>
                    )}
                    <button
                      type="button"
                      className="btn btn-danger"
                      onClick={() => startReview(doc, 'REJECT')}
                    >
                      Reject
                    </button>
                  </>
                )}

                {canArchive && doc.status !== 'ARCHIVED' && (
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
                <ReviewForm
                  state={reviewing}
                  extraction={doc.extraction}
                  onChange={patchFields}
                  onCommentChange={(c) =>
                    setReviewState((s) => (s ? { ...s, comment: c } : null))
                  }
                  onSubmit={submitReview}
                  onCancel={cancelReview}
                />
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}
