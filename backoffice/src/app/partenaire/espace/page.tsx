import { redirect } from "next/navigation";
import { PortalShell } from "@/components/PortalShell";
import { PartnerLinkCard } from "@/components/PartnerLinkCard";
import { RequestPayoutForm } from "@/components/RequestPayoutForm";
import { partnerSignOut } from "@/actions/partners";
import { Badge, Button, Card, CardHeader, EmptyState, StatCard, Table, Td, Th, THead, Tr } from "@/components/ui";
import { getPartnerSession } from "@/lib/session";
import { getPartnerDashboard } from "@/lib/data";
import { PLAN_LABELS, partnerLink, qrImageUrl } from "@/lib/partners";
import { formatDate, formatFCFA, timeAgo } from "@/lib/format";

export const dynamic = "force-dynamic";

export const metadata = { title: "Mon espace partenaire" };

export default async function PartnerSpacePage() {
  const session = await getPartnerSession();
  if (!session) redirect("/partenaire/connexion");

  const dash = await getPartnerDashboard(session.partnerId);
  if (!dash) redirect("/partenaire/connexion");

  const link = partnerLink(dash.code);
  const qrUrl = qrImageUrl(link);

  return (
    <PortalShell maxWidth="max-w-3xl">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Bonjour {dash.name || "partenaire"} 👋</h1>
          <p className="mt-0.5 text-sm text-slate-500">
            Code <code className="font-medium text-slate-700">{dash.code}</code>
            {dash.status === "inactive" && (
              <span className="ml-2"><Badge color="gray">Compte suspendu</Badge></span>
            )}
          </p>
        </div>
        <form action={partnerSignOut}>
          <Button type="submit" size="sm" variant="secondary">Se déconnecter</Button>
        </form>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="Visites du lien" value={dash.visits} accent="blue" />
        <StatCard label="Clients confirmés" value={dash.clients} accent="green" />
        <StatCard label="Gains ce mois" value={formatFCFA(dash.earnings_month)} accent="orange" />
        <StatCard label="Gains cumulés" value={formatFCFA(dash.earnings_total)} accent="gray" />
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <Card className="p-4">
          <p className="text-xs text-slate-500">Commission due (en attente de paiement)</p>
          <p className="text-xl font-bold text-orange-600">{formatFCFA(dash.commission_due)}</p>
        </Card>
        <Card className="p-4">
          <p className="text-xs text-slate-500">Déjà payé</p>
          <p className="text-xl font-bold text-slate-700">{formatFCFA(dash.commission_paid)}</p>
        </Card>
      </div>

      <div className="mt-5 space-y-5">
        <PartnerLinkCard link={link} qrUrl={qrUrl} />

        <RequestPayoutForm available={dash.available} />

        {/* Historique des retraits */}
        <Card>
          <CardHeader title="Mes retraits" subtitle="Demandes de paiement de tes commissions." />
          <div className="px-5 pb-5">
            {dash.payouts.length === 0 ? (
              <EmptyState title="Aucun retrait" subtitle="Demande un retrait dès que tu as des commissions dues." />
            ) : (
              <Table>
                <THead>
                  <Th className="text-right">Montant</Th>
                  <Th>Statut</Th>
                  <Th>Demandé</Th>
                  <Th>Payé</Th>
                </THead>
                <tbody>
                  {dash.payouts.map((p) => (
                    <Tr key={p.id}>
                      <Td className="text-right tabular-nums font-semibold text-slate-700">{formatFCFA(p.amount_fcfa)}</Td>
                      <Td><Badge color={p.status === "paid" ? "green" : "orange"}>{p.status === "paid" ? "Payé" : "En attente"}</Badge></Td>
                      <Td className="whitespace-nowrap text-xs text-slate-500">{formatDate(p.requested_at)}</Td>
                      <Td className="whitespace-nowrap text-xs text-slate-500">{p.paid_at ? formatDate(p.paid_at) : "—"}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            )}
          </div>
        </Card>

        {/* Clients apportés */}
        <Card>
          <CardHeader title="Mes clients" subtitle="Commerçants abonnés grâce à toi." />
          <div className="px-5 pb-5">
            {dash.sales.length === 0 ? (
              <EmptyState title="Aucun client confirmé" subtitle="Partage ton lien pour commencer à gagner." />
            ) : (
              <Table>
                <THead>
                  <Th>Client</Th>
                  <Th>Plan</Th>
                  <Th className="text-right">Commission</Th>
                  <Th>Statut</Th>
                  <Th>Date</Th>
                </THead>
                <tbody>
                  {dash.sales.map((s) => (
                    <Tr key={s.id}>
                      <Td className="font-medium text-slate-900">{s.client_name || "Client"}</Td>
                      <Td><Badge color="blue">{PLAN_LABELS[s.plan]}</Badge></Td>
                      <Td className="text-right tabular-nums font-semibold text-slate-700">{formatFCFA(s.commission_fcfa)}</Td>
                      <Td><Badge color={s.status === "paid" ? "green" : "orange"}>{s.status === "paid" ? "Payée" : "Due"}</Badge></Td>
                      <Td className="whitespace-nowrap text-xs text-slate-500">{formatDate(s.created_at)}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            )}
          </div>
        </Card>

        {/* Visites récentes */}
        <Card>
          <CardHeader title="Visites récentes" subtitle={`${dash.visits} clic(s) au total sur ton lien.`} />
          <div className="px-5 pb-5">
            {dash.recent_visits.length === 0 ? (
              <EmptyState title="Aucune visite" subtitle="Les clics sur ton lien apparaîtront ici." />
            ) : (
              <ul className="divide-y divide-slate-100 text-sm">
                {dash.recent_visits.map((v) => (
                  <li key={v.id} className="flex items-center justify-between py-2">
                    <span className="text-slate-600">Clic sur ton lien</span>
                    <span className="text-xs text-slate-400">{timeAgo(v.created_at)}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </Card>
      </div>
    </PortalShell>
  );
}
