// ============================================================
// Types partagés du back-office Lissafi
// ============================================================

export type UserSummary = {
  user_id: string;
  email: string | null;
  created_at: string | null;
  last_sign_in_at: string | null;
  shop_name: string;
  shop_phone: string;
  premium: boolean;
  premium_expiry: number;
  demo_taken: boolean;
  activation_code: string;
  product_count: number;
  sale_count: number;
  sales_total: number;
  client_count: number;
  receipt_count: number;
};

// Sous-ensemble minimal passé aux composants client (ex: sélecteur de
// destinataires) — évite d'envoyer activation_code, premium, ventes… au
// navigateur.
export type NotificationAccount = {
  user_id: string;
  email: string | null;
  shop_name: string;
  shop_phone: string;
};

export type Stats = {
  users: number;
  users_today: number;
  active_today: number;
  premium: number;
  demo: number;
  products: number;
  sales: number;
  sales_today: number;
  sales_total_fcfa: number;
  sales_today_fcfa: number;
  credit_total_fcfa: number;
  debt_total_fcfa: number;
  clients: number;
  debt_transactions: number;
  receipts: number;
  logs: number;
  errors_24h: number;
  tickets_open: number;
  premium_expiring_30d: number;
};

export type SalesPoint = {
  day: string;
  sales: number;
  total: number;
};

export type SignupPoint = {
  day: string;
  signups: number;
};

export type AppLog = {
  id: number;
  user_id: string;
  email: string | null;
  event_type: string;
  level: string;
  message: string;
  meta: string;
  created_at: number;
};

export type AuditLog = {
  created_at: string;
  auth_event: string;
  ip: string | null;
  user_id: string | null;
  payload: Record<string, unknown>;
};

export type Ticket = {
  id: number;
  user_id: string;
  email?: string | null;
  subject: string;
  message: string;
  status: string;
  priority: string;
  created_at: number;
  updated_at: number;
};

export type TicketReply = {
  id: number;
  ticket_id: number;
  user_id: string;
  message: string;
  is_admin: boolean;
  created_at: number;
};

export type AppSettingRow = {
  key: string;
  value: string;
  user_id: string;
};

export type SaleRow = {
  id: number;
  date: number;
  total: number;
  amount_paid: number;
  change_given: number;
  is_credit: boolean;
  client_id: string | null;
  synced: boolean;
  user_id: string;
};

export type AdminAction = {
  id: number;
  admin_user_id: string;
  action: string;
  target_user_id: string | null;
  details: string;
  created_at: string;
};

export type Product = {
  barcode: string;
  name: string;
  sell_price: number;
  buy_price: number;
  stock: number;
  min_stock: number;
  category: string;
  has_barcode: boolean;
  created_at: number;
  updated_at: number;
  user_id: string;
  deleted: boolean;
};

export type Client = {
  id: string;
  name: string;
  phone: string;
  total_debt: number;
  created_at: number;
  updated_at: number;
  user_id: string;
};

export type DebtTransaction = {
  id: number;
  client_id: string;
  sale_id: number | null;
  amount: number;
  date: number;
  note: string;
  user_id: string;
};

export type SaleItem = {
  id: number;
  sale_id: number;
  barcode: string;
  name: string;
  price: number;
  quantity: number;
  user_id: string;
};

export type ExplorerData = {
  rows: Record<string, unknown>[];
  total: number;
  userEmails: Record<string, string>;
};

export type AccountSalePage = {
  sales: SaleRow[];
  itemsBySale: Record<number, SaleItem[]>;
  total: number;
};

export type NotificationLog = {
  id: number;
  kind: "manuel" | "recap_quotidien";
  title: string;
  body: string;
  admin_user_id: string | null;
  target_summary: string;
  recap_date: string | null;
  recipients: number;
  success: number;
  failed: number;
  created_at: string;
};

// ============================================================
// Programme de partenariat
// ============================================================

export type PartnerType = "agent" | "ambassador" | "strategic" | "referral";
export type Plan = "plus" | "business" | "pack";

export type PartnerSummary = {
  id: string;
  name: string;
  type: PartnerType;
  phone: string;
  email: string;
  code: string;
  status: "active" | "inactive";
  created_at: string;
  sale_count: number;
  commission_due: number;
  commission_paid: number;
  commission_remaining: number;
};

export type PartnerSale = {
  id: number;
  partner_id: string;
  client_name: string;
  client_phone: string;
  plan: Plan;
  amount_paid_fcfa: number;
  commission_fcfa: number;
  status: "owed" | "paid";
  paid_at: string | null;
  note: string;
  created_at: number;
};

export type PartnerVisit = {
  id: number;
  partner_id: string;
  created_at: number;
};

export type PartnerPayout = {
  id: number;
  partner_id: string;
  amount_fcfa: number;
  status: "requested" | "paid";
  requested_at: number;
  paid_at: number | null;
};

// File des demandes de retrait côté admin (avec identité du partenaire jointe).
export type PayoutRequest = PartnerPayout & {
  partner_name: string;
  partner_phone: string;
  partner_code: string;
};

// Tableau de bord de l'espace partenaire (portail auto-serveur).
export type PartnerDashboard = {
  id: string;
  name: string;
  code: string;
  phone: string;
  email: string;
  status: "active" | "inactive";
  visits: number;
  clients: number; // ventes confirmées attribuées
  earnings_month: number; // commissions du mois courant
  earnings_total: number; // commissions cumulées (toutes)
  commission_due: number;
  commission_paid: number;
  available: number; // due − retraits demandés non payés
  sales: PartnerSale[];
  payouts: PartnerPayout[];
  recent_visits: PartnerVisit[];
};
