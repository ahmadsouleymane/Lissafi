"use client";

import { ReactNode } from "react";

/**
 * Formulaire de confirmation — enveloppe une action serveur et demande
 * une confirmation via `window.confirm()` avant soumission.
 * Utile pour les actions destructives (désactivation premium, suppression…).
 */
export function ConfirmForm({
  action,
  confirmText,
  children,
  className,
}: {
  action: (formData: FormData) => void | Promise<void>;
  confirmText: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <form
      action={action}
      className={className}
      onSubmit={(e) => {
        if (!window.confirm(confirmText)) e.preventDefault();
      }}
    >
      {children}
    </form>
  );
}
