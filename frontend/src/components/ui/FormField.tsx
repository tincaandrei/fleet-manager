import type { ReactNode } from 'react';

interface FormFieldProps {
  label: ReactNode;
  children: ReactNode;
  className?: string;
}

export default function FormField({ label, children, className = '' }: FormFieldProps) {
  return (
    <label className={`form-field${className ? ` ${className}` : ''}`}>
      <span>{label}</span>
      {children}
    </label>
  );
}
