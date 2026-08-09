import Link from "next/link";

const tabs = [
  { href: "/donnees", label: "Explorateur", exact: true },
  { href: "/donnees/schema", label: "Schéma des données", exact: false },
];

export default function DonneesLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="space-y-5">
      <div className="flex gap-1 border-b border-slate-200 pb-3">
        {tabs.map((t) => (
          <Link
            key={t.href}
            href={t.href}
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900"
          >
            {t.label}
          </Link>
        ))}
      </div>
      {children}
    </div>
  );
}
