import Link from "next/link";
import { approvePayoutQuick } from "@/actions/payouts";
import { ConfirmForm } from "@/components/ConfirmForm";
import { Badge, Button, EmptyState, PageHeader, StatCard, Table, Td, Th, THead, Tr } from "@/components/ui";
import { getPayoutRequests } from "@/lib/data";
import { formatDate, formatFCFA } from "@/lib/format";
import type { PayoutRequest } from "@/types";

export const dynamic = "force-dynamic";

export default async function PayoutsPage() {
  const requests = await getPayoutRequests();
  const pending = requests.filter((r) => r.status === "requested");
  const pendingTotal = pending.reduce((s, r) => s + r.amount_fcfa, 0);
  const paidTotal = requests.filter((r) => r.status === "paid").reduce((s, r) => s + r.amount_fcfa, 0);

  return (
    <div className="space-y-5">
      <PageHeader
        title="Demandes de retrait"
        subtitle="Paie le partenaire (Orange Money / Moov) puis valide — les commissions dues sont soldées automatiquement."
        action={
          <Link href="/partenaires">
            <Button size="sm" variant="secondary">← Partenaires</Button>
          </Link>
        }
      />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        <StatCard label="En attente" value={pending.length} accent="orange" />
        <StatCard label="Montant à payer" value={formatFCFA(pendingTotal)} accent="orange" />
        <StatCard label="Total déjà payé" value={formatFCFA(paidTotal)} accent="gray" />
      </div>

      {requests.length === 0 ? (
        <EmptyState title="Aucune demande de retrait" subtitle="Les demandes des partenaires apparaîtront ici." />
      ) : (
        <Table>
          <THead>
            <Th>Partenaire</Th>
            <Th className="text-right">Montant</Th>
            <Th>Demandé le</Th>
            <Th>Statut</Th>
            <Th>Action</Th>
          </THead>
          <tbody>
            {requests.map((r) => (
              <PayoutRow key={r.id} req={r} />
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}

function PayoutRow({ req }: { req: PayoutRequest }) {
  const requested = req.status === "requested";
  return (
    <Tr>
      <Td>
        <p className="font-medium text-slate-900">{req.partner_name}</p>
        <p className="text-xs text-slate-400">
          <code>{req.partner_code}</code>
          {req.partner_phone ? ` · ${req.partner_phone}` : ""}
        </p>
      </Td>
      <Td className="text-right tabular-nums font-semibold text-slate-800">{formatFCFA(req.amount_fcfa)}</Td>
      <Td className="whitespace-nowrap text-xs text-slate-500">{formatDate(req.requested_at)}</Td>
      <Td><Badge color={requested ? "orange" : "green"}>{requested ? "En attente" : "Payé"}</Badge></Td>
      <Td>
        {requested ? (
          <ConfirmForm
            action={approvePayoutQuick.bind(null, req.id)}
            confirmText={`Confirmer le paiement de ${formatFCFA(req.amount_fcfa)} à ${req.partner_name} ?`}
          >
            <Button type="submit" size="sm">Valider le paiement</Button>
          </ConfirmForm>
        ) : (
          <span className="text-xs text-slate-400">{req.paid_at ? formatDate(req.paid_at) : "—"}</span>
        )}
      </Td>
    </Tr>
  );
}
