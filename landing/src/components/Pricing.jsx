import { APK } from "../config";
import { Kicker, Reveal } from "./ui";

const TIERS = [
  {
    name: "Gratuit",
    amount: "0 F",
    period: "pour découvrir la caisse",
    features: [
      ["10 produits au catalogue", true],
      ["10 clients suivis", true],
      ["Ventes illimitées", true],
      ["Historique complet et illimité", true],
      ["Marche sans réseau", true],
      ["Gestion de stock complète", false],
      ["Rapports & export illimités", false],
    ],
    cta: "Télécharger l'APK",
  },
  {
    name: "Lissafi Essentiel",
    badge: "Populaire",
    amount: "25 000 F",
    period: "/an",
    sub: "= 68 F par jour · tout illimité",
    features: [
      ["Produits illimités", true],
      ["Clients & crédits illimités", true],
      ["Ventes illimitées chaque jour", true],
      ["Historique illimité", true],
      ["Gestion de stock complète", true],
      ["Rapports jour / semaine / mois", true],
      ["Export CSV de tes données", true],
      ["Support prioritaire", true],
    ],
    cta: "Télécharger l'APK",
    note: "Paiement en espèces ou mobile money · activation dans l'app",
  },
  {
    name: "Lissafi Premium",
    badge: "Tout inclus",
    top: true,
    amount: "100 000 F",
    period: "/an",
    sub: "= 274 F par jour · le service complet",
    features: [
      ["Tout Lissafi Essentiel", true],
      ["Sauvegarde cloud renforcée", true],
      ["Accompagnement au démarrage", true],
      ["Formation de ton équipe sur place", true],
      ["Support prioritaire", true],
      ["Accès en avant-première aux nouveautés", true],
    ],
    cta: "Télécharger l'APK",
    note: "Paiement en espèces ou mobile money · activation dans l'app",
  },
];

export default function Pricing() {
  return (
    <section className="section rail" id="tarifs">
      <div className="container">
        <Reveal className="section-head center">
          <Kicker>Les tarifs</Kicker>
          <h2>Un tarif pour chaque étape de ta boutique.</h2>
        </Reveal>

        <div className="pricing-grid">
          {TIERS.map((t, i) => (
            <Reveal
              key={t.name}
              delay={i * 0.08}
              className={`price-card ${t.top ? "top" : ""} ${
                t.badge === "Populaire" ? "premium" : ""
              }`}
            >
              {t.badge && (
                <span className={`price-badge ${t.top ? "top" : ""}`}>
                  {t.badge}
                </span>
              )}
              <div className="price-plan">{t.name}</div>
              <div className="price-amount">
                {t.amount}
                <small>{t.period}</small>
              </div>
              {t.sub && <div className="price-sub">{t.sub}</div>}
              <ul className="price-features">
                {t.features.map(([f, on]) => (
                  <li key={f} className={on ? "" : "off"}>
                    {f}
                  </li>
                ))}
              </ul>
              <a
                className={`btn ${
                  t.top || t.badge === "Populaire" ? "btn-primary" : "btn-ghost"
                }`}
                href={APK}
                download
              >
                {t.cta}
              </a>
              {t.note && <p className="price-note">{t.note}</p>}
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
