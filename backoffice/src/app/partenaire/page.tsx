import Link from "next/link";
import { redirect } from "next/navigation";
import { PortalShell } from "@/components/PortalShell";
import { Button } from "@/components/ui";
import { getPartnerSession } from "@/lib/session";
import { COMMISSIONS, PLAN_LABELS } from "@/lib/partners";
import { formatFCFA } from "@/lib/format";
import type { Plan } from "@/types";

export const dynamic = "force-dynamic";

export const metadata = { title: "Devenir partenaire" };

const STEPS = [
  {
    n: 1,
    title: "Inscris-toi gratuitement",
    body: "Crée ton compte en 30 secondes : nom, téléphone, email. Actif immédiatement, aucune validation à attendre.",
  },
  {
    n: 2,
    title: "Partage ton lien",
    body: "Tu reçois un lien perso et un QR code à mettre dans ton statut WhatsApp, ta bio TikTok ou sur une affichette.",
  },
  {
    n: 3,
    title: "Tes clients s'abonnent",
    body: "Chaque commerçant qui active Lissafi grâce à ton lien t'est attribué automatiquement.",
  },
  {
    n: 4,
    title: "Sois payé en espèces",
    body: "Ta commission tombe. Tu demandes un retrait quand tu veux, payé par Orange Money ou Moov.",
  },
];

const BENEFITS = [
  ["Paiement en espèces", "Orange Money ou Moov, pas de bon d'achat ni de points."],
  ["Sans plafond", "Plus tu amènes de clients, plus tu gagnes. Aucune limite."],
  ["Zéro investissement", "Pas de stock à acheter, pas de frais. Juste ton lien."],
  ["Suivi en temps réel", "Visites, clients, gains : tout est visible dans ton espace."],
];

const FAQ = [
  ["Combien je gagne ?", "Une commission fixe par client payant : 5 000 à 15 000 F selon l'abonnement choisi. Une nouvelle commission à chaque renouvellement."],
  ["Quand suis-je payé ?", "Dès qu'un client est confirmé, la commission est « due ». Tu demandes un retrait quand tu veux, on te paie après validation."],
  ["Est-ce vraiment gratuit ?", "Oui. L'inscription et la participation sont 100 % gratuites, sans engagement."],
  ["Comment mes clients sont-ils comptés ?", "Ton lien contient ton code. Quand un client active Lissafi via ce lien, il t'est attribué automatiquement."],
];

