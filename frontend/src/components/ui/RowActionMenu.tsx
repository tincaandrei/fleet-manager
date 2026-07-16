import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

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
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const containerRef = useRef<HTMLDivElement | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const updatePosition = useCallback(() => {
    const trigger = triggerRef.current;
    if (!trigger) return;

    const viewportPadding = 8;
    const gap = 4;
    const triggerRect = trigger.getBoundingClientRect();
    const menuWidth = menuRef.current?.offsetWidth ?? 196;
    const menuHeight = menuRef.current?.offsetHeight ?? Math.min(actions.length * 42 + 12, 320);
    const left = Math.max(
      viewportPadding,
      Math.min(triggerRect.right - menuWidth, window.innerWidth - menuWidth - viewportPadding),
    );
    const hasRoomBelow = triggerRect.bottom + gap + menuHeight <= window.innerHeight - viewportPadding;
    const top = hasRoomBelow
      ? triggerRect.bottom + gap
      : Math.max(viewportPadding, triggerRect.top - menuHeight - gap);

    setPosition({ top, left });
  }, [actions.length]);

  useEffect(() => {
    if (!open) return;

    const handlePointerDown = (event: MouseEvent) => {
      const target = event.target as Node;
      const insideTrigger = containerRef.current?.contains(target);
      const insideMenu = menuRef.current?.contains(target);
      if (!insideTrigger && !insideMenu) setOpen(false);
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

  useLayoutEffect(() => {
    if (!open) return;

    updatePosition();
    const animationFrame = window.requestAnimationFrame(updatePosition);
    window.addEventListener('resize', updatePosition);
    window.addEventListener('scroll', updatePosition, true);
    return () => {
      window.cancelAnimationFrame(animationFrame);
      window.removeEventListener('resize', updatePosition);
      window.removeEventListener('scroll', updatePosition, true);
    };
  }, [open, updatePosition]);

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
        ref={triggerRef}
        type="button"
        className="row-menu-trigger"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        disabled={disabled}
        onClick={() => {
          if (!open) updatePosition();
          setOpen((current) => !current);
        }}
      >
        <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <circle cx="5" cy="12" r="1.8" />
          <circle cx="12" cy="12" r="1.8" />
          <circle cx="19" cy="12" r="1.8" />
        </svg>
      </button>

      {open && createPortal(
        <div
          ref={menuRef}
          className="row-menu-dropdown"
          role="menu"
          style={{ top: position.top, left: position.left }}
        >
          {regular.map(renderItem)}
          {regular.length > 0 && destructive.length > 0 && <div className="row-menu-separator" />}
          {destructive.map(renderItem)}
        </div>,
        document.body,
      )}
    </div>
  );
}
