import { Kicker, Reveal } from "./ui";

const PAINS = [
  {
    n: "01",
    t: "Les crédits oubliés",
    d: "Le « je paie demain » s'efface du cahier — et de ta mémoire. À la fin du mois, tu ne sais plus qui te doit quoi.",
  },
  {
    n: "02",
    t: "Les erreurs de monnaie",
    d: "Dans la foule, le calcul se fait de tête. Et la tête, parfois, se trompe. Le client repart, l'erreur reste.",
  },
  {
    n: "03",
    t: "Le stock deviné",
    d: "Tu commandes au souvenir. Trop, ou pas assez. L'argent dort en rayon pendant que la vente s'échappe.",
  },
];

export default function Problem() {
  return (
    <section className="section" id="probleme">
      <div className="container problem-grid">
        <Reveal>
          <Kicker>N° 001 — Le problème</Kicker>
          <p className="problem-lede">
            Le cahier ne coûte rien. <em>Et chaque mois, il te fait perdre de l'argent.</em>
          </p>
          <span className="problem-stat">≈ 15 000 F de crédits oubliés tous les 3 mois</span>
        </Reveal>
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {PAINS.map((p, i) => (
            <Reveal key={p.n} delay={i * 0.08}>
              <div className="problem-card">
                <span className="problem-num">{p.n}</span>
                <div>
                  <h3>{p.t}</h3>
                  <p>{p.d}</p>
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