export default async function PartnerHomePage() {
  const session = await getPartnerSession();
  if (session) redirect("/partenaire/espace");

  const maxCommission = Math.max(...Object.values(COMMISSIONS));

  return (
    <PortalShell maxWidth="max-w-3xl">
      {/* ---------- Hero ---------- */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-brand-700 via-brand-600 to-brand-800 px-6 py-12 text-center text-white shadow-xl sm:px-10 sm:py-14">
        {/* Filigrane décoratif : la marque géométrique Lissafi */}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src="/assets/logo-icon.svg"
          alt=""
          aria-hidden
          className="pointer-events-none absolute -right-8 -top-8 h-44 w-44 opacity-10 sm:h-56 sm:w-56"
        />
        <span className="relative inline-flex rounded-full bg-white/15 px-3 py-1 text-xs font-semibold uppercase tracking-wide ring-1 ring-white/25">
          Programme partenaire
        </span>
        <h1 className="relative mt-5 text-3xl font-extrabold leading-tight sm:text-4xl">
          Gagne de l'argent en présentant Lissafi.
        </h1>
        <p className="relative mx-auto mt-3 max-w-xl text-sm text-white/85 sm:text-base">
          Partage ton lien. À chaque commerçant qui s'abonne grâce à toi, tu touches
          jusqu'à <strong className="font-bold text-white">{formatFCFA(maxCommission)}</strong> en
          espèces. Sans stock, sans plafond, payé sur mobile money.
        </p>
        <div className="relative mt-7 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <Link href="/partenaire/inscription" className="w-full sm:w-auto">
            <Button size="lg" className="w-full bg-white text-brand-700 shadow-sm hover:bg-white/90 sm:w-auto">
              Créer mon compte partenaire
            </Button>
          </Link>
          <Link href="/partenaire/connexion" className="w-full sm:w-auto">
            <Button size="lg" variant="ghost" className="w-full text-white hover:bg-white/10 sm:w-auto">
              J'ai déjà un compte
            </Button>
          </Link>
        </div>
        <p className="relative mt-4 text-xs text-white/70">Gratuit · actif immédiatement · sans engagement</p>
      </section>

      {/* ---------- Comment ça marche ---------- */}
      <section className="mt-12">
        <h2 className="text-center text-xs font-semibold uppercase tracking-wide text-brand-600">Comment ça marche</h2>
        <p className="mt-1 text-center text-xl font-bold text-slate-900">4 étapes, et tu gagnes.</p>
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          {STEPS.map((s) => (
            <div key={s.n} className="relative rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-50 text-sm font-bold text-brand-700">
                {s.n}
              </span>
              <h3 className="mt-3 text-sm font-semibold text-slate-900">{s.title}</h3>
              <p className="mt-1 text-sm leading-relaxed text-slate-600">{s.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ---------- Barème ---------- */}
      <section className="mt-12">
        <h2 className="text-center text-xs font-semibold uppercase tracking-wide text-brand-600">Ta commission</h2>
        <p className="mt-1 text-center text-xl font-bold text-slate-900">Payée en espèces, par client.</p>
        <div className="mt-6 grid gap-4 sm:grid-cols-3">
          {(Object.keys(COMMISSIONS) as Plan[]).map((p) => (
            <div key={p} className="rounded-2xl border border-slate-200 bg-white p-5 text-center shadow-sm">
              <p className="text-sm font-medium text-slate-500">{PLAN_LABELS[p]}</p>
              <p className="mt-2 text-3xl font-extrabold text-brand-600">{COMMISSIONS[p].toLocaleString("fr-FR")}</p>
              <p className="text-xs font-medium text-slate-400">FCFA par client</p>
            </div>
          ))}
        </div>
        <p className="mt-4 text-center text-sm text-slate-500">
          Exemple : 5 commerçants sur {PLAN_LABELS.business} ={" "}
          <strong className="font-bold text-slate-700">{formatFCFA(COMMISSIONS.business * 5)}</strong> pour toi.
        </p>
      </section>

      {/* ---------- Pourquoi ---------- */}
      <section className="mt-12">
        <h2 className="text-center text-xs font-semibold uppercase tracking-wide text-brand-600">Pourquoi te lancer</h2>
        <p className="mt-1 text-center text-xl font-bold text-slate-900">Un revenu en plus, sans effort.</p>
        <div className="mt-6 grid gap-3 sm:grid-cols-2">
          {BENEFITS.map(([title, body]) => (
            <div key={title} className="flex gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-brand-600 text-xs font-bold text-white">✓</span>
              <div>
                <p className="text-sm font-semibold text-slate-900">{title}</p>
                <p className="text-sm text-slate-600">{body}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ---------- FAQ ---------- */}
      <section className="mt-12">
        <h2 className="text-center text-xs font-semibold uppercase tracking-wide text-brand-600">Questions fréquentes</h2>
        <div className="mt-6 space-y-3">
          {FAQ.map(([q, a]) => (
            <details key={q} className="group rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <summary className="cursor-pointer list-none text-sm font-semibold text-slate-900 marker:hidden [&::-webkit-details-marker]:hidden">
                <span className="flex items-center justify-between gap-3">
                  {q}
                  <span className="text-brand-600 transition-transform group-open:rotate-45">+</span>
                </span>
              </summary>
              <p className="mt-2 text-sm leading-relaxed text-slate-600">{a}</p>
            </details>
          ))}
        </div>
      </section>

      {/* ---------- CTA final ---------- */}
      <section className="mt-12 rounded-3xl bg-slate-900 px-6 py-10 text-center text-white">
        <h2 className="text-2xl font-bold">Prêt à gagner avec Lissafi ?</h2>
        <p className="mx-auto mt-2 max-w-md text-sm text-white/80">
          Crée ton compte maintenant, récupère ton lien, et commence à toucher tes premières commissions.
        </p>
        <Link href="/partenaire/inscription" className="mt-6 inline-block">
          <Button size="lg" className="bg-brand-500 text-white hover:bg-brand-400">Devenir partenaire — c'est parti</Button>
        </Link>
      </section>
    </PortalShell>
  );
}
