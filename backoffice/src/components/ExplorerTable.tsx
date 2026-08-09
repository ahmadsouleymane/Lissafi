import type { ReactNode } from "react";
import { Badge, Table, Td, Th, THead, Tr } from "@/components/ui";
import { formatDate, formatDateShort, formatFCFA, toNumber } from "@/lib/format";
import type { ExplorerColumn, ExplorerTableConfig } from "@/lib/explorer";

function formatValue(value: unknown, type: ExplorerColumn["type"]): ReactNode {
  if (value === null || value === undefined || value === "") return "—";
  switch (type) {
    case "money":
      return <span className="tabular-nums">{formatFCFA(toNumber(value))}</span>;
    case "date":
      return <span className="whitespace-nowrap text-xs">{formatDateShort(toNumber(value))}</span>;
    case "datetime":
      return <span className="whitespace-nowrap text-xs">{formatDate(toNumber(value))}</span>;
    case "boolean":
      return value ? <Badge color="green">Oui</Badge> : <Badge color="gray">Non</Badge>;
    case "number":
      return <span className="tabular-nums">{toNumber(value).toLocaleString("fr-FR")}</span>;
    default:
      return String(value);
  }
}

export function ExplorerTable({
  config,
  rows,
  userEmails,
}: {
  config: ExplorerTableConfig;
  rows: Record<string, unknown>[];
  userEmails: Record<string, string>;
}) {
  const cols: ExplorerColumn[] = config.hasUserId
    ? [{ key: "__account__", label: "Compte", description: "Compte auquel la ligne appartient.", type: "text" }, ...config.columns]
    : config.columns;

  return (
    <Table>
      <THead>
        {cols.map((c) => (
          <Th key={c.key}>{c.label}</Th>
        ))}
      </THead>
      <tbody>
        {rows.map((row, i) => (
          <Tr key={i}>
            {cols.map((c) => (
              <Td key={c.key} className="align-middle">
                {c.key === "__account__"
                  ? userEmails[row.user_id as string] || String(row.user_id ?? "").slice(0, 8)
                  : formatValue(row[c.key], c.type)}
              </Td>
            ))}
          </Tr>
        ))}
      </tbody>
    </Table>
  );
}
