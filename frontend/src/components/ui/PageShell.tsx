import type { ReactNode } from 'react';
import Navbar from '../Navbar';

interface PageShellProps {
  children: ReactNode;
  className?: string;
}

export default function PageShell({ children, className = '' }: PageShellProps) {
  return (
    <>
      <Navbar />
      <main className={`page page-shell${className ? ` ${className}` : ''}`}>
        {children}
      </main>
    </>
  );
}
