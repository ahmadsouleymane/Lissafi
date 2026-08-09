import { Card, CardHeader, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { EXPLORER_TABLES } from "@/lib/explorer";

export const dynamic = "force-dynamic";

export default function SchemaPage() {
  return (
    <div className="space-y-5">
      <PageHeader title="Schéma des données" subtitle="Toutes les tables de l'application et la description de chaque champ." />
      {EXPLORER_TABLES.map((t) => (
        <Card key={t.table}>
          <CardHeader title={t.label} subtitle={<span><code className="rounded bg-slate-100 px-1.5 py-0.5 text-xs">{t.table}</code> — {t.description}</span>} />
          <Table className="mb-4">
            <THead>
              <Th>Champ</Th>
              <Th>Type</Th>
              <Th>Description</Th>
            </THead>
            <tbody>
              {t.columns.map((c) => (
                <Tr key={c.key}>
                  <Td className="font-mono text-xs text-slate-700">{c.key}</Td>
                  <Td>
                    <code className="rounded bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-600">{c.type}</code>
                  </Td>
                  <Td className="text-slate-600">{c.description}</Td>
                </Tr>
              ))}
            </tbody>
          </Table>
        </Card>
      ))}
    </div>
  );
}
