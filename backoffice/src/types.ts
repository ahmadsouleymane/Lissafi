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
