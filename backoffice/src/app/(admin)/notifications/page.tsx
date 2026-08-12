import { NotificationForm } from "@/components/NotificationForm";
import { setRecapNotificationsEnabledQuick } from "@/actions/notifications";
import { Badge, Button, Card, CardHeader, EmptyState, PageHeader, Table, Td, Th, THead, Tr } from "@/components/ui";
import { IconBell } from "@/components/icons";
import { getManualNotificationHistory, getRecapEnabled, getRecapHistory, getUserSummaries } from "@/lib/data";
import { formatDateTimeIso } from "@/lib/format";
import type { NotificationAccount } from "@/types";

export default async function NotificationsPage() {
  const [summaries, recapEnabled, recapHistory, manualHistory] = await Promise.all([
    getUserSummaries(),
    getRecapEnabled(),
    getRecapHistory(),
    getManualNotificationHistory(),
  ]);

  // Projection minimale : le sélecteur de destinataires n'a besoin que de
  // l'id, l'email et la boutique — pas des codes premium, ventes, dates…
  const accounts: NotificationAccount[] = summaries.map((a) => ({
    user_id: a.user_id,
    email: a.email,
    shop_name: a.shop_name,
    shop_phone: a.shop_phone,
  }));

  return (
    <div className="space-y-5">
      <PageHeader
        title="Notifications"
        subtitle="Envoi manuel et récap automatique quotidien vers l'app Android"
        action={
          <span className="flex items-center gap-2 rounded-lg bg-brand-50 px-3 py-1.5 text-xs font-medium text-brand-700">
            <IconBell size={15} /> {accounts.length} compte(s)
          </span>
        }
      />

      <Card>
        <CardHeader
          title="Récap automatique"
          subtitle="Lundi–vendredi, 8h, heure de Niamey — CA, ventes et nouvelles dettes de la veille"
          action={
            <form action={setRecapNotificationsEnabledQuick.bind(null, !recapEnabled)}>
              <Button type="submit" size="sm" variant={recapEnabled ? "dangerOutline" : "secondary"}>
                {recapEnabled ? "Désactiver" : "Activer"}
              </Button>
            </form>
          }
        />
        <div className="px-5 pb-5">
          <Badge color={recapEnabled ? "green" : "gray"}>{recapEnabled ? "Activé" : "Désactivé"}</Badge>
          <div className="mt-4">
            {recapHistory.length === 0 ? (
              <EmptyState title="Aucune exécution pour l'instant" subtitle="La première aura lieu au prochain jour ouvré, 8h (Niamey)." />
            ) : (
              <Table>
                <THead>
                  <Th>Date</Th>
                  <Th>Destinataires</Th>
                  <Th>Succès</Th>
                  <Th>Échecs</Th>
                </THead>
                <tbody>
                  {recapHistory.map((r) => (
                    <Tr key={r.id}>
                      <Td>{formatDateTimeIso(r.created_at)}</Td>
                      <Td>{r.recipients}</Td>
                      <Td>{r.success}</Td>
                      <Td>{r.failed}</Td>
                    </Tr>
                  ))}
                </tbody>
              </Table>
            )}
          </div>
        </div>
      </Card>

      <NotificationForm accounts={accounts} />

      <Card>
        <CardHeader title="Historique des envois manuels" />
        <div className="px-5 pb-5">
          {manualHistory.length === 0 ? (
            <EmptyState title="Aucun envoi manuel pour l'instant" />
          ) : (
            <Table>
              <THead>
                <Th>Date</Th>
                <Th>Titre</Th>
                <Th>Ciblage</Th>
                <Th>Destinataires</Th>
                <Th>Succès</Th>
                <Th>Échecs</Th>
              </THead>
              <tbody>
                {manualHistory.map((n) => (
                  <Tr key={n.id}>
                    <Td>{formatDateTimeIso(n.created_at)}</Td>
                    <Td className="max-w-xs truncate">{n.title}</Td>
                    <Td>{n.target_summary}</Td>
                    <Td>{n.recipients}</Td>
                    <Td>{n.success}</Td>
                    <Td>{n.failed}</Td>
                  </Tr>
                ))}
              </tbody>
            </Table>
          )}
        </div>
      </Card>
    </div>
  );
}
