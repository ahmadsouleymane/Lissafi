// ============================================================
// Registre des tables consultables dans le back-office.
// Source unique de vérité : utilisée par l'explorateur global,
// les onglets des comptes et la page « Schéma des données ».
// ============================================================

export type ColumnType = "money" | "date" | "datetime" | "boolean" | "number" | "text";

export type ExplorerColumn = {
  key: string;
  label: string;
  description: string;
  type: ColumnType;
  hideOnMobile?: boolean;
};

export type ExplorerTableConfig = {
  table: string;          // nom PostgREST
  label: string;          // libellé français
  description: string;    // description (page Schéma)
  orderBy: { column: string; ascending?: boolean };
  searchColumns: string[]; // colonnes cherchables par `q`
  hasUserId: boolean;     // ajoute une colonne « Compte » (email résolu)
  columns: ExplorerColumn[];
};

export const EXPLORER_TABLES: ExplorerTableConfig[] = [
  {
    table: "products",
    label: "Produits",
    description: "Catalogue de produits de chaque boutique : code-barres, prix, stock.",
    orderBy: { column: "updated_at", ascending: false },
    searchColumns: ["name", "barcode", "category"],
    hasUserId: true,
    columns: [
      { key: "name", label: "Nom", description: "Nom du produit affiché dans la caisse.", type: "text" },
      { key: "barcode", label: "Code-barres", description: "Code-barres du produit (clé locale avec l'utilisateur).", type: "text", hideOnMobile: true },
      { key: "sell_price", label: "Prix de vente", description: "Prix de vente en FCFA.", type: "money" },
      { key: "buy_price", label: "Prix d'achat", description: "Prix d'achat en FCFA.", type: "money", hideOnMobile: true },
      { key: "stock", label: "Stock", description: "Quantité en stock.", type: "number" },
      { key: "min_stock", label: "Stock min.", description: "Seuil d'alerte de réapprovisionnement.", type: "number", hideOnMobile: true },
      { key: "category", label: "Catégorie", description: "Catégorie du produit.", type: "text", hideOnMobile: true },
      { key: "deleted", label: "Supprimé", description: "Produit supprimé (soft delete).", type: "boolean", hideOnMobile: true },
    ],
  },
  {
    table: "clients",
    label: "Clients",
    description: "Clients enregistrés de chaque boutique : nom, téléphone, dette.",
    orderBy: { column: "name", ascending: true },
    searchColumns: ["name", "phone"],
    hasUserId: true,
    columns: [
      { key: "name", label: "Nom", description: "Nom du client.", type: "text" },
      { key: "phone", label: "Téléphone", description: "Numéro de téléphone du client.", type: "text", hideOnMobile: true },
      { key: "total_debt", label: "Dette totale", description: "Dette cumulée du client en FCFA.", type: "money" },
      { key: "created_at", label: "Créé le", description: "Date de création du client (epoch ms).", type: "date", hideOnMobile: true },
    ],
  },
  {
    table: "sales",
    label: "Ventes",
    description: "Toutes les ventes : total, paiement, type (comptant/crédit), client.",
    orderBy: { column: "date", ascending: false },
    searchColumns: ["client_id"],
    hasUserId: true,
    columns: [
      { key: "id", label: "N°", description: "Identifiant de la vente (BIGSERIAL).", type: "number" },
      { key: "date", label: "Date", description: "Date de la vente (epoch ms).", type: "datetime" },
      { key: "total", label: "Total", description: "Montant total de la vente en FCFA.", type: "money" },
      { key: "amount_paid", label: "Payé", description: "Montant payé en FCFA.", type: "money", hideOnMobile: true },
      { key: "change_given", label: "Monnaie", description: "Monnaie rendue en FCFA.", type: "money", hideOnMobile: true },
      { key: "is_credit", label: "Crédit", description: "Vente faite à crédit.", type: "boolean" },
      { key: "synced", label: "Synchronisée", description: "Vente poussée vers le cloud.", type: "boolean", hideOnMobile: true },
      { key: "client_id", label: "Client", description: "Identifiant local du client (ou vide).", type: "text", hideOnMobile: true },
    ],
  },
  {
    table: "sale_items",
    label: "Articles vendus",
    description: "Lignes de chaque ticket de vente : article, prix unitaire, quantité.",
    orderBy: { column: "id", ascending: false },
    searchColumns: ["name", "barcode"],
    hasUserId: true,
    columns: [
      { key: "sale_id", label: "Vente N°", description: "Identifiant de la vente parente.", type: "number" },
      { key: "name", label: "Article", description: "Nom de l'article vendu.", type: "text" },
      { key: "barcode", label: "Code-barres", description: "Code-barres de l'article.", type: "text", hideOnMobile: true },
      { key: "price", label: "Prix unitaire", description: "Prix unitaire en FCFA.", type: "money" },
      { key: "quantity", label: "Qté", description: "Quantité vendue.", type: "number" },
    ],
  },
  {
    table: "debt_transactions",
    label: "Dettes",
    description: "Transactions de dette : montant, date, note, client concerné.",
    orderBy: { column: "date", ascending: false },
    searchColumns: ["client_id", "note"],
    hasUserId: true,
    columns: [
      { key: "client_id", label: "Client", description: "Identifiant local du client.", type: "text" },
      { key: "amount", label: "Montant", description: "Montant de la transaction en FCFA.", type: "money" },
      { key: "date", label: "Date", description: "Date de la transaction (epoch ms).", type: "datetime" },
      { key: "note", label: "Note", description: "Note libre sur la transaction.", type: "text" },
      { key: "sale_id", label: "Vente N°", description: "Vente associée (si crédit).", type: "number", hideOnMobile: true },
    ],
  },
  {
    table: "app_settings",
    label: "Paramètres",
    description: "Paramètres applicatifs stockés par compte (clé → valeur).",
    orderBy: { column: "key", ascending: true },
    searchColumns: ["key"],
    hasUserId: true,
    columns: [
      { key: "key", label: "Clé", description: "Nom du paramètre (shop_name, is_premium…).", type: "text" },
      { key: "value", label: "Valeur", description: "Valeur du paramètre.", type: "text" },
    ],
  },
];

/** Renvoie la config d'une table, ou undefined si elle n'est pas dans le registre. */
export function getExplorerConfig(table: string): ExplorerTableConfig | undefined {
  return EXPLORER_TABLES.find((t) => t.table === table);
}
