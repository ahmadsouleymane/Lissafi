import Link from "next/link";
import { notFound } from "next/navigation";
import { markAllCommissionsPaidQuick, markCommissionPaidQuick, togglePartnerStatusQuick } from "@/actions/partners";
import { AddPartnerSaleForm } from "@/components/AddPartnerSaleForm";
import { ConfirmForm } from "@/components/ConfirmForm";
import { Badge, Button, Card, CardHeader, EmptyState, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { getPartner, getPartnerSales } from "@/lib/data";
import { PARTNER_TYPE_LABELS, PLAN_LABELS } from "@/lib/partners";
import { formatDate, formatFCFA } from "@/lib/format";
import type { PartnerSale } from "@/types";

export default async function PartnerDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [partner, sales] = await Promise.all([getPartner(id), getPartnerSales(id)]);
  if (!partner) notFound();

  const active = partner.status === "active";

  return (
    <div className="space-y-5">
      <PageHeader
        title={partner.name}
        subtitle={`${PARTNER_TYPE_LABELS[partner.type]} · code ${partner.code}`}
        action={
          <Link href="/partenaires">
            <Button size="sm" variant="secondary">← Tous les partenaires</Button>
          </Link>
        }
      />

      <Card className="p-4">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div>
            <p className="text-xs text-slate-500">Téléphone</p>
            <p className="font-medium text-slate-900">{partner.phone || "—"}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500">Statut</p>
            <Badge color={active ? "green" : "gray"}>{active ? "Actif" : "Inactif"}</Badge>
          </div>
          <div>
            <p className="text-xs text-slate-500">Commission due</p>
            <p className="font-bold text-orange-600">{formatFCFA(partner.commission_due)}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500">Commission payée</p>
            <p className="font-bold text-slate-700">{formatFCFA(partner.commission_paid)}</p>
          </div>
        </div>
        <div className="mt-4 flex flex-wrap gap-1.5">
          {active && partner.commission_due > 0 && (
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
      </Card>

      <AddPartnerSaleForm partnerId={partner.id} />

      <Card>
        <CardHeader title="Ventes attribuées" subtitle="Historique des commissions" />
        <div className="px-5 pb-5">
          {sales.length === 0 ? (
            <EmptyState title="Aucune vente attribuée" />
          ) : (
            <Table>
              <THead>
                <Th>Client</Th>
                <Th>Plan</Th>
                <Th className="text-right">Payé (FCFA)</Th>
                <Th className="text-right">Commission</Th>
                <Th>Statut</Th>
                <Th>Date</Th>
                <Th></Th>
              </THead>
              <tbody>
                {sales.map((s) => (
                  <SaleRow key={s.id} sale={s} />
                ))}
              </tbody>
            </Table>
          )}
        </div>
      </Card>
    </div>
  );
}

function SaleRow({ sale }: { sale: PartnerSale }) {
  const paid = sale.status === "paid";
  return (
    <Tr>
      <Td>
        <p className="font-medium text-slate-900">{sale.client_name || "—"}</p>
        {sale.client_phone && <p className="text-xs text-slate-400">{sale.client_phone}</p>}
      </Td>
      <Td>
        <Badge color="blue">{PLAN_LABELS[sale.plan]}</Badge>
      </Td>
      <Td className="text-right tabular-nums text-slate-600">{formatFCFA(sale.amount_paid_fcfa)}</Td>
      <Td className="text-right tabular-nums font-semibold text-slate-700">{formatFCFA(sale.commission_fcfa)}</Td>
      <Td>
        <Badge color={paid ? "green" : "orange"}>{paid ? "Payée" : "Due"}</Badge>
      </Td>
      <Td className="whitespace-nowrap text-xs text-slate-500">{formatDate(sale.created_at)}</Td>
      <Td>
        {!paid && (
          <form action={markCommissionPaidQuick.bind(null, sale.id)}>
            <Button type="submit" size="sm" variant="secondary">Marquer payée</Button>
          </form>
        )}
      </Td>
    </Tr>
  );
}
