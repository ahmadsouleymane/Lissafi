import { WifiOff, Calculator, Users, Package, Send } from "lucide-react";
import { Kicker, Reveal } from "./ui";

const CARDS = [
  {
    size: "lg",
    icon: WifiOff,
    title: "Tout est stocké sur ton téléphone",
    desc: "Tes ventes s'enregistrent directement en local — pas besoin d'un serveur ni d'une connexion. Quand le réseau revient, tout se synchronise tout seul, en silence.",
    shot: "/assets/screens/rapports.png",
    alt: "Écran des rapports de vente dans Lissafi",
  },
  {
    size: "md",
    icon: Calculator,
    title: "La monnaie rendue",
    desc: "Le total se calcule tout seul, la monnaie à rendre s'affiche. Zéro erreur, même quand la boutique est pleine.",
    stat: "0 F",
    statLabel: "d'erreur de caisse",
  },
  {
    size: "md",
    icon: Users,
    title: "Les crédits clients",
    desc: "Le nom, le montant, la date. Plus jamais de « je paie demain » oublié.",
    shot: "/assets/screens/clients.png",
    alt: "Écran des clients et crédits dans Lissafi",
  },
  {
    size: "md",
    icon: Package,
    title: "Le stock à l'unité",
    desc: "L'alerte sonne avant la rupture. Tu commandes avec des chiffres, pas des souvenirs.",
    shot: "/assets/screens/produits.png",
    alt: "Écran des produits dans Lissafi",
  },
  {
    size: "md",
    icon: Send,
    title: "La preuve par WhatsApp",
    desc: "Le ticket part directement sur le téléphone du client. La confiance, la fin des disputes.",
    shot: "/assets/screens/recu.png",
    alt: "Reçu de vente partagé par Lissafi",
  },
];

export default function FeatureBento() {
  return (
    <section className="section rail" id="fonctionnalites">
      <div className="container">
        <Reveal className="section-head">
          <Kicker>Fonctionnalités</Kicker>
          <h2>Tout ce qu'il faut pour tenir ta boutique, sans papier.</h2>
          <p>
            Pas un outil de gestion compliqué : une caisse rapide, des crédits
            suivis et un stock à jour — pensés pour un téléphone simple et une
            boutique qui bouge.
          </p>
        </Reveal>

        <div className="bento-grid">
          {CARDS.map((c, i) => {
            const Icon = c.icon;
            return (
              <Reveal
                key={c.title}
                delay={i * 0.06}
                className={`bento-card ${c.size}`}
              >
                <div className="bento-body">
                  <span className="bento-icon">
                    <Icon size={19} strokeWidth={2} />
                  </span>
                  <h3>{c.title}</h3>
                  <p>{c.desc}</p>
                  {c.stat && (
                    <div className="bento-stat">
                      {c.stat}
                      <span>{c.statLabel}</span>
                    </div>
                  )}
                </div>
                {c.shot && (
                  <div className="bento-window">
                    <div className="window-bar" aria-hidden="true">
                      <i />
                      <i />
                      <i />
                    </div>
                    <img src={c.shot} alt={c.alt} loading="lazy" />
                  </div>
                )}
              </Reveal>
            );
          })}
        </div>
      </div>
    </section>
  );
}
