"use client";

import { useState } from "react";
import { Button, Card } from "./ui";

// Carte "mon lien" : lien personnel + QR téléchargeable + copier.
export function PartnerLinkCard({ link, qrUrl }: { link: string; qrUrl: string }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(link);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard indisponible (contexte non sécurisé) — on ignore.
    }
  }

  return (
    <Card className="p-5">
      <h2 className="text-sm font-semibold text-slate-900">Mon lien de partage</h2>
      <p className="mt-1 text-xs text-slate-500">
        Diffuse-le sur ton statut WhatsApp, ta bio TikTok ou en affichette. Chaque client qui
        s'abonne via ce lien t'est attribué.
      </p>

      <div className="mt-4 flex flex-col gap-4 sm:flex-row sm:items-center">
        <div className="shrink-0 self-center rounded-xl border border-slate-200 bg-white p-2">
          {/* QR généré par un service public — image téléchargeable. */}
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={qrUrl} alt="QR code de mon lien partenaire" width={160} height={160} className="h-40 w-40" />
        </div>

        <div className="min-w-0 flex-1">
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <p className="break-all text-xs font-medium text-slate-700">{link}</p>
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            <Button size="sm" onClick={copy}>{copied ? "Lien copié ✓" : "Copier le lien"}</Button>
            <a href={`${qrUrl}&download=1`} download="lissafi-qr.png">
              <Button size="sm" variant="secondary">Télécharger le QR</Button>
            </a>
          </div>
        </div>
      </div>
    </Card>
  );
}
