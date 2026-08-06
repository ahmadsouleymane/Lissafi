import { updateAdminSettingQuickForm } from "@/actions/admin";
import { Badge, Button, Card, CardHeader, Input, PageHeader } from "@/components/ui";
import { getAdminSettings } from "@/lib/data";
import { isConfigured } from "@/lib/supabase";

export default async function ReglagesPage() {
  const settings = await getAdminSettings();
  const configured = isConfigured();

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      <PageHeader title="Réglages" subtitle="Configuration globale du back-office" />

      {/* État de la configuration */}
      <Card>
        <CardHeader title="État de la connexion Supabase" subtitle="Variables d'environnement (côté serveur uniquement)" />
        <div className="space-y-2 px-5 pb-5 text-sm">
          <div className="flex items-center justify-between">
            <span className="text-slate-600">Configuration générale</span>
            {configured ? <Badge color="green">Configuré</Badge> : <Badge color="red">Non configuré</Badge>}
          </div>
          <div className="flex items-center justify-between">
            <span className="text-slate-600">Clé service_role présente</span>
            <Badge color={configured ? "green" : "red"}>{configured ? "Oui" : "Non"}</Badge>
          </div>
          <p className="text-xs text-slate-400">
            Renseigne <code className="rounded bg-slate-100 px-1">SUPABASE_ANON_KEY</code> et{" "}
            <code className="rounded bg-slate-100 px-1">SUPABASE_SERVICE_ROLE_KEY</code> (voir <code className="rounded bg-slate-100 px-1">.env.example</code>).
          </p>
        </div>
      </Card>

      {/* Paramètres métier */}
      <Card>
        <CardHeader title="Paramètres métier" subtitle="Utilisés comme valeurs par défaut des actions admin" />
        <div className="space-y-4 px-5 pb-5">
          <SettingForm label="Prix premium (FCFA / an)" hint="Informationnel — l'encaissement reste manuel (WhatsApp / mobile money)" name="premium_price_fcfa" value={settings["premium_price_fcfa"] ?? "10000"} />
          <SettingForm label="Durée premium (jours)" hint="Durée par défaut du bouton « Activer 1 an »" name="premium_days" value={settings["premium_days"] ?? "365"} />
          <SettingForm label="Durée démo (jours)" hint="Durée de la période d'essai gratuite" name="demo_days" value={settings["demo_days"] ?? "7"} />
        </div>
      </Card>

      {/* Ajout d'un admin */}
      <Card>
        <CardHeader title="Ajouter un administrateur" subtitle="Un admin est un compte Supabase Auth ajouté à la table admins" />
        <div className="px-5 pb-5">
          <ol className="list-decimal space-y-2 pl-5 text-sm text-slate-600">
            <li>
              Crée un compte avec l'email admin dans Supabase → <em>Authentication → Users → Add user</em> (ou depuis l'app, à la connexion).
            </li>
            <li>
              Copie son <code className="rounded bg-slate-100 px-1">user_id</code> (UUID) et exécute dans le SQL Editor :
            </li>
          </ol>
          <pre className="mt-3 overflow-x-auto rounded-lg bg-slate-900 p-4 text-xs text-slate-100">
{`INSERT INTO public.admins (user_id, email)
VALUES ('<UUID_DU_COMPTE>', 'ton@email.com');`}
          </pre>
          <p className="mt-3 text-xs text-slate-400">
            Tu peux aussi te créer un compte admin directement depuis cette page de connexion, puis l'ajouter à la table.
          </p>
        </div>
      </Card>
    </div>
  );
}

function SettingForm({ label, hint, name, value }: { label: string; hint?: string; name: string; value: string }) {
  return (
    <form action={updateAdminSettingQuickForm} className="flex flex-wrap items-end gap-3">
      <input type="hidden" name="key" value={name} />
      <label className="min-w-40 flex-1">
        <span className="mb-1 block text-xs font-medium text-slate-600">{label}</span>
        <Input type="text" name="value" defaultValue={value} />
      </label>
      <Button type="submit" variant="secondary" size="sm">Enregistrer</Button>
      {hint && <p className="w-full text-xs text-slate-400">{hint}</p>}
    </form>
  );
}
