import Link from "next/link";
import { redirect } from "next/navigation";
import { PortalShell } from "@/components/PortalShell";
import { Button, Card } from "@/components/ui";
import { getPartnerSession } from "@/lib/session";
import { COMMISSIONS, PLAN_LABELS } from "@/lib/partners";
import { formatFCFA } from "@/lib/format";
import type { Plan } from "@/types";

export const dynamic = "force-dynamic";

export const metadata = { title: "Devenir partenaire" };

export default async function PartnerHomePage() {
  const session = await getPartnerSession();
  if (session) redirect("/partenaire/espace");

  return (
    <PortalShell maxWidth="max-w-lg">
      <div className="text-center">
        <span className="inline-flex rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-700 ring-1 ring-brand-600/20">
          Programme partenaire Lissafi
        </span>
        <h1 className="mt-4 text-2xl font-bold text-slate-900">
          Gagne de l'argent en présentant Lissafi
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          Partage ton lien. À chaque commerçant qui s'abonne grâce à toi, tu touches une
          commission en espèces (Orange Money / Moov). Paiement à la demande, sans plafond.
        </p>
      </div>

      <Card className="mt-6 p-5">
        <h2 className="text-sm font-semibold text-slate-900">Ta commission par client payant</h2>
        <ul className="mt-3 space-y-2">
          {(Object.keys(COMMISSIONS) as Plan[]).map((p) => (
            <li key={p} className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm">
              <span className="text-slate-700">{PLAN_LABELS[p]}</span>
              <span className="font-bold text-brand-600">{formatFCFA(COMMISSIONS[p])}</span>
            </li>
          ))}
        </ul>
        <p className="mt-3 text-xs text-slate-500">
          Exemple : 5 commerçants sur Lissafi Business = {formatFCFA(COMMISSIONS.business * 5)} pour toi.
        </p>
      </Card>

      <div className="mt-6 space-y-3">
        <Link href="/partenaire/inscription" className="block">
          <Button size="lg" className="w-full">Créer mon compte partenaire</Button>
        </Link>
        <Link href="/partenaire/connexion" className="block">
          <Button size="lg" variant="secondary" className="w-full">J'ai déjà un compte — me connecter</Button>
        </Link>
      </div>
    </PortalShell>
  );
}
