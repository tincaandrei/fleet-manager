import { useEffect, useState } from 'react';
import { getDocumentInfoFolder } from '../api/documentApi';
import type { DocumentInfoFolder } from '../types/document';

interface Props {
  documentId: string;
  onClose: () => void;
}

function FieldTable({ data, title }: { data: Record<string, unknown>; title: string }) {
  const entries = Object.entries(data).filter(([, v]) => v !== null && v !== undefined && v !== '');
  if (entries.length === 0) return <p className="doc-empty">{title}: no fields extracted.</p>;

  return (
    <div className="info-folder-section">
      <h4 className="info-folder-section-title">{title}</h4>
      <table className="info-folder-table">
        <tbody>
          {entries.map(([key, value]) => (
            <tr key={key}>
              <th>{key}</th>
              <td>{typeof value === 'object' ? JSON.stringify(value) : String(value)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function DocumentInfoFolderPanel({ documentId, onClose }: Props) {
  const [folder, setFolder] = useState<DocumentInfoFolder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setLoading(true);
      setError(null);
      getDocumentInfoFolder(documentId)
        .then((res) => setFolder(res.data))
        .catch((err: unknown) => {
          const e = err as { response?: { data?: { message?: string } } };
          setError(e?.response?.data?.message ?? 'Failed to load info folder.');
        })
        .finally(() => setLoading(false));
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [documentId]);

  return (
    <div className="info-folder-panel">
      <div className="info-folder-header">
        <span className="info-folder-title">Info Folder</span>
        <button
          type="button"
          className="info-folder-close"
          aria-label="Close info folder"
          onClick={onClose}
        >
          ✕
        </button>
      </div>

      {loading && <p className="doc-empty">Loading info folder…</p>}
      {!loading && error && <p className="error">{error}</p>}

      {!loading && !error && folder && (
        <>
          <FieldTable
            title="Key Fields"
            data={folder.canonicalFields ?? {}}
          />
          <FieldTable
            title="Additional Extracted Metadata"
            data={folder.extraFields ?? {}}
          />
        </>
      )}
    </div>
  );
}
