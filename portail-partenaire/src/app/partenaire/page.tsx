import Link from "next/link";
import { redirect } from "next/navigation";
import { PortalShell } from "@/components/PortalShell";
import { Reveal } from "@/components/PortalReveal";
import { getPartnerSession } from "@/lib/session";
import { COMMISSIONS, PLAN_LABELS } from "@/lib/partners";
import { formatFCFA } from "@/lib/format";
import type { Plan } from "@/types";

export const dynamic = "force-dynamic";

export const metadata = { title: "Devenir partenaire" };

const STEPS = [
  { n: "1", title: "Inscris-toi", body: "Compte gratuit en 30 secondes. Actif immédiatement, sans validation à attendre." },
  { n: "2", title: "Partage ton lien", body: "Tu reçois un lien perso et un QR code à poser sur WhatsApp, TikTok ou une affichette." },
  { n: "3", title: "Tes clients s'abonnent", body: "Chaque commerçant qui active Lissafi grâce à ton lien t'est attribué automatiquement." },
  { n: "4", title: "Sois payé", body: "Ta commission tombe. Tu demandes un retrait quand tu veux, payé sur mobile money." },
];

const BENEFITS = [
  ["💵", "Payé en mobile money", "De l'argent réel sur ton téléphone, pas des points ni des bons d'achat."],
  ["🚀", "Sans plafond", "Plus tu amènes de clients, plus tu gagnes. Aucune limite de gains."],
  ["🎒", "Zéro investissement", "Pas de stock à acheter, aucun frais. Juste ton lien à partager."],
  ["📊", "Suivi en temps réel", "Visites, clients, commissions : tout est visible dans ton espace."],
];

const FAQ = [
  ["Combien je gagne ?", "Une commission fixe par client payant, de 5 000 à 15 000 F selon l'abonnement choisi. Et une nouvelle commission à chaque renouvellement."],
  ["Quand suis-je payé ?", "Dès qu'un client est confirmé, ta commission est « due ». Tu demandes un retrait quand tu veux, on te paie après validation."],
  ["Est-ce vraiment gratuit ?", "Oui, à 100 %. L'inscription et la participation sont gratuites, sans engagement."],
  ["Comment mes clients sont-ils comptés ?", "Ton lien contient ton code unique. Quand un client active Lissafi via ce lien, il t'est attribué automatiquement."],
];

