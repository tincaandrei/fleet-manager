import { useEffect, useRef, useState } from 'react';

export interface RowAction {
  label: string;
  onSelect: () => void;
  disabled?: boolean;
  /** Destructive actions render separated and in red. */
  danger?: boolean;
}

interface RowActionMenuProps {
  actions: RowAction[];
  /** Accessible name for the trigger, e.g. "Actions for alice@example.com". */
  label: string;
  disabled?: boolean;
}

/**
 * Compact "..." dropdown for secondary row actions. Destructive actions are
 * visually separated at the bottom of the menu.
 */
export default function RowActionMenu({ actions, label, disabled }: RowActionMenuProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!open) return;

    const handlePointerDown = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };

    document.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [open]);

  const safeActions = actions.filter(Boolean);
  const regular = safeActions.filter((action) => !action.danger);
  const destructive = safeActions.filter((action) => action.danger);

  if (safeActions.length === 0) {
    return null;
  }

  const renderItem = (action: RowAction) => (
    <button
      key={action.label}
      type="button"
      role="menuitem"
      className={`row-menu-item${action.danger ? ' row-menu-item--danger' : ''}`}
      disabled={action.disabled}
      onClick={() => {
        setOpen(false);
        action.onSelect();
      }}
    >
      {action.label}
    </button>
  );

  return (
    <div className="row-menu" ref={containerRef}>
      <button
        type="button"
        className="row-menu-trigger"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        disabled={disabled}
        onClick={() => setOpen((current) => !current)}
      >
        <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <circle cx="5" cy="12" r="1.8" />
          <circle cx="12" cy="12" r="1.8" />
          <circle cx="19" cy="12" r="1.8" />
        </svg>
      </button>

      {open && (
        <div className="row-menu-dropdown" role="menu">
          {regular.map(renderItem)}
          {regular.length > 0 && destructive.length > 0 && <div className="row-menu-separator" />}
          {destructive.map(renderItem)}
        </div>
      )}
    </div>
  );
}
