import type { ReactNode } from 'react';

interface DataStateProps {
  type?: 'loading' | 'empty' | 'error' | 'info' | 'success';
  children: ReactNode;
  className?: string;
}

export default function DataState({ type = 'empty', children, className = '' }: DataStateProps) {
  if (type === 'error') {
    return <p className={`error${className ? ` ${className}` : ''}`}>{children}</p>;
  }

  if (type === 'success') {
    return <p className={`success-note${className ? ` ${className}` : ''}`}>{children}</p>;
  }

  if (type === 'info') {
    return <p className={`info-note${className ? ` ${className}` : ''}`}>{children}</p>;
  }

  return <p className={`doc-empty data-state data-state-${type}${className ? ` ${className}` : ''}`}>{children}</p>;
}
