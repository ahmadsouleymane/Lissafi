import { redirect } from "next/navigation";
import { PortalShell } from "@/components/PortalShell";
import { PartnerLinkCard } from "@/components/PartnerLinkCard";
import { RequestPayoutForm } from "@/components/RequestPayoutForm";
import { partnerSignOut } from "@/actions/partners";
import { Badge, Button, Card, CardHeader, EmptyState, StatCard, Table, Td, Th, THead, Tr } from "@/components/ui";
import { getPartnerSession } from "@/lib/session";
import { getPartnerDashboard } from "@/lib/data";
import { MEMBER_PLAN_LABELS, PLAN_LABELS, partnerLink, qrImageUrl } from "@/lib/partners";
import { formatDate, formatFCFA } from "@/lib/format";
import type { MemberPlan } from "@/types";

export const dynamic = "force-dynamic";

export const metadata = { title: "Mon espace partenaire" };

const PLAN_BADGE: Record<MemberPlan, "gray" | "blue" | "amber"> = {
  free: "gray",
  plus: "blue",
  business: "amber",
};

export default async function PartnerSpacePage() {
  const session = await getPartnerSession();
  if (!session) redirect("/partenaire/connexion");

  const dash = await getPartnerDashboard(session.partnerId);
  if (!dash) redirect("/partenaire/connexion");

  const link = partnerLink(dash.code);
  const qrUrl = qrImageUrl(link);
  const conversion = dash.conversion_rate == null ? null : Math.round(dash.conversion_rate * 100);

  return (
    <PortalShell maxWidth="max-w-3xl">
      {/* Bandeau d'accueil, style landing */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-brand-700 via-brand-800 to-brand-900 px-6 py-8 text-white shadow-lg">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-brand-200">Espace partenaire</p>
            <h1 className="mt-1 text-2xl font-bold">Bonjour {dash.name || "partenaire"} 👋</h1>
            <p className="mt-1 text-sm text-brand-100">
              Ton code :{" "}
              <code className="rounded bg-white/10 px-1.5 py-0.5 font-mono text-sm">{dash.code}</code>
              {dash.status === "inactive" && <span className="ml-2 text-amber-200">· compte suspendu</span>}
            </p>
          </div>
          <form action={partnerSignOut}>
            <Button type="submit" size="sm" variant="secondary">Se déconnecter</Button>
          </form>
        </div>
      </div>

      {/* Les 3 indicateurs clés */}
      <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <StatCard label="Installations" value={dash.installs} accent="blue" />
        <StatCard label="Comptes créés" value={dash.accounts} accent="green" />
        <StatCard label="Abonnés payants" value={dash.paid} accent="orange" />
      </div>
      <p className="mt-2 text-center text-xs text-slate-400">
        Taux de conversion (compte → payé) :{" "}
        <span className="font-semibold text-slate-600">{conversion == null ? "—" : `${conversion} %`}</span>
      </p>

      <div className="mt-5 space-y-5">
        <PartnerLinkCard link={link} qrUrl={qrUrl} />

        {/* Membres parrainés + leur plan */}
        <Card>
          <CardHeader
            title="Tes membres"
            subtitle={`${dash.accounts} personne(s) venue(s) grâce à toi, avec leur formule.`}
          />
          <div className="px-5 pb-5">
            {dash.members.length === 0 ? (
              <EmptyState
                title="Aucun membre pour l'instant"
                subtitle="Partage ton lien : les personnes qui installent l'app apparaîtront ici."
              />
            ) : (
              <Table>
                <THead>
                  <Th>Nom</Th>
                  <Th>Formule</Th>
                </THead>
                <tbody>
                  {dash.members.map((m) => (
                    <Tr key={m.user_id}>
                      <Td className="font-medium text-slate-900">{m.name}</Td>
                      <Td>
                        <Badge color={PLAN_BADGE[m.plan]}>{MEMBER_PLAN_LABELS[m.plan]}</Badge>
                      </Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            )}
          </div>
        </Card>

        {/* Commissions */}
        <div className="grid gap-3 sm:grid-cols-2">
          <Card className="p-4">
            <p className="text-xs text-slate-500">Commission due (en attente)</p>
            <p className="text-xl font-bold text-orange-600">{formatFCFA(dash.commission_due)}</p>
          </Card>
          <Card className="p-4">
            <p className="text-xs text-slate-500">Déjà payé</p>
            <p className="text-xl font-bold text-slate-700">{formatFCFA(dash.commission_paid)}</p>
          </Card>
        </div>

        <RequestPayoutForm available={dash.available} />

        {/* Ventes attribuées (commissions) */}
        <Card>
          <CardHeader
            title="Ventes attribuées"
            subtitle="Commissions générées quand un membre passe en payant."
          />
          <div className="px-5 pb-5">
            {dash.sales.length === 0 ? (
              <EmptyState
                title="Aucune vente attribuée"
                subtitle="Quand un membre prend Lissafi Plus ou Business, la commission apparaît ici."
              />
            ) : (
              <Table>
                <THead>
                  <Th>Client</Th>
                  <Th>Plan</Th>
                  <Th className="text-right">Commission</Th>
                  <Th>Statut</Th>
                </THead>
                <tbody>
                  {dash.sales.map((s) => (
                    <Tr key={s.id}>
                      <Td className="font-medium text-slate-900">{s.client_name || "Client"}</Td>
                      <Td><Badge color="blue">{PLAN_LABELS[s.plan]}</Badge></Td>
                      <Td className="text-right tabular-nums font-semibold text-slate-700">{formatFCFA(s.commission_fcfa)}</Td>
                      <Td><Badge color={s.status === "paid" ? "green" : "orange"}>{s.status === "paid" ? "Payée" : "Due"}</Badge></Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            )}
          </div>
        </Card>

        {/* Retraits */}
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
      </div>
    </PortalShell>
  );
}
