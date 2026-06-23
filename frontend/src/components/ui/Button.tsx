import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Link } from 'react-router-dom';

type ButtonVariant = 'primary' | 'secondary' | 'success' | 'danger' | 'ghost';
type ButtonSize = 'sm' | 'md';

interface BaseProps {
  children: ReactNode;
  variant?: ButtonVariant;
  size?: ButtonSize;
  className?: string;
}

type ButtonProps = BaseProps & ButtonHTMLAttributes<HTMLButtonElement>;

interface ButtonLinkProps extends BaseProps {
  to: string;
  replace?: boolean;
}

function classes(variant: ButtonVariant, size: ButtonSize, className = '') {
  const variantClass = variant === 'primary' ? '' : ` btn-${variant}`;
  const sizeClass = size === 'sm' ? ' btn-sm' : '';
  return `btn${variantClass}${sizeClass}${className ? ` ${className}` : ''}`;
}

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  className,
  type = 'button',
  ...props
}: ButtonProps) {
  return (
    <button type={type} className={classes(variant, size, className)} {...props}>
      {children}
    </button>
  );
}

export function ButtonLink({
  children,
  variant = 'primary',
  size = 'md',
  className,
  to,
  replace,
}: ButtonLinkProps) {
  return (
    <Link to={to} replace={replace} className={classes(variant, size, className)}>
      {children}
    </Link>
  );
}
