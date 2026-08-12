import { Kicker, Reveal } from "./ui";

const AVANT = [
  "Calculs de tête, erreurs fréquentes",
  "Crédits notés sur un bout de papier, vite perdu",
  "Aucune idée du chiffre du mois",
  "Le stock, à l'œil et à la mémoire",
];

const APRES = [
  "Le total et la monnaie calculés automatiquement",
  "Chaque crédit client daté et retrouvable",
  "Le chiffre du jour, de la semaine, du mois — en un coup d'œil",
  "Une alerte avant la rupture de stock",
];

export default function AvantApres() {
  return (
    <section className="section" id="avant-apres">
      <div className="container">
        <Reveal>
          <Kicker>Avant / Après</Kicker>
          <h2>Le cahier vs Lissafi.</h2>
        </Reveal>

        <div className="avant-apres-grid">
          <Reveal>
            <div className="aa-col aa-avant">
              <div className="aa-label">Avec le cahier</div>
              <ul>
                {AVANT.map((t) => (
                  <li key={t}>{t}</li>
                ))}
              </ul>
            </div>
          </Reveal>
          <Reveal delay={0.1}>
            <div className="aa-col aa-apres">
              <div className="aa-label">Avec Lissafi</div>
              <ul>
                {APRES.map((t) => (
                  <li key={t}>{t}</li>
                ))}
              </ul>
              <img className="aa-shot" src="/assets/screens/rapports.png" alt="Écran de rapports de l'application Lissafi" />
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}
