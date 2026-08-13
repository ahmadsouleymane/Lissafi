import { redirect } from "next/navigation";
import { PortalShell } from "@/components/PortalShell";
import { PartnerSignUpForm } from "@/components/PartnerAuthForms";
import { Card } from "@/components/ui";
import { getPartnerSession } from "@/lib/session";

export const dynamic = "force-dynamic";

export const metadata = { title: "Inscription partenaire" };

export default async function PartnerSignUpPage() {
  const session = await getPartnerSession();
  if (session) redirect("/partenaire/espace");

  return (
    <PortalShell>
      <div className="mb-5 text-center">
        <h1 className="text-xl font-bold text-slate-900">Créer mon compte partenaire</h1>
        <p className="mt-1 text-sm text-slate-500">Gratuit, actif immédiatement.</p>
      </div>
      <Card className="p-5">
        <PartnerSignUpForm />
      </Card>
    </PortalShell>
  );
}
