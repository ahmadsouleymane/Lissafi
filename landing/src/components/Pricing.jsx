import { MessageCircle, Printer } from "lucide-react";
import { copyPartnerCodeForDownload, downloadUrl } from "../lib/tracking";
import { WHATSAPP_NUMBER } from "../config";
import { Kicker, Reveal } from "./ui";

// Le Pack Lancement est un produit physique (imprimante à livrer). La commande
// se fait sur WhatsApp — livraison au Niger uniquement pour le moment.
function packOrderUrl() {
  const text =
    "Bonjour, je veux commander le Pack Lancement Lissafi (imprimante 58 mm + 2 rouleaux + 1 an Petite boutique, 60 000 F). Je suis au Niger.";
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(text)}`;
}

const TIERS = [
  {
    name: "Petite boutique",
    badge: "Populaire",
    amount: "24 000 F",
    period: "/an",
    sub: "= 66 F par jour · ou 3 000 F/mois",
    features: [
      ["Jusqu'à 200 produits", true],
      ["Clients & crédits illimités", true],
      ["Ventes illimitées chaque jour", true],
      ["Ticket WhatsApp & impression", true],
      ["Rapports jour / semaine", true],
      ["Sauvegarde cloud", true],
    ],
    plan: "plus",
    cta: "Payer en ligne",
    note: "Carte ou mobile money · ou paie au mois sur WhatsApp",
  },
  {
    name: "Commerce / Supermarché",
    badge: "Tout inclus",
    top: true,
    amount: "50 000 F",
    period: "/an",
    sub: "ou 6 000 F/mois",
    features: [
      ["Tout Petite boutique", true],
      ["Produits illimités", true],
      ["Multi-caisses (chiffre global)", true],
      ["Plusieurs utilisateurs", true],
      ["Export CSV", true],
      ["Support prioritaire", true],
    ],
    plan: "business",
    cta: "Payer en ligne",
    note: "Carte ou mobile money · ou paie au mois sur WhatsApp",
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
            <b>14 jours d'essai gratuit</b>, toutes les fonctions débloquées.
            Tu ne paies que si Lissafi te fait gagner du temps.
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
              Petite boutique offert.
            </p>
            <ul className="pack-features">
              <li>Imprimante thermique 58 mm (POS)</li>
              <li>2 rouleaux de papier thermique</li>
              <li>1 an Petite boutique offert</li>
              <li>Ventes illimitées pendant 1 an</li>
            </ul>
          </div>
          <div className="pack-right">
            <div className="pack-price">
              60 000 F<small> une fois</small>
            </div>
            <p className="pack-sub">
              Imprimante + 2 rouleaux + 1 an Petite boutique
            </p>
            <a className="btn btn-primary" href={packOrderUrl()} target="_blank" rel="noopener noreferrer">
              <MessageCircle size={16} /> Commander le pack
            </a>
            <a className="btn btn-ghost" href={downloadUrl()} download onClick={() => copyPartnerCodeForDownload()}>
              Télécharger l'APK
            </a>
            <p className="price-note">
              🇳🇪 Livraison au Niger uniquement pour le moment · paiement à la livraison ou mobile money
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
              {t.plan ? (
                <div className="price-cta-group">
                  <a className="btn btn-primary" href={`/payer?plan=${t.plan}`}>
                    {t.cta}
                  </a>
                  <a
                    className="btn btn-ghost"
                    href={downloadUrl()}
                    download
                    onClick={() => copyPartnerCodeForDownload()}
                  >
                    Télécharger l'APK
                  </a>
                </div>
              ) : (
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
              )}
              {t.note && <p className="price-note">{t.note}</p>}
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
