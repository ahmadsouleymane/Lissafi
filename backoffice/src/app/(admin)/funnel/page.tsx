import { Card, CardHeader, EmptyState, PageHeader } from "@/components/ui";
import { getFunnel } from "@/lib/data";
import { isConfigured } from "@/lib/supabase";
import { toNumber } from "@/lib/format";

const DAYS = 30;

function pct(part: number, whole: number): number {
  return whole > 0 ? Math.round((part / whole) * 100) : 0;
}

export default async function FunnelPage() {
  if (!isConfigured()) {
    return (
      <div>
        <PageHeader title="Entonnoir de conversion" subtitle="Suivi du parcours utilisateur" />
        <Card className="mt-6 p-6">
          <p className="text-sm text-slate-600">
            Back-office non relié à Supabase. Renseigne les clés puis exécute <code className="rounded bg-slate-100 px-1">supabase-admin.sql</code>.
          </p>
        </Card>
      </div>
    );
  }

  const funnel = await getFunnel(DAYS);

  // Étapes de l'entonnoir, de la plus large à la plus étroite.
  const steps = [
    { key: "signups", label: "Inscriptions", hint: "comptes créés", value: toNumber(funnel.signups) },
    { key: "trials", label: "Essais démarrés", hint: "essai 14 jours lancé", value: toNumber(funnel.trials) },
    { key: "first_product", label: "1er produit ajouté", hint: "a commencé son catalogue", value: toNumber(funnel.first_product) },
    { key: "first_sale", label: "1re vente", hint: "a encaissé", value: toNumber(funnel.first_sale) },
    { key: "subscribe_click", label: "Intention d'achat", hint: "a cliqué sur payer", value: toNumber(funnel.subscribe_click) },
    { key: "subscribed", label: "Abonnés", hint: "premium actif (total)", value: toNumber(funnel.subscribed) },
  ];

  const top = Math.max(1, steps[0].value);
  const hasData = steps.some((s) => s.value > 0);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Entonnoir de conversion"
        subtitle={`${DAYS} derniers jours — inscription → essai → activation → abonnement`}
      />

      <Card className="border-amber-200 bg-amber-50 p-4">
        <p className="text-sm text-amber-800">
          <strong>Haut de l'entonnoir à venir.</strong> Les étapes « visiteurs landing » et « téléchargements »
          nécessitent un beacon sur la landing (Bloc D). Cet écran suit le parcours à partir de l'inscription.
        </p>
      </Card>

      <Card>
        <CardHeader title="Parcours utilisateur" subtitle="Volume par étape et taux de conversion" />
        <div className="px-5 pb-6 pt-1">
          {!hasData ? (
            <EmptyState title="Aucune donnée sur la période" subtitle="Les étapes se rempliront à mesure que l'app remonte les événements" />
          ) : (
            <div className="space-y-3">
              {steps.map((step, i) => {
                const width = Math.max(4, Math.round((step.value / top) * 100));
                const fromSignup = pct(step.value, steps[0].value);
                const fromPrev = i === 0 ? null : pct(step.value, steps[i - 1].value);
                return (
                  <div key={step.key}>
                    <div className="mb-1 flex items-baseline justify-between gap-3">
                      <div className="flex items-baseline gap-2">
                        <span className="text-sm font-semibold text-slate-900">{step.label}</span>
                        <span className="text-xs text-slate-400">{step.hint}</span>
                      </div>
                      <div className="flex items-baseline gap-2 text-right">
                        <span className="text-sm font-bold text-slate-900">{new Intl.NumberFormat("fr-FR").format(step.value)}</span>
                        <span className="text-xs text-slate-400">{fromSignup}%</span>
                      </div>
                    </div>
                    <div className="h-7 w-full overflow-hidden rounded-lg bg-slate-100">
                      <div
                        className="flex h-full items-center rounded-lg bg-brand-500 px-2 text-xs font-medium text-white transition-all"
                        style={{ width: `${width}%` }}
                      >
                        {width > 12 ? new Intl.NumberFormat("fr-FR").format(step.value) : ""}
                      </div>
                    </div>
                    {fromPrev !== null && (
                      <p className="mt-1 text-xs text-slate-400">
                        {fromPrev}% de l'étape précédente
                        {fromPrev < 50 && step.value < steps[i - 1].value ? " — point de fuite à surveiller" : ""}
                      </p>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
