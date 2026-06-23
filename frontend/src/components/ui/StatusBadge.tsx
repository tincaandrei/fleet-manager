import type { ReactNode } from 'react';

interface StatusBadgeProps {
  status: string;
  children?: ReactNode;
  tone?: string;
}

export default function StatusBadge({ status, children, tone }: StatusBadgeProps) {
  const statusClass = tone ?? status;
  return (
    <span className={`status-badge status-${statusClass}`}>
      {children ?? status}
    </span>
  );
}
