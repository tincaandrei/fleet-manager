import { useState } from 'react';
import { exportDocumentHistoryPdf, exportVehicleCostsExcel } from '../../api/documentApi';
import { Button } from '../ui/Button';
import DataState from '../ui/DataState';
import { getApiErrorMessage } from '../../utils/apiError';
import { saveBlob } from '../../utils/download';

interface OrgReportsPanelProps {
  /** Selected organization; when omitted, only global downloads are offered. */
  businessId?: number;
  businessName?: string | null;
}

/**
 * Report downloads for the superadmin console: organization-scoped exports
 * (sending businessId) and global exports (omitting it).
 */
export default function OrgReportsPanel({ businessId, businessName }: OrgReportsPanelProps) {
  const [busyKey, setBusyKey] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const today = () => new Date().toISOString().slice(0, 10);

  const run = async (key: string, task: () => Promise<void>) => {
    setBusyKey(key);
    setError(null);
    try {
      await task();
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Export failed.'));
    } finally {
      setBusyKey(null);
    }
  };

  const downloadHistoryPdf = (scopedBusinessId?: number) => async () => {
    const res = await exportDocumentHistoryPdf(scopedBusinessId);
    const suffix = scopedBusinessId ? `org-${scopedBusinessId}-` : '';
    saveBlob(res.data, `document-history-${suffix}${today()}.pdf`);
  };

  const downloadCostsExcel = (scopedBusinessId?: number) => async () => {
    const res = await exportVehicleCostsExcel(scopedBusinessId);
    const suffix = scopedBusinessId ? `org-${scopedBusinessId}-` : '';
    saveBlob(res.data, `fleet-costs-${suffix}${today()}.xlsx`);
  };

  return (
    <div className="org-reports-panel">
      {error && <DataState type="error">{error}</DataState>}

      {businessId != null && (
        <section className="management-section">
          <div className="section-header">
            <h2>{businessName ? `${businessName} reports` : 'Organization reports'}</h2>
            <span>Scoped to the selected organization</span>
          </div>
          <div className="report-actions">
            <Button
              variant="secondary"
              disabled={busyKey !== null}
              onClick={() => void run('org-pdf', downloadHistoryPdf(businessId))}
            >
              {busyKey === 'org-pdf' ? 'Exporting...' : 'Document history (PDF)'}
            </Button>
            <Button
              variant="secondary"
              disabled={busyKey !== null}
              onClick={() => void run('org-xlsx', downloadCostsExcel(businessId))}
            >
              {busyKey === 'org-xlsx' ? 'Exporting...' : 'Vehicle costs (Excel)'}
            </Button>
          </div>
        </section>
      )}

      <section className="management-section">
        <div className="section-header">
          <h2>Global reports</h2>
          <span>All organizations</span>
        </div>
        <div className="report-actions">
          <Button
            variant="secondary"
            disabled={busyKey !== null}
            onClick={() => void run('all-pdf', downloadHistoryPdf(undefined))}
          >
            {busyKey === 'all-pdf' ? 'Exporting...' : 'Document history (PDF)'}
          </Button>
          <Button
            variant="secondary"
            disabled={busyKey !== null}
            onClick={() => void run('all-xlsx', downloadCostsExcel(undefined))}
          >
            {busyKey === 'all-xlsx' ? 'Exporting...' : 'Vehicle costs (Excel)'}
          </Button>
        </div>
      </section>
    </div>
  );
}
