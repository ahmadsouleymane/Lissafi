import { Printer } from "lucide-react";
import { copyPartnerCodeForDownload, downloadUrl } from "../lib/tracking";
import { Kicker, Reveal } from "./ui";

const TIERS = [
  {
    name: "Gratuit",
    amount: "0 F",
    period: "pour découvrir la caisse",
    features: [
      ["10 produits au catalogue", true],
      ["10 clients suivis", true],
      ["10 ventes par jour", true],
      ["Marche sans réseau", true],
    ],
    cta: "Télécharger l'APK",
  },
  {
    name: "Lissafi Plus",
    badge: "Populaire",
    amount: "30 000 F",
    period: "/an",
    sub: "= 82 F par jour",
    features: [
      ["100 produits au catalogue", true],
      ["100 clients & crédits suivis", true],
      ["Ventes illimitées chaque jour", true],
      ["Historique illimité", true],
      ["Gestion de stock complète", true],
      ["Rapports jour / semaine / mois", true],
      ["Export de tes données", true],
    ],
    cta: "Télécharger l'APK",
    note: "Paiement en espèces ou mobile money",
  },
  {
    name: "Lissafi Business",
    badge: "Tout inclus",
    top: true,
    amount: "75 000 F",
    period: "/an",
    sub: "= 205 F par jour",
    features: [
      ["Tout Lissafi Plus", true],
      ["Produits illimités", true],
      ["Clients & crédits illimités", true],
      ["Jusqu'à 5 utilisateurs sur le même compte", true],
      ["Sauvegarde cloud renforcée", true],
      ["Accompagnement au démarrage", true],
      ["Formation de ton équipe sur place", true],
      ["Support prioritaire", true],
      ["Accès en avant-première aux nouveautés", true],
    ],
    cta: "Télécharger l'APK",
    note: "Paiement en espèces ou mobile money",
  },
];

export default function Pricing() {
  return (
    <section className="section rail" id="tarifs">
      <div className="container">
        <Reveal className="section-head center">
          <Kicker>Les tarifs</Kicker>
          <h2>Un tarif pour chaque étape de ta boutique.</h2>
          <p>
            <b>Prix de lancement</b> : ils augmenteront après le lancement.
            Profites-en maintenant.
          </p>
        </Reveal>

        <div className="pack-launch">
          <div className="pack-left">
            <span className="pack-badge">
              <Printer size={13} /> Offre de lancement
            </span>
            <h3>Pack Lancement</h3>
            <p className="pack-desc">
              Tout pour imprimer tes reçus dès le premier jour, avec un an de
              Lissafi Plus offert.
            </p>
            <ul className="pack-features">
              <li>Imprimante thermique 58 mm (POS)</li>
              <li>2 rouleaux de papier thermique</li>
              <li>1 an Lissafi Plus offert</li>
              <li>Ventes illimitées pendant 1 an</li>
            </ul>
          </div>
          <div className="pack-right">
            <div className="pack-price">
              60 000 F<small> une fois</small>
            </div>
            <p className="pack-sub">
              Imprimante + 2 rouleaux + 1 an Lissafi Plus
            </p>
            <a className="btn btn-primary" href={downloadUrl()} download onClick={() => copyPartnerCodeForDownload()}>
              Télécharger l'APK
            </a>
            <p className="price-note">
              Paiement en espèces ou mobile money
            </p>
          </div>
        </div>

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
                href={downloadUrl()}
                download
                onClick={() => copyPartnerCodeForDownload()}
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
