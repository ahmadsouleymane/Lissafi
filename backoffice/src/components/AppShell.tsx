"use client";

import { ReactNode, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Logo } from "./Logo";
import {
  IconActivity,
  IconBell,
  IconChat,
  IconCrown,
  IconDatabase,
  IconGrid,
  IconHandshake,
  IconLogIn,
  IconLogout,
  IconMenu,
  IconClose,
  IconSettings,
  IconUsers,
} from "./icons";
import { logout } from "@/actions/auth";
import { cx } from "./ui";

const NAV = [
  { href: "/", label: "Tableau de bord", icon: IconGrid, exact: true },
  { href: "/comptes", label: "Comptes", icon: IconUsers },
  { href: "/premium", label: "Premium", icon: IconCrown },
  { href: "/partenaires", label: "Partenaires", icon: IconHandshake },
  { href: "/notifications", label: "Notifications", icon: IconBell },
  { href: "/logs", label: "Erreurs & activité", icon: IconActivity },
  { href: "/connexions", label: "Connexions", icon: IconLogIn },
  { href: "/support", label: "Support", icon: IconChat },
  { href: "/reglages", label: "Réglages", icon: IconSettings },
  { href: "/donnees", label: "Données", icon: IconDatabase },
];

function NavLinks({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  return (
    <nav className="flex flex-col gap-1 px-3">
      {NAV.map((item) => {
        const active = item.exact ? pathname === item.href : pathname.startsWith(item.href);
        const Icon = item.icon;
        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={onNavigate}
            className={cx(
              "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
              active ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
            )}
          >
            <Icon size={18} />
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}

export function AppShell({ email, children }: { email: string; children: ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);

  const sidebarContent = (
    <div className="flex h-full flex-col">
      <div className="flex h-16 items-center px-5">
        <Logo />
      </div>
      <div className="flex-1 overflow-y-auto py-2">
        <NavLinks onNavigate={() => setMobileOpen(false)} />
      </div>
      <div className="border-t border-slate-200 p-3">
        <div className="mb-2 px-3 text-xs text-slate-500">
          <p className="font-medium text-slate-600">{email}</p>
          <p>Administrateur Lissafi</p>
        </div>
        <form action={logout}>
          <button
            type="submit"
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
          >
            <IconLogout size={18} />
            Se déconnecter
          </button>
        </form>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen lg:pl-64">
      {/* Sidebar bureau */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-slate-200 bg-white lg:block">
        {sidebarContent}
      </aside>

      {/* Drawer mobile */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 bg-slate-900/40" onClick={() => setMobileOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-72 bg-white shadow-xl">
            <button
              className="absolute right-3 top-4 rounded-lg p-1.5 text-slate-500 hover:bg-slate-100"
              onClick={() => setMobileOpen(false)}
              aria-label="Fermer le menu"
            >
              <IconClose size={20} />
            </button>
            {sidebarContent}
          </aside>
        </div>
      )}

      {/* Barre supérieure mobile */}
      <header className="sticky top-0 z-20 flex h-14 items-center gap-3 border-b border-slate-200 bg-surface/90 px-4 backdrop-blur lg:hidden">
        <button className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100" onClick={() => setMobileOpen(true)} aria-label="Ouvrir le menu">
          <IconMenu size={22} />
        </button>
        <Logo withText={false} />
        <span className="font-bold text-slate-900">Lissafi Admin</span>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-6 lg:px-8 lg:py-8">{children}</main>
    </div>
  );
}
