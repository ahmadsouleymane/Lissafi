import "server-only";
import { supabaseAdmin } from "./supabase";
import { toNumber } from "./format";
import { getExplorerConfig } from "./explorer";
import type {
  AccountSalePage,
  AdminAction,
  AppLog,
  AppSettingRow,
  AuditLog,
  Client,
  DebtTransaction,
  ExplorerData,
  NotificationLog,
  Product,
  SalesPoint,
  SaleItem,
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

function logRpcError(fn: string, error: unknown) {
  console.error(`[data] ${fn} — échec RPC:`, error instanceof Error ? error.message : error);
}

/** Statistiques globales du dashboard. */
export async function getStats(): Promise<Stats> {
  const { data, error } = await supabaseAdmin().rpc("admin_stats");
  if (error) logRpcError("getStats", error);
  return (data ?? {}) as Stats;
}

/** Liste de tous les comptes avec indicateurs agrégés. */
export async function getUserSummaries(): Promise<UserSummary[]> {
  const { data, error } = await supabaseAdmin().rpc("admin_user_summaries");
  if (error) logRpcError("getUserSummaries", error);
  return Array.isArray(data) ? (data as UserSummary[]) : [];
}

export type AvailablePremiumCode = { code: string; plan: string; created_at: string };

/** Codes premium non utilisés (à vendre). Accès service_role uniquement. */
export async function getAvailablePremiumCodes(): Promise<AvailablePremiumCode[]> {
  const { data, error } = await supabaseAdmin()
    .from("premium_codes")
    .select("code, plan, created_at")
    .is("used_by", null)
    .limit(200);
  if (error) {
    console.error("[admin] getAvailablePremiumCodes:", error);
    return [];
  }
  return (data ?? []) as AvailablePremiumCode[];
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
export async function getUserDetail(
  userId: string,
  knownSummary?: UserSummary | null
): Promise<{
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
    knownSummary ? Promise.resolve([knownSummary]) : getUserSummaries(),
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

// ============================================================
// Données brutes — consultation complète (lecture seule)
// ============================================================

/** Produits d'un compte (classe par ordre de mise à jour). */
export async function getAccountProducts(userId: string): Promise<Product[]> {
  const { data, error } = await supabaseAdmin()
    .from("products")
    .select("*")
    .eq("user_id", userId)
    .order("updated_at", { ascending: false });
  if (error) logRpcError("getAccountProducts", error);
  return (data ?? []) as Product[];
}

/** Clients d'un compte (classe par nom). */
export async function getAccountClients(userId: string): Promise<Client[]> {
  const { data, error } = await supabaseAdmin()
    .from("clients")
    .select("*")
    .eq("user_id", userId)
    .order("name", { ascending: true });
  if (error) logRpcError("getAccountClients", error);
  return (data ?? []) as Client[];
}

/** Transactions de dette d'un compte, avec le nom du client résolu. */
export async function getAccountDebts(userId: string): Promise<(DebtTransaction & { client_name: string })[]> {
  const [txns, clients] = await Promise.all([
    supabaseAdmin()
      .from("debt_transactions")
      .select("*")
      .eq("user_id", userId)
      .order("date", { ascending: false }),
    getAccountClients(userId),
  ]);
  if (txns.error) logRpcError("getAccountDebts", txns.error);
  const names = new Map(clients.map((c) => [c.id, c.name]));
  return ((txns.data ?? []) as DebtTransaction[]).map((t) => ({
    ...t,
    client_name: names.get(t.client_id) ?? t.client_id,
  }));
}

/** Ventes d'un compte, paginées, avec les articles de la page. */
export async function getAccountSales(userId: string, page = 1, limit = 50): Promise<AccountSalePage> {
  const pageSafe = Math.max(1, page);
  const limitSafe = Math.min(100, Math.max(1, limit));
  const from = (pageSafe - 1) * limitSafe;
  const to = from + limitSafe - 1;
  const { data, count, error } = await supabaseAdmin()
    .from("sales")
    .select("*", { count: "exact" })
    .eq("user_id", userId)
    .order("date", { ascending: false })
    .range(from, to);
  if (error) logRpcError("getAccountSales", error);
  const sales = (data ?? []) as SaleRow[];
  const ids = sales.map((s) => s.id);
  const itemsBySale: Record<number, SaleItem[]> = {};
  if (ids.length > 0) {
    const { data: items, error: itemsError } = await supabaseAdmin()
      .from("sale_items")
      .select("*")
      .in("sale_id", ids)
      .order("id", { ascending: true });
    if (itemsError) logRpcError("getAccountSales (items)", itemsError);
    for (const it of (items ?? []) as SaleItem[]) {
      (itemsBySale[it.sale_id] ??= []).push(it);
    }
  }
  return { sales, itemsBySale, total: toNumber(count) };
}

/** Options {id, label} pour le filtre utilisateur de l'explorateur. */
export async function getAccountOptions(): Promise<{ id: string; label: string }[]> {
  const users = await getUserSummaries();
  return users
    .filter((u) => u.user_id)
    .map((u) => ({ id: u.user_id, label: `${u.shop_name || "Boutique sans nom"} — ${u.email || "—"}` }))
    .sort((a, b) => a.label.localeCompare(b.label, "fr"));
}

/**
 * Données d'une table du registre pour l'explorateur global.
 * Valide `table` contre le registre — jamais de requête sur un nom arbitraire.
 */
export async function getExplorerData(
  table: string,
  opts: { user?: string; q?: string; page?: number; limit?: number } = {}
): Promise<ExplorerData> {
  const config = getExplorerConfig(table);
  if (!config) return { rows: [], total: 0, userEmails: {} };

  const page = Math.max(1, opts.page ?? 1);
  const limit = Math.min(100, Math.max(1, opts.limit ?? 50));
  const from = (page - 1) * limit;
  const to = from + limit - 1;

  let query = supabaseAdmin()
    .from(config.table)
    .select("*", { count: "exact" })
    .order(config.orderBy.column, { ascending: config.orderBy.ascending ?? false })
    .range(from, to);

  if (opts.user) query = query.eq("user_id", opts.user);
  if (opts.q) {
    // Neutralise les caractères de logique PostgREST (`,` `(` `)` `*`) pour
    // éviter une requête `.or()` invalide → erreur 400 silencieuse.
    const clean = opts.q.replace(/[,()*]/g, " ").replace(/\s+/g, " ").trim();
    if (clean) {
      const like = `ilike.*${clean}*`;
      query = query.or(config.searchColumns.map((c) => `${c}.${like}`).join(","));
    }
  }

  const { data, count, error } = await query;
  if (error) logRpcError("getExplorerData", error);
  const userEmails = await getUserEmails();
  return {
    rows: (data ?? []) as Record<string, unknown>[],
    total: toNumber(count),
    userEmails,
  };
}

// ============================================================
// Notifications push
// ============================================================

/** Historique des envois manuels de notifications (les plus récents d'abord). */
export async function getManualNotificationHistory(limit = 20): Promise<NotificationLog[]> {
  const { data, error } = await supabaseAdmin()
    .from("notification_log")
    .select("*")
    .eq("kind", "manuel")
    .order("created_at", { ascending: false })
    .limit(limit);
  if (error) logRpcError("getManualNotificationHistory", error);
  return (data ?? []) as NotificationLog[];
}

/** Historique des exécutions du récap automatique quotidien. */
export async function getRecapHistory(limit = 5): Promise<NotificationLog[]> {
  const { data, error } = await supabaseAdmin()
    .from("notification_log")
    .select("*")
    .eq("kind", "recap_quotidien")
    .order("created_at", { ascending: false })
    .limit(limit);
  if (error) logRpcError("getRecapHistory", error);
  return (data ?? []) as NotificationLog[];
}

/** État du réglage "récap quotidien activé" (activé par défaut si non défini). */
export async function getRecapEnabled(): Promise<boolean> {
  const settings = await getAdminSettings();
  return settings["recap_notifications_enabled"] !== "false";
}
