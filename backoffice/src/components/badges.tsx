import { Badge, BadgeColor } from "./ui";
import { daysUntil } from "@/lib/format";

// ============================================================
// Badges métier : premium, statuts de tickets, niveaux de log
// ============================================================

/** Statut premium d'un compte (actif / expiré / démo / gratuit). */
export function PremiumBadge({ premium, expiry }: { premium: boolean; expiry: number }) {
  if (premium) {
    const days = daysUntil(expiry);
    if (days < 0) return <Badge color="red">Expiré</Badge>;
    if (days <= 15) return <Badge color="orange">Premium · expire dans {days} j</Badge>;
    return <Badge color="green">Premium</Badge>;
  }
  return <Badge color="gray">Gratuit</Badge>;
}

const ticketStatusMeta: Record<string, { label: string; color: BadgeColor }> = {
  open: { label: "Ouvert", color: "blue" },
  in_progress: { label: "En cours", color: "amber" },
  resolved: { label: "Résolu", color: "green" },
  closed: { label: "Clos", color: "gray" },
};

export function TicketStatusBadge({ status }: { status: string }) {
  const meta = ticketStatusMeta[status] ?? { label: status, color: "gray" as BadgeColor };
  return <Badge color={meta.color}>{meta.label}</Badge>;
}

const priorityMeta: Record<string, { label: string; color: BadgeColor }> = {
  low: { label: "Basse", color: "gray" },
  normal: { label: "Normale", color: "blue" },
  high: { label: "Haute", color: "orange" },
  urgent: { label: "Urgente", color: "red" },
};

export function PriorityBadge({ priority }: { priority: string }) {
  const meta = priorityMeta[priority] ?? { label: priority, color: "gray" as BadgeColor };
  return <Badge color={meta.color}>{meta.label}</Badge>;
}

const levelMeta: Record<string, { label: string; color: BadgeColor }> = {
  debug: { label: "DEBUG", color: "gray" },
  info: { label: "INFO", color: "blue" },
  warn: { label: "WARN", color: "amber" },
  error: { label: "ERREUR", color: "red" },
};

export function LevelBadge({ level }: { level: string }) {
  const meta = levelMeta[level] ?? { label: level.toUpperCase(), color: "gray" as BadgeColor };
  return <Badge color={meta.color}>{meta.label}</Badge>;
}

export function EventTypeLabel({ type }: { type: string }) {
  const labels: Record<string, string> = {
    app_start: "Démarrage",
    sync: "Synchro",
    sync_error: "Échec synchro",
    receipt: "Reçu",
    error: "Erreur",
    session_invalid: "Session invalide",
    ticket: "Signalement",
  };
  return <span>{labels[type] ?? type}</span>;
}
