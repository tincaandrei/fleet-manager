import type { ReactNode } from 'react';

interface ResponsiveTableProps {
  children: ReactNode;
  className?: string;
  ariaLabel?: string;
}

export default function ResponsiveTable({ children, className = '', ariaLabel }: ResponsiveTableProps) {
  return (
    <div className="responsive-table-wrap">
      <table className={`vehicles-table responsive-table${className ? ` ${className}` : ''}`} aria-label={ariaLabel}>
        {children}
      </table>
    </div>
  );
}
