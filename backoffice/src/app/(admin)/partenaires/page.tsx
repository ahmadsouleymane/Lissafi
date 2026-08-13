import Link from "next/link";
import { markAllCommissionsPaidQuick, togglePartnerStatusQuick } from "@/actions/partners";
import { AddPartnerForm } from "@/components/AddPartnerForm";
import { ConfirmForm } from "@/components/ConfirmForm";
import { Badge, Button, EmptyState, PageHeader, StatCard, Table, Td, Th, THead, Tr } from "@/components/ui";
import { IconHandshake } from "@/components/icons";
import { getPartners } from "@/lib/data";
import { PARTNER_TYPE_LABELS } from "@/lib/partners";
import { formatFCFA } from "@/lib/format";
import type { PartnerSummary } from "@/types";

export default async function PartnersPage() {
  const partners = await getPartners();
  const activeCount = partners.filter((p) => p.status === "active").length;
  const totalClients = partners.reduce((s, p) => s + p.sale_count, 0);
  const totalDue = partners.reduce((s, p) => s + p.commission_due, 0);
  const totalPaid = partners.reduce((s, p) => s + p.commission_paid, 0);

  return (
    <div className="space-y-5">
      <PageHeader
        title="Partenaires"
        subtitle="Réseau de revendeurs, ambassadeurs et parrainage — commission en espèces par client payant."
        action={
          <span className="flex items-center gap-2 rounded-lg bg-brand-50 px-3 py-1.5 text-xs font-medium text-brand-700">
            <IconHandshake size={15} /> {activeCount} actif(s)
          </span>
        }
      />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="Partenaires actifs" value={activeCount} icon={<IconHandshake size={18} />} accent="green" />
        <StatCard label="Clients amenés" value={totalClients} accent="blue" />
        <StatCard label="Commissions dues" value={formatFCFA(totalDue)} accent="orange" />
        <StatCard label="Commissions payées" value={formatFCFA(totalPaid)} accent="gray" />
      </div>

      <AddPartnerForm />

      {partners.length === 0 ? (
        <EmptyState title="Aucun partenaire" subtitle="Ajoute ton premier partenaire ci-dessus." />
      ) : (
        <Table>
          <THead>
            <Th>Partenaire</Th>
            <Th>Type</Th>
            <Th className="text-right">Clients</Th>
            <Th className="text-right">Commission due</Th>
            <Th className="text-right">Commission payée</Th>
            <Th>Statut</Th>
            <Th>Actions</Th>
          </THead>
          <tbody>
            {partners.map((p) => (
              <PartnerRow key={p.id} partner={p} />
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}

function PartnerRow({ partner }: { partner: PartnerSummary }) {
  const active = partner.status === "active";
  return (
    <Tr>
      <Td>
        <Link href={`/partenaires/${partner.id}`} className="font-medium text-brand-600 hover:underline">
          {partner.name}
        </Link>
        <p className="text-xs text-slate-400">
          <code>{partner.code}</code>
          {partner.phone ? ` · ${partner.phone}` : ""}
        </p>
      </Td>
      <Td>
        <Badge color="blue">{PARTNER_TYPE_LABELS[partner.type]}</Badge>
      </Td>
      <Td className="text-right tabular-nums text-slate-600">{partner.sale_count}</Td>
      <Td className="text-right tabular-nums text-orange-600">{formatFCFA(partner.commission_due)}</Td>
      <Td className="text-right tabular-nums text-slate-600">{formatFCFA(partner.commission_paid)}</Td>
      <Td>
        <Badge color={active ? "green" : "gray"}>{active ? "Actif" : "Inactif"}</Badge>
      </Td>
      <Td>
        <div className="flex flex-wrap items-center gap-1.5">
          <Link href={`/partenaires/${partner.id}`}>
            <Button size="sm" variant="secondary">+ Vente</Button>
          </Link>
          {partner.commission_due > 0 && (
            <form action={markAllCommissionsPaidQuick.bind(null, partner.id)}>
              <Button type="submit" size="sm">Tout payer</Button>
            </form>
          )}
          {active ? (
            <ConfirmForm action={togglePartnerStatusQuick.bind(null, partner.id)} confirmText={`Désactiver ${partner.name} ?`}>
              <Button type="submit" size="sm" variant="dangerOutline">Désactiver</Button>
            </ConfirmForm>
          ) : (
            <form action={togglePartnerStatusQuick.bind(null, partner.id)}>
              <Button type="submit" size="sm" variant="secondary">Réactiver</Button>
            </form>
          )}
        </div>
      </Td>
    </Tr>
  );
}
