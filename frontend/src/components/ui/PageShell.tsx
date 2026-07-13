import type { ReactNode } from 'react';
import AppLayout from '../layout/AppLayout';

interface PageShellProps {
  children: ReactNode;
  className?: string;
}

export default function PageShell({ children, className = '' }: PageShellProps) {
  return (
    <AppLayout>
      <main className={`page page-shell${className ? ` ${className}` : ''}`}>
        {children}
      </main>
    </AppLayout>
  );
}
