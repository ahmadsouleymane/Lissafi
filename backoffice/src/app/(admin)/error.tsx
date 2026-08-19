"use client";

import { useEffect } from "react";
import { Button, Card, PageHeader } from "@/components/ui";

export default function AdminError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error("[admin] erreur de rendu:", error);
  }, [error]);

  return (
    <div>
      <PageHeader title="Une erreur est survenue" subtitle="Le back-office a rencontré un problème inattendu." />
      <Card className="mt-6 p-6">
        <p className="text-sm text-slate-600">
          Souvent temporaire (connexion à Supabase interrompue). Réessaie — si le problème persiste, vérifie les{" "}
          <a href="/reglages" className="text-brand-600 hover:underline">réglages</a> ou les logs Vercel.
        </p>
        {error.digest && <p className="mt-2 text-xs text-slate-400">Référence : {error.digest}</p>}
        <Button className="mt-4" onClick={reset}>Réessayer</Button>
      </Card>
    </div>
  );
}
