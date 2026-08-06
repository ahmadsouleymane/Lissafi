import { supabaseAdmin } from "./supabase";
import { toNumber } from "./format";
import type {
  AdminAction,
  AppLog,
  AppSettingRow,
  AuditLog,
  SalesPoint,
  SaleRow,
  SignupPoint,
  Stats,
  Ticket,
  TicketReply,
  UserSummary,
} from "@/types";

// ============================================================
// Lectures — toutes passent par le client service_role (serveur)
// ============================================================

/** Statistiques globales du dashboard. */
export async function getStats(): Promise<Stats> {
  const { data } = await supabaseAdmin().rpc("admin_stats");
  return (data ?? {}) as Stats;
}

/** Liste de tous les comptes avec indicateurs agrégés. */
export async function getUserSummaries(): Promise<UserSummary[]> {
  const { data } = await supabaseAdmin().rpc("admin_user_summaries");
  return Array.isArray(data) ? (data as UserSummary[]) : [];
}

/** Map user_id → email (léger, pour joindre les emails dans les listes). */
export async function getUserEmails(): Promise<Record<string, string>> {
  const { data } = await supabaseAdmin().rpc("admin_user_emails");
  const map: Record<string, string> = {};
  for (const row of data ?? []) map[row.user_id] = row.email ?? "";
  return map;
}

/** Ventes par jour (N derniers jours). */
export async function getSalesSeries(days = 30): Promise<SalesPoint[]> {
  const { data } = await supabaseAdmin().rpc("admin_sales_series", { days });
  return Array.isArray(data) ? (data as SalesPoint[]) : [];
}

/** Inscriptions par jour (N derniers jours). */
export async function getSignupsSeries(days = 30): Promise<SignupPoint[]> {
  const { data } = await supabaseAdmin().rpc("admin_signups_series", { days });
  return Array.isArray(data) ? (data as SignupPoint[]) : [];
}

/** Journal applicatif (app_logs) avec filtres. */
export async function getLogs(opts: {
  fromTs?: number;
  toTs?: number;
  level?: string;
  eventType?: string;
  userId?: string;
  limit?: number;
} = {}): Promise<AppLog[]> {
  const { data } = await supabaseAdmin().rpc("admin_logs", {
    from_ts: opts.fromTs ?? 0,
    to_ts: opts.toTs ?? 0,
    lvl: opts.level ?? "",
    etype: opts.eventType ?? "",
    uid: opts.userId ?? null,
    lim: opts.limit ?? 200,
  });
  return Array.isArray(data) ? (data as AppLog[]) : [];
}

/** Journal des connexions/auth (auth.audit_log_entries). */
export async function getAuditLogs(opts: { fromTs?: number; toTs?: number; limit?: number } = {}): Promise<AuditLog[]> {
  const { data } = await supabaseAdmin().rpc("admin_audit_logs", {
    from_ts: opts.fromTs ?? 0,
    to_ts: opts.toTs ?? 0,
    lim: opts.limit ?? 500,
  });
  return Array.isArray(data) ? (data as AuditLog[]) : [];
}

/** Tickets de support (avec email joint). */
export async function getTickets(status?: string): Promise<Ticket[]> {
  let query = supabaseAdmin().from("support_tickets").select("*").order("created_at", { ascending: false });
  if (status && status !== "all") query = query.eq("status", status);
  const { data } = await query.limit(300);
  const tickets = (data ?? []) as Ticket[];
  const emails = await getUserEmails();
  return tickets.map((t) => ({ ...t, email: emails[t.user_id] ?? null }));
}

export async function getTicket(id: number): Promise<(Ticket & { email?: string | null }) | null> {
  const { data } = await supabaseAdmin().from("support_tickets").select("*").eq("id", id).maybeSingle();
  if (!data) return null;
  const emails = await getUserEmails();
  return { ...(data as Ticket), email: emails[(data as Ticket).user_id] ?? null };
}

export async function getTicketReplies(ticketId: number): Promise<TicketReply[]> {
  const { data } = await supabaseAdmin()
    .from("ticket_replies")
    .select("*")
    .eq("ticket_id", ticketId)
    .order("created_at", { ascending: true });
  return (data ?? []) as TicketReply[];
}

/** Détail d'un compte : récap + ventes récentes + logs + tickets + paramètres. */
export async function getUserDetail(userId: string): Promise<{
  summary: UserSummary | null;
  sales: SaleRow[];
  settings: Record<string, string>;
  logs: AppLog[];
  tickets: Ticket[];
  actions: AdminAction[];
  clientCount: number;
  clientDebtTotal: number;
}> {
  const [summaries, salesRes, settingsRes, logsRes, ticketsRes, actionsRes, clientsRes] = await Promise.all([
    getUserSummaries(),
    supabaseAdmin().from("sales").select("*").eq("user_id", userId).order("date", { ascending: false }).limit(100),
    supabaseAdmin().from("app_settings").select("*").eq("user_id", userId),
    getLogs({ userId, limit: 60 }),
    supabaseAdmin().from("support_tickets").select("*").eq("user_id", userId).order("created_at", { ascending: false }),
    supabaseAdmin().from("admin_actions").select("*").eq("target_user_id", userId).order("created_at", { ascending: false }).limit(20),
    supabaseAdmin().from("clients").select("total_debt").eq("user_id", userId),
  ]);

  const summary = summaries.find((s) => s.user_id === userId) ?? null;

  const settings: Record<string, string> = {};
  for (const row of (settingsRes.data ?? []) as AppSettingRow[]) settings[row.key] = row.value;

  const clients = (clientsRes.data ?? []) as { total_debt: number }[];

  return {
    summary,
    sales: (salesRes.data ?? []) as SaleRow[],
    settings,
    logs: logsRes,
    tickets: (ticketsRes.data ?? []) as Ticket[],
    actions: (actionsRes.data ?? []) as AdminAction[],
    clientCount: clients.length,
    clientDebtTotal: clients.reduce((acc, c) => acc + toNumber(c.total_debt), 0),
  };
}

/** Paramètres globaux du back-office. */
export async function getAdminSettings(): Promise<Record<string, string>> {
  const { data } = await supabaseAdmin().from("admin_settings").select("*");
  const map: Record<string, string> = {};
  for (const row of data ?? []) map[row.key] = row.value;
  return map;
}

/** Dernières actions admin (journal de traçabilité). */
export async function getRecentAdminActions(limit = 15): Promise<AdminAction[]> {
  const { data } = await supabaseAdmin()
    .from("admin_actions")
    .select("*")
    .order("created_at", { ascending: false })
    .limit(limit);
  return (data ?? []) as AdminAction[];
}

/** Comptage simple d'une table (utilitaire). */
export async function countTable(table: string): Promise<number> {
  const { count } = await supabaseAdmin().from(table).select("*", { count: "exact", head: true });
  return toNumber(count);
}