export default async function PartnerHomePage() {
  const session = await getPartnerSession();
  if (session) redirect("/partenaire/espace");

  const maxCommission = Math.max(...Object.values(COMMISSIONS));

  return (
    <PortalShell bleed>
      {/* ============ HERO ============ */}
      <section className="relative overflow-hidden bg-gradient-to-br from-[#04120b] via-brand-900 to-[#071c12] text-white">
        <div className="portal-noise absolute inset-0 opacity-60" aria-hidden />
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src="/assets/logo-icon.svg"
          alt=""
          aria-hidden
          className="pointer-events-none absolute -right-16 -top-10 h-72 w-72 opacity-[0.07] sm:h-96 sm:w-96"
        />
        <div className="absolute -left-24 top-1/3 h-72 w-72 rounded-full bg-brand-500/20 blur-3xl" aria-hidden />

        <div className="relative mx-auto max-w-5xl px-4 py-20 text-center sm:px-6 sm:py-28">
          <Reveal delay={0}>
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1.5 text-xs font-semibold uppercase tracking-widest text-brand-200 ring-1 ring-white/15">
              Programme partenaire Lissafi
            </span>
          </Reveal>

          <Reveal delay={0.08}>
            <h1 className="mx-auto mt-6 max-w-3xl text-4xl font-extrabold leading-[1.08] tracking-tight sm:text-6xl">
              Gagne de l'argent en présentant{" "}
              <span className="bg-gradient-to-r from-brand-300 to-emerald-200 bg-clip-text text-transparent">Lissafi</span>.
            </h1>
          </Reveal>

          <Reveal delay={0.16}>
            <p className="mx-auto mt-5 max-w-xl text-base text-white/75 sm:text-lg">
              Partage ton lien. À chaque commerçant qui s'abonne grâce à toi, tu touches
              jusqu'à <strong className="font-bold text-white">{formatFCFA(maxCommission)}</strong> —
              payé sur mobile money. Sans stock, sans plafond.
            </p>
          </Reveal>

          <Reveal delay={0.24}>
            <div className="mt-9 flex flex-col items-center justify-center gap-3 sm:flex-row">
              <Link
                href="/partenaire/inscription"
                className="w-full rounded-xl bg-white px-7 py-3.5 text-center text-base font-semibold text-brand-800 shadow-lg shadow-black/20 transition-transform hover:-translate-y-0.5 sm:w-auto"
              >
                Créer mon compte partenaire
              </Link>
              <Link
                href="/partenaire/connexion"
                className="w-full rounded-xl px-7 py-3.5 text-center text-base font-semibold text-white ring-1 ring-white/25 transition-colors hover:bg-white/10 sm:w-auto"
              >
                J'ai déjà un compte
              </Link>
            </div>
          </Reveal>

          <Reveal delay={0.32}>
            <p className="mt-5 text-xs font-medium text-white/55">
              Gratuit · actif tout de suite · payé sur mobile money
            </p>
          </Reveal>

          {/* Stats strip */}
          <Reveal delay={0.4}>
            <div className="mx-auto mt-14 grid max-w-2xl grid-cols-3 gap-3 rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur">
              {[
                [formatFCFA(maxCommission), "par client Business"],
                ["0 F", "pour démarrer"],
                ["∞", "gains, sans plafond"],
              ].map(([big, small]) => (
                <div key={small}>
                  <p className="text-xl font-extrabold text-white sm:text-2xl">{big}</p>
                  <p className="mt-1 text-[11px] leading-tight text-white/60 sm:text-xs">{small}</p>
                </div>
              ))}
            </div>
          </Reveal>
        </div>
      </section>

      {/* ============ COMMENT ÇA MARCHE ============ */}
      <section className="bg-slate-50 py-20 sm:py-24">
        <div className="mx-auto max-w-5xl px-4 sm:px-6">
          <Reveal className="text-center">
            <p className="text-xs font-bold uppercase tracking-widest text-brand-600">Comment ça marche</p>
            <h2 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">4 étapes, et tu gagnes.</h2>
          </Reveal>

          <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {STEPS.map((s, i) => (
              <Reveal key={s.n} delay={i * 0.08} className="group relative rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
                <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-600 text-lg font-extrabold text-white shadow-sm">
                  {s.n}
                </span>
                <h3 className="mt-4 text-base font-bold text-slate-900">{s.title}</h3>
                <p className="mt-1.5 text-sm leading-relaxed text-slate-600">{s.body}</p>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ============ BARÈME ============ */}
      <section className="relative overflow-hidden bg-gradient-to-b from-white to-slate-50 py-20 sm:py-24">
        <div className="mx-auto max-w-5xl px-4 sm:px-6">
          <Reveal className="text-center">
            <p className="text-xs font-bold uppercase tracking-widest text-brand-600">Ta commission</p>
            <h2 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">Payée en espèces, par client.</h2>
          </Reveal>

          <div className="mt-12 grid gap-5 sm:grid-cols-3">
            {(Object.keys(COMMISSIONS) as Plan[]).map((p, i) => {
              const featured = p === "business";
              return (
                <Reveal
                  key={p}
                  delay={i * 0.08}
                  className={
                    featured
                      ? "relative rounded-3xl bg-gradient-to-br from-brand-700 to-brand-900 p-7 text-center text-white shadow-xl sm:-translate-y-2"
                      : "relative rounded-3xl border border-slate-200 bg-white p-7 text-center shadow-sm"
                  }
                >
                  {featured && (
                    <span className="absolute left-1/2 top-4 -translate-x-1/2 rounded-full bg-accent-500 px-3 py-0.5 text-[10px] font-bold uppercase tracking-wide text-white">
                      Le plus rentable
                    </span>
                  )}
                  <p className={featured ? "mt-4 text-sm font-medium text-white/70" : "text-sm font-medium text-slate-500"}>
                    {PLAN_LABELS[p]}
                  </p>
                  <p className={featured ? "mt-2 text-4xl font-extrabold text-white" : "mt-2 text-4xl font-extrabold text-brand-600"}>
                    {COMMISSIONS[p].toLocaleString("fr-FR")}
                  </p>
                  <p className={featured ? "text-xs font-medium text-white/60" : "text-xs font-medium text-slate-400"}>
                    FCFA par client
                  </p>
                </Reveal>
              );
            })}
          </div>

          <Reveal delay={0.1}>
            <p className="mt-8 text-center text-sm text-slate-500">
              Exemple : 5 commerçants sur {PLAN_LABELS.business} ={" "}
              <strong className="font-bold text-brand-700">{formatFCFA(COMMISSIONS.business * 5)}</strong> pour toi.
            </p>
          </Reveal>
        </div>
      </section>

      {/* ============ POURQUOI ============ */}
      <section className="bg-slate-900 py-20 text-white sm:py-24">
        <div className="mx-auto max-w-5xl px-4 sm:px-6">
          <Reveal className="text-center">
            <p className="text-xs font-bold uppercase tracking-widest text-brand-400">Pourquoi te lancer</p>
            <h2 className="mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">Un revenu en plus, sans effort.</h2>
          </Reveal>

          <div className="mt-12 grid gap-4 sm:grid-cols-2">
            {BENEFITS.map(([emoji, title, body], i) => (
              <Reveal key={title} delay={i * 0.08} className="flex gap-4 rounded-2xl border border-white/10 bg-white/[0.04] p-5">
                <span className="text-2xl">{emoji}</span>
                <div>
                  <p className="text-base font-bold text-white">{title}</p>
                  <p className="mt-1 text-sm leading-relaxed text-white/65">{body}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ============ FAQ ============ */}
      <section className="bg-slate-50 py-20 sm:py-24">
        <div className="mx-auto max-w-3xl px-4 sm:px-6">
          <Reveal className="text-center">
            <p className="text-xs font-bold uppercase tracking-widest text-brand-600">Questions fréquentes</p>
            <h2 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">Tout est clair.</h2>
          </Reveal>

          <div className="mt-10 space-y-3">
            {FAQ.map(([q, a], i) => (
              <Reveal key={q} delay={i * 0.05}>
                <details className="group rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <summary className="flex cursor-pointer list-none items-center justify-between gap-3 text-base font-semibold text-slate-900 [&::-webkit-details-marker]:hidden">
                    {q}
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-brand-50 text-lg font-bold text-brand-600 transition-transform group-open:rotate-45">
                      +
                    </span>
                  </summary>
                  <p className="mt-3 text-sm leading-relaxed text-slate-600">{a}</p>
                </details>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ============ CTA FINAL ============ */}
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-700 to-brand-900 py-20 text-center text-white sm:py-24">
        <div className="portal-noise absolute inset-0 opacity-50" aria-hidden />
        <div className="relative mx-auto max-w-2xl px-4 sm:px-6">
          <Reveal>
            <h2 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Prêt à gagner avec Lissafi ?</h2>
            <p className="mx-auto mt-3 max-w-md text-base text-white/80">
              Crée ton compte, récupère ton lien, et touche tes premières commissions.
            </p>
            <Link
              href="/partenaire/inscription"
              className="mt-8 inline-block rounded-xl bg-white px-8 py-4 text-base font-bold text-brand-800 shadow-lg shadow-black/20 transition-transform hover:-translate-y-0.5"
            >
              Devenir partenaire — c'est parti
            </Link>
          </Reveal>
        </div>
      </section>
    </PortalShell>
  );
}
