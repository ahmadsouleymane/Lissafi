import { ReactNode } from "react";
import Link from "next/link";

// En-tête + conteneur partagés des pages du portail partenaire.
// `bleed` = la page gère elle-même ses sections pleine largeur (accueil).
export function PortalShell({
  children,
  maxWidth = "max-w-md",
  bleed = false,
}: {
  children: ReactNode;
  maxWidth?: string;
  bleed?: boolean;
}) {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/85 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4 sm:px-6">
          <Link href="/partenaire" className="flex items-center gap-2.5">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/assets/logo-header.svg" alt="Lissafi" className="h-6 w-auto" />
            <span className="hidden text-sm font-semibold text-brand-700 sm:inline">Partenaires</span>
          </Link>
          <Link
            href="/partenaire/connexion"
            className="text-sm font-medium text-slate-600 transition-colors hover:text-brand-700"
          >
            Se connecter
          </Link>
        </div>
      </header>

      {bleed ? (
        <main className="flex-1">{children}</main>
      ) : (
        <main className="mx-auto w-full flex-1 px-4 py-8">
          <div className={`mx-auto ${maxWidth}`}>{children}</div>
        </main>
      )}

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl flex-col items-center gap-1 px-4 py-6 text-center sm:flex-row sm:justify-between sm:text-left">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/assets/logo-header.svg" alt="Lissafi" className="h-5 w-auto opacity-70" />
          <p className="text-xs text-slate-400">Ta caisse, simplement · © 2026 Lissafi</p>
        </div>
      </footer>
    </div>
  );
}
