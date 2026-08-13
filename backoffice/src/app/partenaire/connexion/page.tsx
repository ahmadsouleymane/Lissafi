import { redirect } from "next/navigation";
import { PortalShell } from "@/components/PortalShell";
import { PartnerSignInForm } from "@/components/PartnerAuthForms";
import { Card } from "@/components/ui";
import { getPartnerSession } from "@/lib/session";

export const dynamic = "force-dynamic";

export const metadata = { title: "Connexion partenaire" };

export default async function PartnerSignInPage() {
  const session = await getPartnerSession();
  if (session) redirect("/partenaire/espace");

  return (
    <PortalShell>
      <div className="mb-5 text-center">
        <h1 className="text-xl font-bold text-slate-900">Espace partenaire</h1>
        <p className="mt-1 text-sm text-slate-500">Connecte-toi pour suivre tes gains.</p>
      </div>
      <Card className="p-5">
        <PartnerSignInForm />
      </Card>
    </PortalShell>
  );
}
