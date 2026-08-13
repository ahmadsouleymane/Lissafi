import { ReactNode } from "react";
import Link from "next/link";

// En-tête + conteneur partagés des pages du portail partenaire (public).
// Volontairement séparé de l'AppShell admin (aucune session admin ici).
export function PortalShell({ children, maxWidth = "max-w-md" }: { children: ReactNode; maxWidth?: string }) {
  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-14 max-w-4xl items-center gap-2.5 px-4">
          <Link href="/partenaire" className="flex items-center gap-2.5">
            <svg width="30" height="30" viewBox="0 0 48 48" fill="none" aria-hidden>
              <rect width="48" height="48" rx="10" fill="#2e8b57" />
              <path
                d="M12 14h3l2.5 14a3 3 0 0 0 3 2.5h12.2a3 3 0 0 0 3-2.4l2.3-11.1H15.6"
                stroke="white"
                strokeWidth="2.6"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <circle cx="20.5" cy="35" r="2.1" fill="white" />
              <circle cx="31.5" cy="35" r="2.1" fill="white" />
              <path d="M30 18.5l2.5 2.5L38 15" stroke="white" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <span className="text-base font-bold text-slate-900">
              Lissafi <span className="font-medium text-brand-600">Partenaires</span>
            </span>
          </Link>
        </div>
      </header>
      <main className="mx-auto w-full px-4 py-8">
        <div className={`mx-auto ${maxWidth}`}>{children}</div>
      </main>
    </div>
  );
}
