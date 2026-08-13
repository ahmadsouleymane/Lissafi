import { ReactNode } from "react";
import Link from "next/link";

// En-tête + conteneur partagés des pages du portail partenaire (public).
// Volontairement séparé de l'AppShell admin (aucune session admin ici).
export function PortalShell({ children, maxWidth = "max-w-md" }: { children: ReactNode; maxWidth?: string }) {
  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-14 max-w-4xl items-center justify-between px-4">
          <Link href="/partenaire" className="flex items-center gap-2.5">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/assets/logo-header.svg" alt="Lissafi" className="h-6 w-auto" />
            <span className="hidden text-sm font-semibold text-brand-700 sm:inline">Partenaires</span>
          </Link>
          <span className="text-xs font-medium text-slate-400">Ta caisse, simplement</span>
        </div>
      </header>
      <main className="mx-auto w-full px-4 py-8">
        <div className={`mx-auto ${maxWidth}`}>{children}</div>
      </main>
    </div>
  );
}
